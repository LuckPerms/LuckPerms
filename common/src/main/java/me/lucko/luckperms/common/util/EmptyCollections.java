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

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Empty collections that do not throw {@link UnsupportedOperationException} on mutate operations.
 */
public final class EmptyCollections {
    private EmptyCollections() {}

    private static final EmptyList<?> LIST = new EmptyList<>();
    private static final EmptySet<?> SET = new EmptySet<>();
    private static final EmptyMap<?, ?> MAP = new EmptyMap<>();

    @SuppressWarnings("unchecked")
    public static <E> List<E> list() {
        return (List<E>) LIST;
    }

    @SuppressWarnings("unchecked")
    public static <E> Set<E> set() {
        return (Set<E>) SET;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> map() {
        return (Map<K, V>) MAP;
    }

    private static final class EmptyList<E> extends AbstractList<E> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public E get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public E set(int index, E element) {
            return null;
        }

        @Override
        public void add(int index, E element) {

        }

        @Override
        public E remove(int index) {
            throw new IndexOutOfBoundsException();
        }
    }

    private static final class EmptySet<E> extends AbstractSet<E> {
        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean add(E e) {
            return true;
        }
    }

    private static final class EmptyMap<K, V> extends AbstractMap<K, V> {
        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public V put(K key, V value) {
            return null;
        }
    }

}
