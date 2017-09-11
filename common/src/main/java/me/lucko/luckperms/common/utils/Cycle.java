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

/**
 * A cycle of elements, backed by a list. All operations are thread safe.
 *
 * @param <E> the element type
 */
public class Cycle<E> {
    protected final List<E> objects;
    protected int index = 0;

    public Cycle(List<E> objects) {
        if (objects == null || objects.isEmpty()) {
            throw new IllegalArgumentException("List of objects cannot be null/empty.");
        }
        this.objects = ImmutableList.copyOf(objects);
    }

    public int getIndex() {
        return index;
    }

    public E current() {
        synchronized (this) {
            return objects.get(index);
        }
    }

    public E next() {
        synchronized (this) {
            index++;
            index = index > objects.size() - 1 ? 0 : index;

            return objects.get(index);
        }
    }

    public E back() {
        synchronized (this) {
            index--;
            index = index == -1 ? objects.size() - 1 : index;

            return objects.get(index);
        }
    }

    public List<E> getBacking() {
        return objects;
    }
}
