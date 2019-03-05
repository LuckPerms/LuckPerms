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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Collector;

public final class ImmutableCollectors {
    private ImmutableCollectors() {}

    private static final Collector<Object, ImmutableList.Builder<Object>, ImmutableList<Object>> LIST = Collector.of(
            ImmutableList.Builder::new,
            ImmutableList.Builder::add,
            (l, r) -> l.addAll(r.build()),
            ImmutableList.Builder::build
    );

    private static final Collector<Object, ImmutableSet.Builder<Object>, ImmutableSet<Object>> SET = Collector.of(
            ImmutableSet.Builder::new,
            ImmutableSet.Builder::add,
            (l, r) -> l.addAll(r.build()),
            ImmutableSet.Builder::build
    );

    public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toList() {
        //noinspection unchecked
        return (Collector) LIST;
    }

    public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toSet() {
        //noinspection unchecked
        return (Collector) SET;
    }

    public static <T extends Enum<T>> Collector<T, EnumSet<T>, ImmutableSet<T>> toEnumSet(Class<T> clazz) {
        return Collector.of(
                () -> EnumSet.noneOf(clazz),
                EnumSet::add,
                (l, r) -> { l.addAll(r); return l; },
                ImmutableSet::copyOf
        );
    }

    public static <E> Collector<E, ?, ImmutableSortedSet<E>> toSortedSet(Comparator<? super E> comparator) {
        return Collector.of(
                () -> new ImmutableSortedSet.Builder<E>(comparator),
                ImmutableSortedSet.Builder::add,
                (l, r) -> l.addAll(r.build()),
                ImmutableSortedSet.Builder::build
        );
    }

    public static <T, K, V> Collector<T, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return Collector.of(
                ImmutableMap.Builder<K, V>::new,
                (r, t) -> r.put(keyMapper.apply(t), valueMapper.apply(t)),
                (l, r) -> l.putAll(r.build()),
                ImmutableMap.Builder::build
        );
    }

    public static <T, K extends Enum<K>, V> Collector<T, EnumMap<K, V>, ImmutableMap<K, V>> toEnumMap(Class<K> clazz, Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return Collector.of(
                () -> new EnumMap<>(clazz),
                (r, t) -> r.put(keyMapper.apply(t), valueMapper.apply(t)),
                (l, r) -> { l.putAll(r); return l; },
                ImmutableMap::copyOf
        );
    }

}
