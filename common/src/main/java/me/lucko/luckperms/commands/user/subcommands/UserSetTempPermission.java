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
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserSetTempPermission extends SubCommand<User> {
    public UserSetTempPermission() {
        super("settemp", "Sets a temporary permission for a user",
                "/%s user <user> settemp <node> <true|false> <duration> [server] [world]",
                Permission.USER_SET_TEMP_PERMISSION, Predicate.notInRange(3, 5));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (node.contains("/") || node.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.USER_USE_ADDGROUP.send(sender);
            return;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender, label);
            return;
        }

        boolean b = Boolean.parseBoolean(bool);

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(2), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(2));
            return;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return;
        }

        try {
            if (args.size() >= 4) {
                final String server = args.get(3).toLowerCase();
                if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                if (args.size() == 4) {
                    user.setPermission(node, b, server, duration);
                    Message.SETPERMISSION_TEMP_SERVER_SUCCESS.send(sender, node, bool, user.getName(), server, DateUtil.formatDateDiff(duration));
                } else {
                    final String world = args.get(4).toLowerCase();
                    user.setPermission(node, b, server, world, duration);
                    Message.SETPERMISSION_TEMP_SERVER_WORLD_SUCCESS.send(sender, node, bool, user.getName(), server, world, DateUtil.formatDateDiff(duration));
                }

            } else {
                user.setPermission(node, b, duration);
                Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, bool, user.getName(), DateUtil.formatDateDiff(duration));
            }

            saveUser(user, sender, plugin);
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, user.getName());
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getBoolTabComplete(args);
    }
}
