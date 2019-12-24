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

package me.lucko.luckperms.common.commands.log;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Paginated;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.actionlog.Action;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.UUID;

public class LogUserHistory extends SubCommand<Log> {
    private static final int ENTRIES_PER_PAGE = 10;

    public LogUserHistory(LocaleManager locale) {
        super(CommandSpec.LOG_USER_HISTORY.localize(locale), "userhistory", CommandPermission.LOG_USER_HISTORY, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) {
        UUID uuid = ArgumentParser.parseUserTarget(0, args, plugin, sender);
        if (uuid == null) {
            return CommandResult.INVALID_ARGS;
        }

        Paginated<LoggedAction> content = new Paginated<>(log.getUserHistory(uuid));

        int page = ArgumentParser.parseIntOrElse(1, args, Integer.MIN_VALUE);
        if (page != Integer.MIN_VALUE) {
            return showLog(page, sender, content);
        } else {
            return showLog(content.getMaxPages(ENTRIES_PER_PAGE), sender, content);
        }
    }

    private static CommandResult showLog(int page, Sender sender, Paginated<LoggedAction> log) {
        int maxPage = log.getMaxPages(ENTRIES_PER_PAGE);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, LoggedAction> entries = log.getPage(page, ENTRIES_PER_PAGE);
        String name = ((Action) entries.values().stream().findAny().get()).getTarget().getName();
        Message.LOG_HISTORY_USER_HEADER.send(sender, name, page, maxPage);

        for (Map.Entry<Integer, LoggedAction> e : entries.entrySet()) {
            Message.LOG_ENTRY.send(sender,
                    e.getKey(),
                    DurationFormatter.CONCISE_LOW_ACCURACY.format(e.getValue().getDurationSince()),
                    e.getValue().getSourceFriendlyString(),
                    Character.toString(LoggedAction.getTypeCharacter(((Action) e.getValue()).getTarget().getType())),
                    e.getValue().getTargetFriendlyString(),
                    e.getValue().getDescription()
            );
        }

        return CommandResult.SUCCESS;
    }
}
