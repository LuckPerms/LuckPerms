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

import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

public class LogGroupHistory extends SubCommand<Log> {
    private static final int ENTRIES_PER_PAGE = 10;

    public LogGroupHistory(LocaleManager locale) {
        super(CommandSpec.LOG_GROUP_HISTORY.spec(locale), "grouphistory", CommandPermission.LOG_GROUP_HISTORY, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        String group = args.get(0).toLowerCase();
        int page = Integer.MIN_VALUE;

        if (args.size() == 2) {
            try {
                page = Integer.parseInt(args.get(1));
            } catch (NumberFormatException e) {
                // invalid page
                Message.LOG_INVALID_PAGE.send(sender);
                return CommandResult.INVALID_ARGS;
            }
        }

        if (!DataConstraints.GROUP_NAME_TEST.test(group)) {
            Message.GROUP_INVALID_ENTRY.send(sender, group);
            return CommandResult.INVALID_ARGS;
        }

        int maxPage = log.getGroupHistoryMaxPages(group, ENTRIES_PER_PAGE);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page == Integer.MIN_VALUE) {
            page = maxPage;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, ExtendedLogEntry> entries = log.getGroupHistory(page, group, ENTRIES_PER_PAGE);
        String name = entries.values().stream().findAny().get().getActedName();
        Message.LOG_HISTORY_GROUP_HEADER.send(sender, name, page, maxPage);

        long now = DateUtil.unixSecondsNow();
        for (Map.Entry<Integer, ExtendedLogEntry> e : entries.entrySet()) {
            long time = e.getValue().getTimestamp();
            Message.LOG_ENTRY.send(sender,
                    e.getKey(),
                    DateUtil.formatTimeBrief(now - time),
                    e.getValue().getActorFriendlyString(),
                    Character.toString(e.getValue().getType().getCode()),
                    e.getValue().getActedFriendlyString(),
                    e.getValue().getAction()
            );
        }

        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
