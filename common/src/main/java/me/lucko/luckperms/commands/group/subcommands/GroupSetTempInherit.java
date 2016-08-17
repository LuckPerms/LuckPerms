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
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;
import me.lucko.luckperms.utils.DateUtil;

import java.util.List;

public class GroupSetTempInherit extends SubCommand<Group> {
    public GroupSetTempInherit() {
        super("settempinherit", "Sets another group for this group to inherit permissions from temporarily",
                "/%s group <group> settempinherit <group> <duration> [server] [world]",
                Permission.GROUP_SET_TEMP_INHERIT, Predicate.notInRange(2, 4));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(1), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(1));
            return CommandResult.INVALID_ARGS;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().loadGroup(groupName)) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group1 = plugin.getGroupManager().get(groupName);
        if (group1 == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        try {
            if (args.size() >= 3) {
                final String server = args.get(2).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 3) {
                    group.setInheritGroup(group1, server, duration);
                    Message.GROUP_SET_TEMP_INHERIT_SERVER_SUCCESS.send(sender, group.getName(), group1.getName(), server,
                            DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("settempinherit " + group1.getName() + " " + duration + " " + server)
                            .build().submit(plugin);
                } else {
                    final String world = args.get(3).toLowerCase();
                    group.setInheritGroup(group1, server, world, duration);
                    Message.GROUP_SET_TEMP_INHERIT_SERVER_WORLD_SUCCESS.send(sender, group.getName(), group1.getName(), server,
                            world, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("settempinherit " + group1.getName() + " " + duration + " " + server + " " + world)
                            .build().submit(plugin);
                }

            } else {
                group.setInheritGroup(group1, duration);
                Message.GROUP_SET_TEMP_INHERIT_SUCCESS.send(sender, group.getName(), group1.getName(), DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(group)
                        .action("settempinherit " + group1.getName() + " " + duration)
                        .build().submit(plugin);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.USER_ALREADY_TEMP_MEMBER_OF.send(sender, group.getName(), group1.getName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
