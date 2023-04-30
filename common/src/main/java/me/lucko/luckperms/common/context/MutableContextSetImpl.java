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

package me.lucko.luckperms.common.context;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;

public final class MutableContextSetImpl extends AbstractContextSet implements MutableContextSet, ContextSet {
    private final SetMultimap<String, String> map;

    public MutableContextSetImpl() {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    MutableContextSetImpl(SetMultimap<String, String> other) {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create(other));
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public @NonNull ImmutableContextSet immutableCopy() {
        // if the map is empty, don't create a new instance
        if (this.map.isEmpty()) {
            return ImmutableContextSetImpl.EMPTY;
        }

        Context[] arr = toArray();
        Arrays.sort(arr);
        return new ImmutableContextSetImpl(arr);
    }

    @Override
    public @NonNull MutableContextSetImpl mutableCopy() {
        synchronized (this.map) {
            return new MutableContextSetImpl(this.map);
        }
    }

    @Override
    public @NonNull ImmutableSet<Context> toSet() {
        ImmutableSet.Builder<Context> builder = ImmutableSet.builder();
        Set<Map.Entry<String, String>> entries = this.map.entries();
        synchronized (this.map) {
            for (Map.Entry<String, String> e : entries) {
                builder.add(new ContextImpl(e.getKey(), e.getValue()));
            }
        }
        return builder.build();
    }

    @Override
    public @NonNull Map<String, Set<String>> toMap() {
        ImmutableMap.Builder<String, Set<String>> builder = ImmutableMap.builder();
        Map<String, Collection<String>> map = this.map.asMap();
        synchronized (this.map) {
            for (Map.Entry<String, Collection<String>> e : map.entrySet()) {
                builder.put(e.getKey(), ImmutableSet.copyOf(e.getValue()));
            }
        }
        return builder.build();
    }

    @Deprecated
    @Override
    public @NonNull Map<String, String> toFlattenedMap() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        Set<Map.Entry<String, String>> entries = this.map.entries();
        synchronized (this.map) {
            for (Map.Entry<String, String> e : entries) {
                builder.put(e.getKey(), e.getValue());
            }
        }
        return builder.build();
    }

    public Context[] toArray() {
        Context[] array;
        synchronized (this.map) {
            Set<Map.Entry<String, String>> entries = this.map.entries();
            array = new Context[entries.size()];
            int i = 0;
            for (Map.Entry<String, String> e : entries) {
                array[i++] = new ContextImpl(e.getKey(), e.getValue());
            }
        }
        return array;
    }

    @Override
    public boolean containsKey(@NonNull String key) {
        return this.map.containsKey(sanitizeKey(key));
    }

    @Override
    public @NonNull Set<String> getValues(@NonNull String key) {
        Collection<String> values = this.map.asMap().get(sanitizeKey(key));
        return values != null ? ImmutableSet.copyOf(values) : ImmutableSet.of();
    }

    @Override
    public boolean contains(@NonNull String key, @NonNull String value) {
        return this.map.containsEntry(sanitizeKey(key), sanitizeValue(value));
    }

    @Override
    public @NonNull Iterator<Context> iterator() {
        return Iterators.forArray(toArray());
    }

    @Override
    public Spliterator<Context> spliterator() {
        return Arrays.spliterator(toArray());
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public void add(@NonNull String key, @NonNull String value) {
        key = sanitizeKey(key);
        value = sanitizeValue(value);

        // special case for server=global and world=global
        if (isGlobalServerWorldEntry(key, value)) {
            return;
        }

        this.map.put(key, value);
    }

    @Override
    public void addAll(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        addAll(contextSet.toSet());
    }

    @Override
    public void remove(@NonNull String key, @NonNull String value) {
        this.map.remove(sanitizeKey(key), sanitizeValue(value));
    }

    @Override
    public void removeAll(@NonNull String key) {
        this.map.removeAll(sanitizeKey(key));
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    protected boolean otherContainsAll(ContextSet other, ContextSatisfyMode mode) {
        switch (mode) {
            // Use other.contains
            case ALL_VALUES_PER_KEY: {
                Set<Map.Entry<String, String>> entries = this.map.entries();
                synchronized (this.map) {
                    for (Map.Entry<String, String> e : entries) {
                        if (!other.contains(e.getKey(), e.getValue())) {
                            return false;
                        }
                    }
                }
                return true;
            }

            // Use other.containsAny
            case AT_LEAST_ONE_VALUE_PER_KEY: {
                Set<Map.Entry<String, Collection<String>>> entries = this.map.asMap().entrySet();
                synchronized (this.map) {
                    for (Map.Entry<String, Collection<String>> e : entries) {
                        if (!other.containsAny(e.getKey(), e.getValue())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            default:
                throw new IllegalArgumentException("Unknown mode: " + mode);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet that = (ContextSet) o;

        return this.size() == that.size() && otherContainsAll(that, ContextSatisfyMode.ALL_VALUES_PER_KEY);
    }

    @Override
    public int hashCode() {
        return this.map.hashCode();
    }

    @Override
    public String toString() {
        return "MutableContextSet(" + this.map + ")";
    }
}
