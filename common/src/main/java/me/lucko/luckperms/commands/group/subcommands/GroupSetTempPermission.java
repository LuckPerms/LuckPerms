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
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;
import me.lucko.luckperms.utils.DateUtil;

import java.util.List;

public class GroupSetTempPermission extends SubCommand<Group> {
    public GroupSetTempPermission() {
        super("settemp", "Sets a permission for the group temporarily", Permission.GROUP_SET_TEMP_PERMISSION,
                Predicate.notInRange(3, 5),
                Arg.list(
                        Arg.create("node", true, "the permission node to set"),
                        Arg.create("true|false", true, "the value of the node"),
                        Arg.create("duration", true, "the duration until the permission node expires"),
                        Arg.create("server", false, "the server to add the permission node on"),
                        Arg.create("world", false, "the world to add the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0).replace("{SPACE}", " ");
        String bool = args.get(1).toLowerCase();

        if (ArgumentChecker.checkNode(node)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (node.toLowerCase().startsWith("group.")) {
            Message.GROUP_USE_INHERIT.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        boolean b = Boolean.parseBoolean(bool);

        long duration;
        try {
            duration = Long.parseLong(args.get(2));
        } catch (NumberFormatException e) {
            try {
                duration = DateUtil.parseDateDiff(args.get(2), true);
            } catch (DateUtil.IllegalDateException e1) {
                Message.ILLEGAL_DATE_ERROR.send(sender, args.get(2));
                return CommandResult.INVALID_ARGS;
            }
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
                    Message.SETPERMISSION_TEMP_SERVER_SUCCESS.send(sender, node, bool, group.getDisplayName(), server,
                            DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("settemp " + node + " " + b + " " + duration + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(4).toLowerCase();
                    group.setPermission(node, b, server, world, duration);
                    Message.SETPERMISSION_TEMP_SERVER_WORLD_SUCCESS.send(sender, node, bool, group.getDisplayName(), server,
                            world, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("settemp " + node + " " + b + " " + duration + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                group.setPermission(node, b, duration);
                Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, bool, group.getDisplayName(), DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(group)
                        .action("settemp " + node + " " + b + " " + duration)
                        .build().submit(plugin, sender);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getBoolTabComplete(args);
    }
}
