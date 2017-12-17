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

package me.lucko.luckperms.common.actionlog;

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

    public static Builder builder() {
        return new Builder();
    }

    private static SortedMap<Integer, ExtendedLogEntry> getPage(Set<ExtendedLogEntry> set, int pageNo, int entries) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo cannot be less than 1: " + pageNo);
        }

        int minimumEntries = ((pageNo * entries) - entries) + 1;
        if (set.size() < minimumEntries) {
            throw new IllegalStateException("Log does not contain that many entries. " +
                    "Requested: " + minimumEntries + ", Log Count: " + set.size());
        }

        final SortedMap<Integer, ExtendedLogEntry> out = new TreeMap<>();

        final int max = minimumEntries + entries - 1;
        int index = 0;
        for (ExtendedLogEntry e : set) {
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
        return (int) Math.ceil((double) size / (double) entries);
    }

    @Getter
    private final SortedSet<ExtendedLogEntry> content;

    public Log(SortedSet<ExtendedLogEntry> content) {
        this.content = ImmutableSortedSet.copyOfSorted(content);
    }

    public SortedSet<ExtendedLogEntry> getRecent() {
        return content;
    }

    public SortedMap<Integer, ExtendedLogEntry> getRecent(int pageNo, int entriesPerPage) {
        return getPage(content, pageNo, entriesPerPage);
    }

    public int getRecentMaxPages(int entriesPerPage) {
        return getMaxPages(content.size(), entriesPerPage);
    }

    public SortedSet<ExtendedLogEntry> getRecent(UUID actor) {
        return content.stream()
                .filter(e -> e.getActor().equals(actor))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, ExtendedLogEntry> getRecent(int pageNo, UUID actor, int entriesPerPage) {
        return getPage(getRecent(actor), pageNo, entriesPerPage);
    }

    public int getRecentMaxPages(UUID actor, int entriesPerPage) {
        return getMaxPages(content.stream()
                .filter(e -> e.getActor().equals(actor))
                .mapToInt(x -> 1).sum(), entriesPerPage);
    }

    public SortedSet<ExtendedLogEntry> getUserHistory(UUID uuid) {
        return content.stream()
                .filter(e -> e.getType() == LogEntry.Type.USER)
                .filter(e -> e.getActed().isPresent())
                .filter(e -> e.getActed().get().equals(uuid))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, ExtendedLogEntry> getUserHistory(int pageNo, UUID uuid, int entriesPerPage) {
        return getPage(getUserHistory(uuid), pageNo, entriesPerPage);
    }

    public int getUserHistoryMaxPages(UUID uuid, int entriesPerPage) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == LogEntry.Type.USER)
                .filter(e -> e.getActed().isPresent())
                .filter(e -> e.getActed().get().equals(uuid))
                .mapToInt(x -> 1).sum(), entriesPerPage);
    }

    public SortedSet<ExtendedLogEntry> getGroupHistory(String name) {
        return content.stream()
                .filter(e -> e.getType() == LogEntry.Type.GROUP)
                .filter(e -> e.getActedName().equals(name))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, ExtendedLogEntry> getGroupHistory(int pageNo, String name, int entriesPerPage) {
        return getPage(getGroupHistory(name), pageNo, entriesPerPage);
    }

    public int getGroupHistoryMaxPages(String name, int entriesPerPage) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == LogEntry.Type.GROUP)
                .filter(e -> e.getActedName().equals(name))
                .mapToInt(x -> 1).sum(), entriesPerPage);
    }

    public SortedSet<ExtendedLogEntry> getTrackHistory(String name) {
        return content.stream()
                .filter(e -> e.getType() == LogEntry.Type.TRACK)
                .filter(e -> e.getActedName().equals(name))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, ExtendedLogEntry> getTrackHistory(int pageNo, String name, int entriesPerPage) {
        return getPage(getTrackHistory(name), pageNo, entriesPerPage);
    }

    public int getTrackHistoryMaxPages(String name, int entriesPerPage) {
        return getMaxPages(content.stream()
                .filter(e -> e.getType() == LogEntry.Type.TRACK)
                .filter(e -> e.getActedName().equals(name))
                .mapToInt(x -> 1).sum(), entriesPerPage);
    }

    public SortedSet<ExtendedLogEntry> getSearch(String query) {
        return content.stream()
                .filter(e -> e.matchesSearch(query))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedMap<Integer, ExtendedLogEntry> getSearch(int pageNo, String query, int entriesPerPage) {
        return getPage(getSearch(query), pageNo, entriesPerPage);
    }

    public int getSearchMaxPages(String query, int entriesPerPage) {
        return getMaxPages(content.stream()
                .filter(e -> e.matchesSearch(query))
                .mapToInt(x -> 1).sum(), entriesPerPage);
    }

    @SuppressWarnings("WeakerAccess")
    public static class Builder {
        private final SortedSet<ExtendedLogEntry> content = new TreeSet<>();

        public Builder add(ExtendedLogEntry e) {
            content.add(e);
            return this;
        }

        public Log build() {
            return new Log(content);
        }
    }

}
