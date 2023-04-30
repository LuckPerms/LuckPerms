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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.actionlog.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

public class Log {
    private static final Log EMPTY = new Log(ImmutableList.of());

    public static Builder builder() {
        return new Builder();
    }

    public static Log empty() {
        return EMPTY;
    }

    private final SortedSet<LoggedAction> content;

    Log(List<LoggedAction> content) {
        this.content = ImmutableSortedSet.copyOf(content);
    }

    public SortedSet<LoggedAction> getContent() {
        return this.content;
    }

    public SortedSet<LoggedAction> getContent(UUID actor) {
        return this.content.stream()
                .filter(e -> e.getSource().getUniqueId().equals(actor))
                .collect(ImmutableCollectors.toSortedSet());
    }

    public SortedSet<LoggedAction> getUserHistory(UUID uniqueId) {
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.USER)
                .filter(e -> e.getTarget().getUniqueId().isPresent() && e.getTarget().getUniqueId().get().equals(uniqueId))
                .collect(ImmutableCollectors.toSortedSet());
    }

    public SortedSet<LoggedAction> getGroupHistory(String name) {
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.GROUP)
                .filter(e -> e.getTarget().getName().equals(name))
                .collect(ImmutableCollectors.toSortedSet());
    }

    public SortedSet<LoggedAction> getTrackHistory(String name) {
        return this.content.stream()
                .filter(e -> e.getTarget().getType() == Action.Target.Type.TRACK)
                .filter(e -> e.getTarget().getName().equals(name))
                .collect(ImmutableCollectors.toSortedSet());
    }

    public SortedSet<LoggedAction> getSearch(String query) {
        return this.content.stream()
                .filter(e -> e.matchesSearch(query))
                .collect(ImmutableCollectors.toSortedSet());
    }

    public static class Builder {
        private final List<LoggedAction> content = new ArrayList<>();

        public Builder add(LoggedAction e) {
            this.content.add(e);
            return this;
        }

        public Log build() {
            if (this.content.isEmpty()) {
                return EMPTY;
            }
            return new Log(this.content);
        }
    }

}
