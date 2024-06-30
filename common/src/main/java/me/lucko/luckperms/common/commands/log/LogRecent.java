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

import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import java.util.List;
import java.util.UUID;

public class LogRecent extends ChildCommand<Void> {
    private static final int ENTRIES_PER_PAGE = 10;
    
    public LogRecent() {
        super(CommandSpec.LOG_RECENT, "recent", CommandPermission.LOG_RECENT, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Void ignored, ArgumentList args, String label) {
        int page = 1;
        UUID uuid = null;

        if (!args.isEmpty()) {
            int pageNo = args.getIntOrDefault(0, Integer.MIN_VALUE);
            if (pageNo != Integer.MIN_VALUE) {
                page = pageNo;
            } else {
                uuid = args.getUserTarget(0, plugin, sender);
                if (uuid == null) {
                    return;
                }

                pageNo = args.getIntOrDefault(1, Integer.MIN_VALUE);
                if (pageNo != Integer.MIN_VALUE) {
                    page = pageNo;
                }
            }
        }

        PageParameters pageParams = new PageParameters(ENTRIES_PER_PAGE, page);
        LogPage log = plugin.getStorage().getLogPage(uuid == null ? ActionFilters.all() : ActionFilters.source(uuid), pageParams).join();

        int maxPage = pageParams.getMaxPage(log.getTotalEntries());
        if (log.getTotalEntries() == 0) {
            Message.LOG_NO_ENTRIES.send(sender);
            return;
        }

        if (page < 1 || page > maxPage) {
            Message.LOG_INVALID_PAGE_RANGE.send(sender, maxPage);
            return;
        }

        List<LogPage.Entry<LoggedAction>> entries = log.getNumberedContent();
        if (uuid != null) {
            String name = entries.stream().findAny().get().value().getSource().getName();
            if (name.contains("@")) {
                name = name.split("@")[0];
            }
            Message.LOG_RECENT_BY_HEADER.send(sender, name, page, maxPage);
        } else {
            Message.LOG_RECENT_HEADER.send(sender, page, maxPage);
        }

        for (LogPage.Entry<LoggedAction> e : entries) {
            Message.LOG_ENTRY.send(sender, e.position(), e.value());
        }
    }

}
