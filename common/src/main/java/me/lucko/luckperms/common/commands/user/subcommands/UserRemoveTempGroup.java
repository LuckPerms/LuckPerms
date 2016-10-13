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

package me.lucko.luckperms.common.commands.user.subcommands;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

public class UserRemoveTempGroup extends SubCommand<User> {
    public UserRemoveTempGroup() {
        super("removetempgroup", "Removes the user from a temporary group", Permission.USER_REMOVETEMPGROUP, Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("group", true, "the group to remove the user from"),
                        Arg.create("server", false, "the server to remove the group on"),
                        Arg.create("world", false, "the world to remove the group on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendDetailedUsage(sender);
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
                    user.unsetPermission("group." + groupName, server, true);
                    Message.USER_REMOVETEMPGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
                    LogEntry.build().actor(sender).acted(user)
                            .action("removetempgroup " + groupName + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission("group." + groupName, server, world, true);
                    Message.USER_REMOVETEMPGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server, world);
                    LogEntry.build().actor(sender).acted(user)
                            .action("removetempgroup " + groupName + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                user.unsetPermission("group." + groupName, true);
                Message.USER_REMOVETEMPGROUP_SUCCESS.send(sender, user.getName(), groupName);
                LogEntry.build().actor(sender).acted(user)
                        .action("removetempgroup " + groupName)
                        .build().submit(plugin, sender);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.USER_NOT_TEMP_MEMBER_OF.send(sender, user.getName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
