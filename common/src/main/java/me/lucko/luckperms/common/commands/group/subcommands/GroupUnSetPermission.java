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

package me.lucko.luckperms.common.commands.group.subcommands;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

public class GroupUnSetPermission extends SubCommand<Group> {
    public GroupUnSetPermission() {
        super("unset", "Unsets a permission for the group", Permission.GROUP_UNSETPERMISSION, Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("node", true, "the permission node to unset"),
                        Arg.create("server", false, "the server to remove the permission node on"),
                        Arg.create("world", false, "the world to remove the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0).replace("{SPACE}", " ");

        if (ArgumentChecker.checkNode(node)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (node.toLowerCase().startsWith("group.")) {
            Message.GROUP_USE_UNINHERIT.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 2) {
                    group.unsetPermission(node, server);
                    Message.UNSETPERMISSION_SERVER_SUCCESS.send(sender, node, group.getDisplayName(), server);
                    LogEntry.build().actor(sender).acted(group)
                            .action("unset " + node + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(2).toLowerCase();
                    group.unsetPermission(node, server, world);
                    Message.UNSETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, group.getDisplayName(), server, world);
                    LogEntry.build().actor(sender).acted(group)
                            .action("unset " + node + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                group.unsetPermission(node);
                Message.UNSETPERMISSION_SUCCESS.send(sender, node, group.getDisplayName());
                LogEntry.build().actor(sender).acted(group)
                        .action("unset " + node)
                        .build().submit(plugin, sender);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVEPERMISSION.send(sender, group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }
    }
}
