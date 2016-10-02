/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.ArrayList;
import java.util.List;

public class GroupRemoveTempPrefix extends SubCommand<Group> {
    public GroupRemoveTempPrefix() {
        super("removetempprefix", "Removes a temporary prefix from the group", Permission.GROUP_REMOVE_TEMP_PREFIX,
                Predicate.notInRange(2, 4),
                Arg.list(
                        Arg.create("priority", true, "the priority to remove the prefix at"),
                        Arg.create("prefix", true, "the prefix string to remove"),
                        Arg.create("server", false, "the server to remove the prefix on"),
                        Arg.create("world", false, "the world to remove the prefix on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        final String prefix = args.get(1).replace("{SPACE}", " ");
        int priority;
        try {
            priority = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            Message.META_INVALID_PRIORITY.send(sender, args.get(0));
            return CommandResult.INVALID_ARGS;
        }

        if (prefix.equalsIgnoreCase("null")) {
            String server = null;
            String world = null;

            if (args.size() >= 3) {
                server = args.get(2).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() != 3) {
                    world = args.get(3).toLowerCase();
                }
            }

            List<Node> toRemove = new ArrayList<>();
            for (Node node : group.getNodes()) {
                if (!node.isPrefix()) continue;
                if (node.getPrefix().getKey() != priority) continue;
                if (node.isPermanent()) continue;

                if (node.getServer().isPresent()) {
                    if (server == null) continue;
                    if (!node.getServer().get().equalsIgnoreCase(server)) continue;
                } else {
                    if (server != null) continue;
                }

                if (node.getWorld().isPresent()) {
                    if (world == null) continue;
                    if (!node.getWorld().get().equalsIgnoreCase(world)) continue;
                } else {
                    if (world != null) continue;
                }

                toRemove.add(node);
            }

            toRemove.forEach(n -> {
                try {
                    group.unsetPermission(n);
                } catch (ObjectLacksException ignored) {}
            });

            Message.BULK_CHANGE_SUCCESS.send(sender, toRemove.size());
            save(group, sender, plugin);
            return CommandResult.SUCCESS;

        } else {

            final String node = "prefix." + priority + "." + ArgumentChecker.escapeCharacters(prefix);

            try {
                if (args.size() >= 3) {
                    final String server = args.get(2).toLowerCase();
                    if (ArgumentChecker.checkServer(server)) {
                        Message.SERVER_INVALID_ENTRY.send(sender);
                        return CommandResult.INVALID_ARGS;
                    }

                    if (args.size() == 3) {
                        group.unsetPermission(node, server, true);
                        Message.REMOVE_TEMP_PREFIX_SERVER_SUCCESS.send(sender, group.getDisplayName(), prefix, priority, server);
                        LogEntry.build().actor(sender).acted(group)
                                .action("removetempprefix " + priority + " " + args.get(1) + " " + server)
                                .build().submit(plugin, sender);
                    } else {
                        final String world = args.get(3).toLowerCase();
                        group.unsetPermission(node, server, world, true);
                        Message.REMOVE_TEMP_PREFIX_SERVER_WORLD_SUCCESS.send(sender, group.getDisplayName(), prefix, priority, server, world);
                        LogEntry.build().actor(sender).acted(group)
                                .action("removetempprefix " + priority + " " + args.get(1) + " " + server + " " + world)
                                .build().submit(plugin, sender);
                    }

                } else {
                    group.unsetPermission(node, true);
                    Message.REMOVE_TEMP_PREFIX_SUCCESS.send(sender, group.getDisplayName(), prefix, priority);
                    LogEntry.build().actor(sender).acted(group)
                            .action("removetempprefix " + priority + " " + args.get(1))
                            .build().submit(plugin, sender);
                }

                save(group, sender, plugin);
                return CommandResult.SUCCESS;
            } catch (ObjectLacksException e) {
                Message.DOES_NOT_HAVE_PREFIX.send(sender, group.getDisplayName());
                return CommandResult.STATE_ERROR;
            }
        }
    }
}
