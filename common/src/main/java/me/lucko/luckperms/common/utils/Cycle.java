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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A cycle of elements, backed by a list. All operations are thread safe.
 *
 * @param <E> the element type
 */
public class Cycle<E> {

    /**
     * The list that backs this instance
     */
    private final List<E> objects;

    /**
     * The number of elements in the cycle
     */
    private final int size;

    /**
     * The current position of the cursor
     */
    private final AtomicInteger cursor = new AtomicInteger(0);

    public Cycle(List<E> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("List of objects cannot be null/empty.");
        }
        this.objects = ImmutableList.copyOf(objects);
        this.size = this.objects.size();
    }

    public int cursor() {
        return this.cursor.get();
    }

    public E current() {
        return this.objects.get(cursor());
    }

    public E next() {
        return this.objects.get(this.cursor.updateAndGet(i -> {
            int n = i + 1;
            if (n >= this.size) {
                return 0;
            }
            return n;
        }));
    }

    public E previous() {
        return this.objects.get(this.cursor.updateAndGet(i -> {
            if (i == 0) {
                return this.size - 1;
            }
            return i - 1;
        }));
    }

    public List<E> getBacking() {
        return this.objects;
    }
}