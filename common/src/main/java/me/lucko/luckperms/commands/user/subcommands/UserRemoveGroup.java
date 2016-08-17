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
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class UserRemoveGroup extends SubCommand<User> {
    public UserRemoveGroup() {
        super("removegroup", "Removes a user from a group", "/%s user <user> removegroup <group> [server] [world]",
                Permission.USER_REMOVEGROUP, Predicate.notInRange(1, 3));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        if ((args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("global")))
                && user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
            Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
            return CommandResult.STATE_ERROR;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 2) {
                    user.unsetPermission("group." + groupName, server);
                    Message.USER_REMOVEGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
                    LogEntry.build().actor(sender).acted(user)
                            .action("removegroup " + groupName + " " + server)
                            .build().submit(plugin);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission("group." + groupName, server, world);
                    Message.USER_REMOVEGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server, world);
                    LogEntry.build().actor(sender).acted(user)
                            .action("removegroup " + groupName + " " + server + " " + world)
                            .build().submit(plugin);
                }

            } else {
                user.unsetPermission("group." + groupName);
                Message.USER_REMOVEGROUP_SUCCESS.send(sender, user.getName(), groupName);
                LogEntry.build().actor(sender).acted(user)
                        .action("removegroup " + groupName)
                        .build().submit(plugin);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.USER_NOT_MEMBER_OF.send(sender, user.getName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
