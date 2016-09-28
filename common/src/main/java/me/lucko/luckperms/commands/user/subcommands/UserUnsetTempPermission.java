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

package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class UserUnsetTempPermission extends SubCommand<User> {
    public UserUnsetTempPermission() {
        super("unsettemp", "Unsets a temporary permission for the user", Permission.USER_UNSET_TEMP_PERMISSION,
                Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("node", true, "the permission node to unset"),
                        Arg.create("server", false, "the server to remove the permission node on"),
                        Arg.create("world", false, "the world to remove the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String node = args.get(0).replace("{SPACE}", " ");

        if (ArgumentChecker.checkNode(node)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (node.toLowerCase().startsWith("group.")) {
            Message.USER_USE_REMOVEGROUP.send(sender);
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
                    user.unsetPermission(node, server, true);
                    Message.UNSET_TEMP_PERMISSION_SERVER_SUCCESS.send(sender, node, user.getName(), server);
                    LogEntry.build().actor(sender).acted(user)
                            .action("unsettemp " + node + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission(node, server, world, true);
                    Message.UNSET_TEMP_PERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, user.getName(), server, world);
                    LogEntry.build().actor(sender).acted(user)
                            .action("unsettemp " + node + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                user.unsetPermission(node, true);
                Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, user.getName());
                LogEntry.build().actor(sender).acted(user)
                        .action("unsettemp " + node)
                        .build().submit(plugin, sender);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, user.getName());
            return CommandResult.STATE_ERROR;
        }
    }
}
