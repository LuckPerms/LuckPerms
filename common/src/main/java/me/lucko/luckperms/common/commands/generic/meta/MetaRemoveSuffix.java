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

package me.lucko.luckperms.common.commands.generic.meta;

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Predicate;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.generic.SecondarySubCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.ArrayList;
import java.util.List;

public class MetaRemoveSuffix extends SecondarySubCommand {
    public MetaRemoveSuffix() {
        super("removesuffix", "Removes a suffix",  Permission.USER_META_REMOVESUFFIX, Permission.GROUP_META_REMOVESUFFIX, Predicate.notInRange(2, 4),
                Arg.list(
                        Arg.create("priority", true, "the priority to add the suffix at"),
                        Arg.create("suffix", true, "the suffix string"),
                        Arg.create("server", false, "the server to add the suffix on"),
                        Arg.create("world", false, "the world to add the suffix on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        final String suffix = args.get(1).replace("{SPACE}", " ");
        int priority;
        try {
            priority = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            Message.META_INVALID_PRIORITY.send(sender, args.get(0));
            return CommandResult.INVALID_ARGS;
        }

        if (suffix.equalsIgnoreCase("null")) {
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
            for (Node node : holder.getNodes()) {
                if (!node.isSuffix()) continue;
                if (node.getSuffix().getKey() != priority) continue;
                if (node.isTemporary()) continue;

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
                    holder.unsetPermission(n);
                } catch (ObjectLacksException ignored) {}
            });

            Message.BULK_CHANGE_SUCCESS.send(sender, toRemove.size());
            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } else {

            final String node = "suffix." + priority + "." + MetaUtils.escapeCharacters(suffix);

            try {
                if (args.size() >= 3) {
                    final String server = args.get(2).toLowerCase();
                    if (ArgumentChecker.checkServer(server)) {
                        Message.SERVER_INVALID_ENTRY.send(sender);
                        return CommandResult.INVALID_ARGS;
                    }

                    if (args.size() == 3) {
                        holder.unsetPermission(node, server);
                        Message.REMOVESUFFIX_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority, server);
                        LogEntry.build().actor(sender).acted(holder)
                                .action("meta removesuffix " + priority + " " + args.get(1) + " " + server)
                                .build().submit(plugin, sender);
                    } else {
                        final String world = args.get(3).toLowerCase();
                        holder.unsetPermission(node, server, world);
                        Message.REMOVESUFFIX_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority, server, world);
                        LogEntry.build().actor(sender).acted(holder)
                                .action("meta removesuffix " + priority + " " + args.get(1) + " " + server + " " + world)
                                .build().submit(plugin, sender);
                    }

                } else {
                    holder.unsetPermission(node);
                    Message.REMOVESUFFIX_SUCCESS.send(sender, holder.getFriendlyName(), suffix, priority);
                    LogEntry.build().actor(sender).acted(holder)
                            .action("meta removesuffix " + priority + " " + args.get(1))
                            .build().submit(plugin, sender);
                }

                save(holder, sender, plugin);
                return CommandResult.SUCCESS;
            } catch (ObjectLacksException e) {
                Message.DOES_NOT_HAVE_SUFFIX.send(sender, holder.getFriendlyName());
                return CommandResult.STATE_ERROR;
            }
        }
    }
}
