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

package me.lucko.luckperms.common.util;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * A simple pagination utility
 *
 * @param <T> the element type
 */
public class Paginated<T> {
    private final List<T> content;

    public Paginated(Collection<T> content) {
        this.content = ImmutableList.copyOf(content);
    }

    public List<T> getContent() {
        return this.content;
    }

    public int getMaxPages(int entriesPerPage) {
        return (int) Math.ceil((double) this.content.size() / (double) entriesPerPage);
    }

    public List<Entry<T>> getPage(int pageNo, int pageSize) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo cannot be less than 1: " + pageNo);
        }

        int first = (pageNo - 1) * pageSize;
        if (this.content.size() <= first) {
            throw new IllegalStateException("Content does not contain that many elements. (requested page: " + pageNo +
                    ", page size: " + pageSize + ", page first index: " + first + ", content size: " + this.content.size() + ")");
        }

        int last = first + pageSize - 1;
        List<Entry<T>> out = new ArrayList<>(pageSize);

        for (int i = first; i <= last && i < this.content.size(); i++) {
            out.add(new Entry<>(i + 1, this.content.get(i)));
        }

        return out;
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
