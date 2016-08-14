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

package me.lucko.luckperms.api;

import java.util.SortedMap;
import java.util.SortedSet;
import java.util.UUID;

/**
 * Represents the internal LuckPerms log.
 * All content is immutable. You can add to the log using the {@link Datastore}, and then request an updated copy.
 */
@SuppressWarnings("unused")
public interface Log {

    /**
     * @return a {@link SortedSet} of all of the {@link LogEntry} objects in this {@link Log}
     */
    SortedSet<LogEntry> getContent();

    SortedSet<LogEntry> getRecent();
    SortedMap<Integer, LogEntry> getRecent(int pageNo);
    int getRecentMaxPages();

    SortedSet<LogEntry> getRecent(UUID actor);
    SortedMap<Integer, LogEntry> getRecent(int pageNo, UUID actor);
    int getRecentMaxPages(UUID actor);

    SortedSet<LogEntry> getUserHistory(UUID uuid);
    SortedMap<Integer, LogEntry> getUserHistory(int pageNo, UUID uuid);
    int getUserHistoryMaxPages(UUID uuid);

    SortedSet<LogEntry> getGroupHistory(String name);
    SortedMap<Integer, LogEntry> getGroupHistory(int pageNo, String name);
    int getGroupHistoryMaxPages(String name);

    SortedSet<LogEntry> getTrackHistory(String name);
    SortedMap<Integer, LogEntry> getTrackHistory(int pageNo, String name);
    int getTrackHistoryMaxPages(String name);

    SortedSet<LogEntry> getSearch(String query);
    SortedMap<Integer, LogEntry> getSearch(int pageNo, String query);
    int getSearchMaxPages(String query);
}
