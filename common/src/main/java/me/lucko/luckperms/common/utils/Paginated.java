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

package me.lucko.luckperms.common.utils;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

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

    public Paginated(Stream<T> content) {
        this.content = content.collect(ImmutableCollectors.toList());
    }

    public List<T> getContent() {
        return this.content;
    }

    public int getMaxPages(int entriesPerPage) {
        return (int) Math.ceil((double) this.content.size() / (double) entriesPerPage);
    }

    public SortedMap<Integer, T> getPage(int pageNo, int entries) {
        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo cannot be less than 1: " + pageNo);
        }

        int minimumEntries = ((pageNo * entries) - entries) + 1;
        if (this.content.size() < minimumEntries) {
            throw new IllegalStateException("Content does not contain that many elements. " +
                    "Requested: " + minimumEntries + ", Size: " + this.content.size());
        }

        final SortedMap<Integer, T> out = new TreeMap<>();

        final int max = minimumEntries + entries - 1;
        int index = 0;
        for (T e : this.content) {
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

}
