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
import me.lucko.luckperms.common.filter.PageParameters;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LogPage {
    public static LogPage of(List<LoggedAction> content, @Nullable PageParameters params, int totalEntries) {
        return new LogPage(content, params, totalEntries);
    }

    private final List<LoggedAction> content;
    private final @Nullable PageParameters params;
    private final int totalEntries;

    LogPage(List<LoggedAction> content, @Nullable PageParameters params, int totalEntries) {
        this.content = ImmutableList.copyOf(content);
        this.params = params;
        this.totalEntries = totalEntries;
    }

    public List<LoggedAction> getContent() {
        return this.content;
    }

    public List<Entry<LoggedAction>> getNumberedContent() {
        int startIndex = this.params != null
                ? this.params.pageSize() * (this.params.pageNumber() - 1)
                : 0;

        List<Entry<LoggedAction>> numberedContent = new ArrayList<>();
        for (int i = 0; i < this.content.size(); i++) {
            int index = startIndex + i + 1;
            numberedContent.add(new Entry<>(index, this.content.get(i)));
        }
        return numberedContent;
    }

    public int getTotalEntries() {
        return this.totalEntries;
    }

    public static final class Entry<T> {
        private final int position;
        private final T value;

        public Entry(int position, T value) {
            this.position = position;
            this.value = value;
        }

        public int position() {
            return this.position;
        }

        public T value() {
            return this.value;
        }

        @Override
        public String toString() {
            return this.position + ": " + this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry)) return false;
            Entry<?> entry = (Entry<?>) o;
            return this.position == entry.position && Objects.equals(this.value, entry.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.position, this.value);
        }
    }

}
