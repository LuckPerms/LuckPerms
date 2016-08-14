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
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntryBuilder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;
import me.lucko.luckperms.utils.DateUtil;

import java.util.List;

public class GroupSetTempPermission extends SubCommand<Group> {
    public GroupSetTempPermission() {
        super("settemp", "Sets a temporary permission for a group",
                "/%s group <group> settemp <node> <true|false> <duration> [server] [world]",
                Permission.GROUP_SET_TEMP_PERMISSION, Predicate.notInRange(3, 5));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (ArgumentChecker.checkNode(node)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.GROUP_USE_INHERIT.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        boolean b = Boolean.parseBoolean(bool);

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(2), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(2));
            return CommandResult.INVALID_ARGS;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        try {
            if (args.size() >= 4) {
                final String server = args.get(3).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 4) {
                    group.setPermission(node, b, server, duration);
                    Message.SETPERMISSION_TEMP_SERVER_SUCCESS.send(sender, node, bool, group.getName(), server,
                            DateUtil.formatDateDiff(duration));
                    LogEntryBuilder.get().actor(sender).acted(group)
                            .action("settemp " + node + " " + b + " " + duration + " " + server)
                            .submit(plugin);
                } else {
                    final String world = args.get(4).toLowerCase();
                    group.setPermission(node, b, server, world, duration);
                    Message.SETPERMISSION_TEMP_SERVER_WORLD_SUCCESS.send(sender, node, bool, group.getName(), server,
                            world, DateUtil.formatDateDiff(duration));
                    LogEntryBuilder.get().actor(sender).acted(group)
                            .action("settemp " + node + " " + b + " " + duration + " " + server + " " + world)
                            .submit(plugin);
                }

            } else {
                group.setPermission(node, b, duration);
                Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, bool, group.getName(), DateUtil.formatDateDiff(duration));
                LogEntryBuilder.get().actor(sender).acted(group)
                        .action("settemp " + node + " " + b + " " + duration)
                        .submit(plugin);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, group.getName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getBoolTabComplete(args);
    }
}
