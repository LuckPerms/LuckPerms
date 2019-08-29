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

package me.lucko.luckperms.common.context.contextset;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;

public final class MutableContextSetImpl extends AbstractContextSet implements MutableContextSet {
    private final SetMultimap<String, String> map;

    public MutableContextSetImpl() {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    MutableContextSetImpl(SetMultimap<String, String> other) {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create(other));
    }

    @Override
    protected SetMultimap<String, String> backing() {
        return this.map;
    }

    @Override
    protected void copyTo(SetMultimap<String, String> other) {
        synchronized (this.map) {
            other.putAll(this.map);
        }
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
        synchronized (this.map) {
            return new ImmutableContextSetImpl(ImmutableSetMultimap.copyOf(this.map));
        }
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

    private ImmutableList<Context> toList() {
        Set<Map.Entry<String, String>> entries = this.map.entries();
        Context[] array;
        synchronized (this.map) {
            array = new Context[entries.size()];
            int i = 0;
            for (Map.Entry<String, String> e : entries) {
                array[i++] = new ContextImpl(e.getKey(), e.getValue());
            }
        }
        return ImmutableList.copyOf(array);
    }

    @Override
    public @NonNull Iterator<Context> iterator() {
        return toList().iterator();
    }

    @Override
    public Spliterator<Context> spliterator() {
        return toList().spliterator();
    }

    @Override
    public void add(@NonNull String key, @NonNull String value) {
        this.map.put(sanitizeKey(key), sanitizeValue(value));
    }

    @Override
    public void addAll(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        if (contextSet instanceof AbstractContextSet) {
            AbstractContextSet other = ((AbstractContextSet) contextSet);
            other.copyTo(this.map);
        } else {
            addAll(contextSet.toSet());
        }
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
    public boolean isSatisfiedBy(@NonNull ContextSet other) {
        if (this == other) {
            return true;
        }

        Objects.requireNonNull(other, "other");
        if (this.isEmpty()) {
            // this is empty, so is therefore always satisfied.
            return true;
        } else if (other.isEmpty()) {
            // this set isn't empty, but the other one is
            return false;
        } else if (this.size() > other.size()) {
            // this set has more unique entries than the other set, so there's no way this can be satisfied.
            return false;
        } else {
            // neither are empty, we need to compare the individual entries
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
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet that = (ContextSet) o;

        final Multimap<String, String> thatBacking;
        if (that instanceof AbstractContextSet) {
            thatBacking = ((AbstractContextSet) that).backing();
        } else {
            Map<String, Set<String>> thatMap = that.toMap();
            ImmutableSetMultimap.Builder<String, String> thatBuilder = ImmutableSetMultimap.builder();
            for (Map.Entry<String, Set<String>> e : thatMap.entrySet()) {
                thatBuilder.putAll(e.getKey(), e.getValue());
            }
            thatBacking = thatBuilder.build();
        }

        return backing().equals(thatBacking);
    }

    @Override
    public String toString() {
        return "MutableContextSet(contexts=" + this.map + ")";
    }
}
