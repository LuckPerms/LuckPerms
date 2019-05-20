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

import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public class Log {
    private static Log empty = null;

    public static Builder builder() {
        return new Builder();
    }

    public static synchronized Log empty() {
        if (empty == null) {
            empty = builder().build();
        }
        return empty;
    }

    private final SortedSet<ExtendedLogEntry> content;

    public Log(SortedSet<ExtendedLogEntry> content) {
        this.content = ImmutableSortedSet.copyOfSorted(content);
    }

    public SortedSet<ExtendedLogEntry> getContent() {
        return this.content;
    }

    public SortedSet<ExtendedLogEntry> getContent(UUID actor) {
        return this.content.stream()
                .filter(e -> e.getActor().equals(actor))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<ExtendedLogEntry> getUserHistory(UUID uuid) {
        return this.content.stream()
                .filter(e -> e.getType() == Action.Type.USER)
                .filter(e -> e.getActed().isPresent() && e.getActed().get().equals(uuid))
                .collect(ImmutableCollectors.toSortedSet(Comparator.naturalOrder()));
    }

    public SortedSet<ExtendedLogEntry> getGroupHistory(String name) {
        return this.content.stream()
                .filter(e -> e.getType() == Action.Type.GROUP)
                .filter(e -> e.getActedName().equals(name))
                .collect(ImmutableCollectors.toSortedSet(Comparator.naturalOrder()));
    }

    public SortedSet<ExtendedLogEntry> getTrackHistory(String name) {
        return this.content.stream()
                .filter(e -> e.getType() == Action.Type.TRACK)
                .filter(e -> e.getActedName().equals(name))
                .collect(ImmutableCollectors.toSortedSet(Comparator.naturalOrder()));
    }

    public SortedSet<ExtendedLogEntry> getSearch(String query) {
        return this.content.stream()
                .filter(e -> e.matchesSearch(query))
                .collect(ImmutableCollectors.toSortedSet(Comparator.naturalOrder()));
    }

    public static class Builder {
        private final SortedSet<ExtendedLogEntry> content = new TreeSet<>();

        public Builder add(ExtendedLogEntry e) {
            this.content.add(e);
            return this;
        }

        public Log build() {
            return new Log(this.content);
        }
    }

}
