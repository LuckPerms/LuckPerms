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

public class GroupAddTempSuffix extends SubCommand<Group> {
    public GroupAddTempSuffix() {
        super("addtempsuffix", "Adds a suffix to the group temporarily", "/%s group <group> addtempsuffix <priority> <suffix> <duration> [server] [world]",
                Permission.GROUP_ADD_TEMP_SUFFIX, Predicate.notInRange(3, 5));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        final String suffix = args.get(1).replace("{SPACE}", " ");
        int priority;
        try {
            priority = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            Message.META_INVALID_PRIORITY.send(sender, args.get(0));
            return CommandResult.INVALID_ARGS;
        }

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

        final String node = "suffix." + priority + "." + ArgumentChecker.escapeCharacters(suffix);

        try {
            if (args.size() >= 4) {
                final String server = args.get(3).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 4) {
                    group.setPermission(node, true, server, duration);
                    Message.ADD_TEMP_SUFFIX_SERVER_SUCCESS.send(sender, group.getName(), suffix, priority, server, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("addtempsuffix " + priority + " " + args.get(1) + " " + duration + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(4).toLowerCase();
                    group.setPermission(node, true, server, world, duration);
                    Message.ADD_TEMP_SUFFIX_SERVER_WORLD_SUCCESS.send(sender, group.getName(), suffix, priority, server, world, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(group)
                            .action("addtempsuffix " + priority + " " + args.get(1) + " " + duration + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                group.setPermission(node, true, duration);
                Message.ADD_TEMP_SUFFIX_SUCCESS.send(sender, group.getName(), suffix, priority, DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(group)
                        .action("addtempsuffix " + priority + " " + args.get(1) + " " + duration)
                        .build().submit(plugin, sender);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_SUFFIX.send(sender, group.getName());
            return CommandResult.STATE_ERROR;
        }
    }
}
