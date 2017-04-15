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

package me.lucko.luckperms.common.data;

import lombok.Getter;

import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.LogEntry;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class Log {
    private static final int PAGE_ENTRIES = 5;

    public static Builder builder() {
        return new Builder();
    }

    private static SortedMap<Integer, LogEntry> getPage(Set<LogEntry> set, int pageNo, int entries) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo cannot be less than 1: " + pageNo);
        }

        int minimumEntries = ((pageNo * 5) - entries) + 1;
        if (set.size() < minimumEntries) {
            throw new IllegalStateException("Log does not contain that many entries. " +
                    "Requested: " + minimumEntries + ", Log Count: " + set.size());
        }

        final SortedMap<Integer, LogEntry> out = new TreeMap<>();

        final int max = minimumEntries + entries - 1;
        int index = 0;
        for (LogEntry e : set) {
            index++;
            if (index >= minimumEntries) {
                out.put(index, e);
            }
            if (index == max) {
                break;
            }
        }

        return out;
    }

    private static int getMaxPages(int size, int entries) {
        return (int) Math.ceil((double) size / entries);
    }

    private static int getMaxPages(long size, int entries) {
        return (int) Math.ceil((double) size / entries);
    }

    @Getter
    private final SortedSet<LogEntry> content;

    public Log(SortedSet<LogEntry> content) {
        this.content = ImmutableSortedSet.copyOfSorted(content);
    }

    public SortedSet<LogEntry> getRecent() {
        return content;
    }

    public SortedMap<Integer, LogEntry> getRecent(int pageNo) {
        return getPage(content, pageNo, PAGE_ENTRIES);
    }

    public int getRecentMaxPages() {
        return getMaxPages(content.size(), PAGE_ENTRIES);
    }

    public SortedSet<LogEntry> getRecent(UUID actor) {
        return content.stream()
                .filter(e -> e.getActor().equals(actor))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, LogEntry> getRecent(int pageNo, UUID actor) {
        return getPage(getRecent(actor), pageNo, PAGE_ENTRIES);
    }

    public int getRecentMaxPages(UUID actor) {
        return getMaxPages(content.stream()
                .filter(e -> e.getActor().equals(actor))
                .count(), PAGE_ENTRIES);
    }

    public SortedSet<LogEntry> getUserHistory(UUID uuid) {
        return content.stream()
                .filter(e -> e.getType() == 'U')
                .filter(e -> e.getActed().equals(uuid))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, LogEntry> getUserHistory(int pageNo, UUID uuid) {
        return getPage(getUserHistory(uuid), pageNo, PAGE_ENTRIES);
    }

    public int getUserHistoryMaxPages(UUID uuid) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == 'U')
                .filter(e -> e.getActed().equals(uuid))
                .count(), PAGE_ENTRIES);
    }

    public SortedSet<LogEntry> getGroupHistory(String name) {
        return content.stream()
                .filter(e -> e.getType() == 'G')
                .filter(e -> e.getActedName().equals(name))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, LogEntry> getGroupHistory(int pageNo, String name) {
        return getPage(getGroupHistory(name), pageNo, PAGE_ENTRIES);
    }

    public int getGroupHistoryMaxPages(String name) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == 'G')
                .filter(e -> e.getActedName().equals(name))
                .count(), PAGE_ENTRIES);
    }

    public SortedSet<LogEntry> getTrackHistory(String name) {
        return content.stream()
                .filter(e -> e.getType() == 'T')
                .filter(e -> e.getActedName().equals(name))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, LogEntry> getTrackHistory(int pageNo, String name) {
        return getPage(getTrackHistory(name), pageNo, PAGE_ENTRIES);
    }

    public int getTrackHistoryMaxPages(String name) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == 'T')
                .filter(e -> e.getActedName().equals(name))
                .count(), PAGE_ENTRIES);
    }

    public SortedSet<LogEntry> getSearch(String query) {
        return content.stream()
                .filter(e -> e.matchesSearch(query))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, LogEntry> getSearch(int pageNo, String query) {
        return getPage(getSearch(query), pageNo, PAGE_ENTRIES);
    }

    public int getSearchMaxPages(String query) {
        return getMaxPages(content.stream()
                .filter(e -> e.matchesSearch(query))
                .count(), PAGE_ENTRIES);
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private final SortedSet<LogEntry> content = new TreeSet<>();

        public Builder add(LogEntry e) {
            content.add(e);
            return this;
        }

        public Log build() {
            return new Log(content);
        }
    }

}
