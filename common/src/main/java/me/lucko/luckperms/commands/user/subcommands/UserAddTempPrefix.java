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
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;
import me.lucko.luckperms.utils.DateUtil;

import java.util.List;

public class UserAddTempPrefix extends SubCommand<User> {
    public UserAddTempPrefix() {
        super("addtempprefix", "Adds a prefix to the user temporarily", "<priority> <prefix> <duration> [server] [world]",
                Permission.USER_ADD_TEMP_PREFIX, Predicate.notInRange(3, 5));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        final String prefix = args.get(1).replace("{SPACE}", " ");
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

        final String node = "prefix." + priority + "." + ArgumentChecker.escapeCharacters(prefix);

        try {
            if (args.size() >= 4) {
                final String server = args.get(3).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 4) {
                    user.setPermission(node, true, server, duration);
                    Message.ADD_TEMP_PREFIX_SERVER_SUCCESS.send(sender, user.getName(), prefix, priority, server, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(user)
                            .action("addtempprefix " + priority + " " + args.get(1) + " " + duration + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(4).toLowerCase();
                    user.setPermission(node, true, server, world, duration);
                    Message.ADD_TEMP_PREFIX_SERVER_WORLD_SUCCESS.send(sender, user.getName(), prefix, priority, server, world, DateUtil.formatDateDiff(duration));
                    LogEntry.build().actor(sender).acted(user)
                            .action("addtempprefix " + priority + " " + args.get(1) + " " + duration + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                user.setPermission(node, true, duration);
                Message.ADD_TEMP_PREFIX_SUCCESS.send(sender, user.getName(), prefix, priority, DateUtil.formatDateDiff(duration));
                LogEntry.build().actor(sender).acted(user)
                        .action("addtempprefix " + priority + " " + args.get(1) + " " + duration)
                        .build().submit(plugin, sender);
            }

            save(user, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_PREFIX.send(sender, user.getName());
            return CommandResult.STATE_ERROR;
        }
    }
}
