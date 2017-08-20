/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.commands.impl.log;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.constants.DataConstraints;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

public class LogRecent extends SubCommand<Log> {
    public LogRecent(LocaleManager locale) {
        super(CommandSpec.LOG_RECENT.spec(locale), "recent", CommandPermission.LOG_RECENT, Predicates.notInRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        if (args.size() == 0) {
            // No page or user
            return showLog(log.getRecentMaxPages(), null, sender, log);
        }

        if (args.size() == 1) {
            // Page or user
            try {
                int p = Integer.parseInt(args.get(0));
                // page
                return showLog(p, null, sender, log);
            } catch (NumberFormatException ignored) {
            }
        }

        // User and possibly page
        final String s = args.get(0);
        UUID u;

        u = Util.parseUuid(s);
        if (u == null) {
            if (s.length() <= 16) {
                if (!DataConstraints.PLAYER_USERNAME_TEST.test(s)) {
                    Message.USER_INVALID_ENTRY.send(sender, s);
                    return CommandResult.INVALID_ARGS;
                }

                UUID uuid = plugin.getStorage().getUUID(s).join();

                if (uuid == null) {
                    Message.USER_NOT_FOUND.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() != 2) {
                    // Just user
                    return showLog(log.getRecentMaxPages(uuid), uuid, sender, log);
                }

                try {
                    int p = Integer.parseInt(args.get(1));
                    // User and page
                    return showLog(p, uuid, sender, log);
                } catch (NumberFormatException e) {
                    // Invalid page
                    return showLog(-1, null, sender, log);
                }
            }

            Message.USER_INVALID_ENTRY.send(sender, s);
            return CommandResult.INVALID_ARGS;
        }

        if (args.size() != 2) {
            // Just user
            return showLog(log.getRecentMaxPages(u), u, sender, log);
        } else {
            try {
                int p = Integer.parseInt(args.get(1));
                // User and page
                return showLog(p, u, sender, log);
            } catch (NumberFormatException e) {
                // Invalid page
                return showLog(-1, null, sender, log);
            }
        }
    }

    private static CommandResult showLog(int page, UUID filter, Sender sender, Log log) {
        int maxPage = (filter != null) ? log.getRecentMaxPages(filter) : log.getRecentMaxPages();
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, LogEntry> entries = (filter != null) ? log.getRecent(page, filter) : log.getRecent(page);
        if (filter != null) {
            String name = entries.values().stream().findAny().get().getActorName();
            Message.LOG_RECENT_BY_HEADER.send(sender, name, page, maxPage);
        } else {
            Message.LOG_RECENT_HEADER.send(sender, page, maxPage);
        }

        for (Map.Entry<Integer, LogEntry> e : entries.entrySet()) {
            long time = e.getValue().getTimestamp();
            long now = DateUtil.unixSecondsNow();
            Message.LOG_ENTRY.send(sender, e.getKey(), DateUtil.formatTimeShort(now - time), e.getValue().getFormatted());
        }
        return CommandResult.SUCCESS;
    }
}
