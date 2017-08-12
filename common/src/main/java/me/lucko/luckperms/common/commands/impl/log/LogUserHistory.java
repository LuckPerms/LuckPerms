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
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.constants.DataConstraints;
import me.lucko.luckperms.common.data.Log;
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

public class LogUserHistory extends SubCommand<Log> {
    public LogUserHistory(LocaleManager locale) {
        super(CommandSpec.LOG_USER_HISTORY.spec(locale), "userhistory", CommandPermission.LOG_USER_HISTORY, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        String user = args.get(0);
        int page = -999;

        if (args.size() == 2) {
            try {
                page = Integer.parseInt(args.get(1));
            } catch (NumberFormatException e) {
                // invalid page
                return showLog(-1, null, sender, log);
            }
        }

        UUID uuid = Util.parseUuid(user);
        if (uuid != null) {
            if (page == -999) {
                page = log.getUserHistoryMaxPages(uuid);
            }

            return showLog(page, uuid, sender, log);
        }

        if (user.length() <= 16) {
            if (!DataConstraints.PLAYER_USERNAME_TEST.test(user)) {
                Message.USER_INVALID_ENTRY.send(sender, user);
                return CommandResult.INVALID_ARGS;
            }

            UUID uuid1 = plugin.getStorage().getUUID(user).join();

            if (uuid1 == null) {
                Message.USER_NOT_FOUND.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (page == -999) {
                page = log.getUserHistoryMaxPages(uuid1);
            }

            return showLog(page, uuid1, sender, log);
        }

        Message.USER_INVALID_ENTRY.send(sender, user);
        return CommandResult.INVALID_ARGS;
    }

    private static CommandResult showLog(int page, UUID user, Sender sender, Log log) {
        int maxPage = log.getUserHistoryMaxPages(user);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, LogEntry> entries = log.getUserHistory(page, user);
        String name = entries.values().stream().findAny().get().getActedName();
        Message.LOG_HISTORY_USER_HEADER.send(sender, name, page, maxPage);

        for (Map.Entry<Integer, LogEntry> e : entries.entrySet()) {
            Message.LOG_ENTRY.send(sender, e.getKey(), DateUtil.formatDateDiff(e.getValue().getTimestamp()), e.getValue().getFormatted());
        }

        return CommandResult.SUCCESS;
    }
}
