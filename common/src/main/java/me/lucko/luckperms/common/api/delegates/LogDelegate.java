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

package me.lucko.luckperms.common.api.delegates;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;

import static me.lucko.luckperms.common.api.ApiUtils.checkName;

/**
 * Provides a link between {@link Log} and {@link me.lucko.luckperms.common.data.Log}
 */
@AllArgsConstructor
public class LogDelegate implements Log {
    private final me.lucko.luckperms.common.data.Log handle;

    @Override
    public SortedSet<LogEntry> getContent() {
        return handle.getContent();
    }

    @Override
    public SortedSet<LogEntry> getRecent() {
        return handle.getRecent();
    }

    @Override
    public SortedMap<Integer, LogEntry> getRecent(int pageNo) {
        return handle.getRecent(pageNo);
    }

    @Override
    public int getRecentMaxPages() {
        return handle.getRecentMaxPages();
    }

    @Override
    public SortedSet<LogEntry> getRecent(@NonNull UUID actor) {
        return handle.getRecent(actor);
    }

    @Override
    public SortedMap<Integer, LogEntry> getRecent(int pageNo, @NonNull UUID actor) {
        return handle.getRecent(pageNo, actor);
    }

    @Override
    public int getRecentMaxPages(@NonNull UUID actor) {
        return handle.getRecentMaxPages(actor);
    }

    @Override
    public SortedSet<LogEntry> getUserHistory(@NonNull UUID uuid) {
        return handle.getUserHistory(uuid);
    }

    @Override
    public SortedMap<Integer, LogEntry> getUserHistory(int pageNo, @NonNull UUID uuid) {
        return handle.getUserHistory(pageNo, uuid);
    }

    @Override
    public int getUserHistoryMaxPages(@NonNull UUID uuid) {
        return handle.getUserHistoryMaxPages(uuid);
    }

    @Override
    public SortedSet<LogEntry> getGroupHistory(@NonNull String name) {
        return handle.getGroupHistory(checkName(name));
    }

    @Override
    public SortedMap<Integer, LogEntry> getGroupHistory(int pageNo, @NonNull String name) {
        return handle.getGroupHistory(pageNo, checkName(name));
    }

    @Override
    public int getGroupHistoryMaxPages(@NonNull String name) {
        return handle.getGroupHistoryMaxPages(checkName(name));
    }

    @Override
    public SortedSet<LogEntry> getTrackHistory(@NonNull String name) {
        return handle.getTrackHistory(checkName(name));
    }

    @Override
    public SortedMap<Integer, LogEntry> getTrackHistory(int pageNo, @NonNull String name) {
        return handle.getTrackHistory(pageNo, checkName(name));
    }

    @Override
    public int getTrackHistoryMaxPages(@NonNull String name) {
        return handle.getTrackHistoryMaxPages(checkName(name));
    }

    @Override
    public SortedSet<LogEntry> getSearch(@NonNull String query) {
        return handle.getSearch(query);
    }

    @Override
    public SortedMap<Integer, LogEntry> getSearch(int pageNo, @NonNull String query) {
        return handle.getSearch(pageNo, query);
    }

    @Override
    public int getSearchMaxPages(@NonNull String query) {
        return handle.getSearchMaxPages(query);
    }
}
