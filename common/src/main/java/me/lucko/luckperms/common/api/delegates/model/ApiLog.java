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

package me.lucko.luckperms.common.api.delegates.model;


import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.Log;

import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;

import javax.annotation.Nonnull;

import static me.lucko.luckperms.common.api.ApiUtils.checkName;

@SuppressWarnings("unchecked")
public class ApiLog implements me.lucko.luckperms.api.Log {
    private static final int ENTRIES_PER_PAGE = 5;
    private final Log handle;

    public ApiLog(Log handle) {
        this.handle = handle;
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getContent() {
        return (SortedSet) this.handle.getContent();
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getRecent() {
        return (SortedSet) this.handle.getRecent();
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getRecent(int pageNo) {
        return (SortedMap) this.handle.getRecent(pageNo, ENTRIES_PER_PAGE);
    }

    @Override
    public int getRecentMaxPages() {
        return this.handle.getRecentMaxPages(ENTRIES_PER_PAGE);
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getRecent(@Nonnull UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return (SortedSet) this.handle.getRecent(actor);
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getRecent(int pageNo, @Nonnull UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return (SortedMap) this.handle.getRecent(pageNo, actor, ENTRIES_PER_PAGE);
    }

    @Override
    public int getRecentMaxPages(@Nonnull UUID actor) {
        Objects.requireNonNull(actor, "actor");
        return this.handle.getRecentMaxPages(actor, ENTRIES_PER_PAGE);
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getUserHistory(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return (SortedSet) this.handle.getUserHistory(uuid);
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getUserHistory(int pageNo, @Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return (SortedMap) this.handle.getUserHistory(pageNo, uuid, ENTRIES_PER_PAGE);
    }

    @Override
    public int getUserHistoryMaxPages(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.getUserHistoryMaxPages(uuid, ENTRIES_PER_PAGE);
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getGroupHistory(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedSet) this.handle.getGroupHistory(checkName(name));
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getGroupHistory(int pageNo, @Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedMap) this.handle.getGroupHistory(pageNo, checkName(name), ENTRIES_PER_PAGE);
    }

    @Override
    public int getGroupHistoryMaxPages(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.getGroupHistoryMaxPages(checkName(name), ENTRIES_PER_PAGE);
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getTrackHistory(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedSet) this.handle.getTrackHistory(checkName(name));
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getTrackHistory(int pageNo, @Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return (SortedMap) this.handle.getTrackHistory(pageNo, checkName(name), ENTRIES_PER_PAGE);
    }

    @Override
    public int getTrackHistoryMaxPages(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.getTrackHistoryMaxPages(checkName(name), ENTRIES_PER_PAGE);
    }

    @Nonnull
    @Override
    public SortedSet<LogEntry> getSearch(@Nonnull String query) {
        Objects.requireNonNull(query, "query");
        return (SortedSet) this.handle.getSearch(query);
    }

    @Nonnull
    @Override
    public SortedMap<Integer, LogEntry> getSearch(int pageNo, @Nonnull String query) {
        Objects.requireNonNull(query, "query");
        return (SortedMap) this.handle.getSearch(pageNo, query, ENTRIES_PER_PAGE);
    }

    @Override
    public int getSearchMaxPages(@Nonnull String query) {
        Objects.requireNonNull(query, "query");
        return this.handle.getSearchMaxPages(query, ENTRIES_PER_PAGE);
    }
}
