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
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class UserSetPermission extends SubCommand<User> {
    public UserSetPermission() {
        super("set", "Sets a permission for a user",
                "/%s user <user> set <node> <true|false> [server] [world]", Permission.USER_SETPERMISSION,
                Predicate.notInRange(2, 4));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (ArgumentChecker.checkNode(node)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.USER_USE_ADDGROUP.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        boolean b = Boolean.parseBoolean(bool);

        try {
            if (args.size() >= 3) {
                final String server = args.get(2).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 3) {
                    user.setPermission(node, b, server);
                    Message.SETPERMISSION_SERVER_SUCCESS.send(sender, node, bool, user.getName(), server);
                    LogEntryBuilder.get().actor(sender).acted(user)
                            .action("set " + node + " " + b + " " + server)
                            .submit(plugin);
                } else {
                    final String world = args.get(3).toLowerCase();
                    user.setPermission(node, b, server, world);
                    Message.SETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, bool, user.getName(), server, world);
                    LogEntryBuilder.get().actor(sender).acted(user)
                            .action("set " + node + " " + b + " " + server + " " + world)
                            .submit(plugin);
                }

            } else {
                user.setPermission(node, b);
                Message.SETPERMISSION_SUCCESS.send(sender, node, bool, user.getName());
                LogEntryBuilder.get().actor(sender).acted(user)
                        .action("set " + node + " " + b)
                        .submit(plugin);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HASPERMISSION.send(sender, user.getName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getBoolTabComplete(args);
    }
}
