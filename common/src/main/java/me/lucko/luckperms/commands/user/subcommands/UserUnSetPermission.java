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
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntryBuilder;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class UserUnSetPermission extends SubCommand<User> {
    public UserUnSetPermission() {
        super("unset", "Unsets a permission for a user",
                "/%s user <user> unset <node> [server] [world]", Permission.USER_UNSETPERMISSION, Predicate.notInRange(1, 3));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String node = args.get(0);

        if (ArgumentChecker.checkNode(node)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
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
                    user.unsetPermission(node, server);
                    Message.UNSETPERMISSION_SERVER_SUCCESS.send(sender, node, user.getName(), server);
                    LogEntryBuilder.get().actor(sender).acted(user)
                            .action("unset " + node + " " + server)
                            .submit(plugin);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission(node, server, world);
                    Message.UNSETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, user.getName(), server, world);
                    LogEntryBuilder.get().actor(sender).acted(user)
                            .action("unset " + node + " " + server + " " + world)
                            .submit(plugin);
                }

            } else {
                user.unsetPermission(node);
                Message.UNSETPERMISSION_SUCCESS.send(sender, node, user.getName());
                LogEntryBuilder.get().actor(sender).acted(user)
                        .action("unset " + node)
                        .submit(plugin);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVEPERMISSION.send(sender, user.getName());
            return CommandResult.STATE_ERROR;
        }
    }
}
