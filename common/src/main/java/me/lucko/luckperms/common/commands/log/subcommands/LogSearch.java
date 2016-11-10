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

package me.lucko.luckperms.common.commands.log.subcommands;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

public class LogSearch extends SubCommand<Log> {
    public LogSearch() {
        super("search", "Search the log for an entry", Permission.LOG_SEARCH, Predicates.is(0),
                Arg.list(
                        Arg.create("query", true, "the query to search by"),
                        Arg.create("page", false, "the page number to view")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Log log, List<String> args, String label) throws CommandException {
        int page = -999;
        if (args.size() > 1) {
            try {
                page = Integer.parseInt(args.get(args.size() - 1));
                args.remove(args.size() - 1);
            } catch (NumberFormatException ignored) {}
        }

        final String query = args.stream().collect(Collectors.joining(" "));

        int maxPage = log.getSearchMaxPages(query);
        if (maxPage == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (page == -999) {
            page = maxPage;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return CommandResult.INVALID_ARGS;
        }

        SortedMap<Integer, LogEntry> entries = log.getSearch(page, query);
        Message.LOG_SEARCH_HEADER.send(sender, query, page, maxPage);

        for (Map.Entry<Integer, LogEntry> e : entries.entrySet()) {
            Message.LOG_ENTRY.send(sender, e.getKey(), DateUtil.formatDateDiff(e.getValue().getTimestamp()), e.getValue().getFormatted());
        }

        return CommandResult.SUCCESS;
    }
}
