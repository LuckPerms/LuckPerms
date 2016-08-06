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
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserAddTempGroup extends SubCommand<User> {
    public UserAddTempGroup() {
        super("addtempgroup", "Adds the user to a group temporarily",
                "/%s user <user> addtempgroup <group> <duration> [server] [world]", Permission.USER_ADDTEMPGROUP,
                Predicate.notInRange(2, 4));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(1), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(1));
            return;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return;
        }

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Message.GROUP_DOES_NOT_EXIST.send(sender);
            } else {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Message.GROUP_DOES_NOT_EXIST.send(sender);
                    return;
                }

                try {
                    if (args.size() >= 3) {
                        final String server = args.get(2).toLowerCase();
                        if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                            Message.SERVER_INVALID_ENTRY.send(sender);
                            return;
                        }

                        if (args.size() == 3) {
                            user.addGroup(group, server, duration);
                            Message.USER_ADDTEMPGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server,
                                    DateUtil.formatDateDiff(duration));
                        } else {
                            final String world = args.get(3).toLowerCase();
                            user.addGroup(group, server, world, duration);
                            Message.USER_ADDTEMPGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server,
                                    world, DateUtil.formatDateDiff(duration));
                        }

                    } else {
                        user.addGroup(group, duration);
                        Message.USER_ADDTEMPGROUP_SUCCESS.send(sender, user.getName(), groupName, DateUtil.formatDateDiff(duration));
                    }

                    saveUser(user, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.USER_ALREADY_TEMP_MEMBER_OF.send(sender, user.getName(), group.getName());
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }
}
