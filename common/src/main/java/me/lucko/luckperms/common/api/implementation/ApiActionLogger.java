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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.actionlog.LogDispatcher;
import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.filter.FilterList;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.actionlog.ActionLog;
import net.luckperms.api.actionlog.ActionLogger;
import net.luckperms.api.actionlog.filter.ActionFilter;
import net.luckperms.api.util.Page;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ApiActionLogger implements ActionLogger {
    private final LuckPermsPlugin plugin;

    public ApiActionLogger(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Action.@NonNull Builder actionBuilder() {
        return LoggedAction.build();
    }

    @Override
    @Deprecated
    public @NonNull CompletableFuture<ActionLog> getLog() {
        return this.plugin.getStorage().getLogPage(ActionFilters.all(), null)
                .thenApply(result -> new ApiActionLog(result.getContent()));
    }

    @Override
    public @NonNull CompletableFuture<List<Action>> queryActions(@NonNull ActionFilter filter) {
        return this.plugin.getStorage().getLogPage(getFilterList(filter), null).thenApply(ActionPage::new).thenApply(Page::entries);
    }

    @Override
    public @NonNull CompletableFuture<Page<Action>> queryActions(@NonNull ActionFilter filter, int pageSize, int pageNumber) {
        return this.plugin.getStorage().getLogPage(getFilterList(filter), new PageParameters(pageSize, pageNumber)).thenApply(ActionPage::new);
    }

    @Override
    public @NonNull CompletableFuture<Void> submit(@NonNull Action entry) {
        return this.plugin.getLogDispatcher().dispatchFromApi((LoggedAction) entry);
    }

    @Override
    public @NonNull CompletableFuture<Void> submitToStorage(@NonNull Action entry) {
        return this.plugin.getLogDispatcher().logToStorage((LoggedAction) entry);
    }

    @Override
    public @NonNull CompletableFuture<Void> broadcastAction(@NonNull Action entry) {
        LogDispatcher dispatcher = this.plugin.getLogDispatcher();

        CompletableFuture<Void> messagingFuture = dispatcher.logToStorage(((LoggedAction) entry));
        dispatcher.broadcastFromApi(((LoggedAction) entry));
        return messagingFuture;
    }

    private static FilterList<Action> getFilterList(ActionFilter filter) {
        Objects.requireNonNull(filter, "filter");
        if (filter instanceof ApiActionFilter) {
            return ((ApiActionFilter) filter).getFilter();
        } else {
            throw new IllegalArgumentException("Unknown filter type: " + filter.getClass());
        }
    }

    private static final class ActionPage implements Page<Action> {
        private final LogPage page;

        private ActionPage(LogPage page) {
            this.page = page;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public @NonNull List<Action> entries() {
            return (List) this.page.getContent();
        }

        @Override
        public int overallSize() {
            return this.page.getTotalEntries();
        }
    }
}
