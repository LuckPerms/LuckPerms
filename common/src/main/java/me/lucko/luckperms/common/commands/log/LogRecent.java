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
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Paginated;
import me.lucko.luckperms.common.util.Predicates;

import java.util.List;
import java.util.UUID;

public class LogRecent extends ChildCommand<Log> {
    private static final int ENTRIES_PER_PAGE = 10;
    
    public LogRecent() {
        super(CommandSpec.LOG_RECENT, "recent", CommandPermission.LOG_RECENT, Predicates.notInRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, ArgumentList args, String label) {
        if (args.isEmpty()) {
            // No page or user
            Paginated<LoggedAction> content = new Paginated<>(log.getContent());
            return showLog(content.getMaxPages(ENTRIES_PER_PAGE), false, sender, content);
        }

        int page = args.getIntOrDefault(0, Integer.MIN_VALUE);
        if (page != Integer.MIN_VALUE) {
            Paginated<LoggedAction> content = new Paginated<>(log.getContent());
            return showLog(page, false, sender, content);
        }

        // User and possibly page
        UUID uuid = args.getUserTarget(0, plugin, sender);
        if (uuid == null) {
            return CommandResult.INVALID_ARGS;
        }

        Paginated<LoggedAction> content = new Paginated<>(log.getContent(uuid));
        page = args.getIntOrDefault(1, Integer.MIN_VALUE);
        if (page != Integer.MIN_VALUE) {
            return showLog(page, true, sender, content);
        } else {
            return showLog(content.getMaxPages(ENTRIES_PER_PAGE), true, sender, content);
        }
    }

    private static CommandResult showLog(int page, boolean specificUser, Sender sender, Paginated<LoggedAction> log) {
        int maxPage = log.getMaxPages(ENTRIES_PER_PAGE);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        List<Paginated.Entry<LoggedAction>> entries = log.getPage(page, ENTRIES_PER_PAGE);
        if (specificUser) {
            String name = entries.stream().findAny().get().value().getSource().getName();
            if (name.contains("@")) {
                name = name.split("@")[0];
            }
            Message.LOG_RECENT_BY_HEADER.send(sender, name, page, maxPage);
        } else {
            Message.LOG_RECENT_HEADER.send(sender, page, maxPage);
        }

        for (Paginated.Entry<LoggedAction> e : entries) {
            Message.LOG_ENTRY.send(sender, e.position(), e.value());
        }
        return CommandResult.SUCCESS;
    }
}
