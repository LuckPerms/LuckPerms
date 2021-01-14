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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ImmutableContextSetImpl extends AbstractContextSet implements ImmutableContextSet {
    public static final ImmutableContextSetImpl EMPTY = new ImmutableContextSetImpl(ImmutableSetMultimap.of());

    public static ImmutableContextSet of(String key, String value) {
        key = sanitizeKey(key);
        value = sanitizeValue(value);

        // special case for 'server=global' and 'world=global'
        if (isGlobalServerWorldEntry(key, value)) {
            return EMPTY;
        }

        return new ImmutableContextSetImpl(ImmutableSetMultimap.of(key, sanitizeValue(value)));
    }

    private final ImmutableSetMultimap<String, String> map;
    private final Context[] array;
    private final int hashCode;

    ImmutableContextSetImpl(ImmutableSetMultimap<String, String> contexts) {
        this.map = contexts;
        this.hashCode = this.map.hashCode();

        Set<Map.Entry<String, String>> entries = this.map.entries();
        this.array = new Context[entries.size()];
        int i = 0;
        for (Map.Entry<String, String> e : entries) {
            this.array[i++] = new ContextImpl(e.getKey(), e.getValue());
        }
    }

    @Override
    protected SetMultimap<String, String> backing() {
        return this.map;
    }

    @Override
    protected void copyTo(SetMultimap<String, String> other) {
        other.putAll(this.map);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Deprecated
    @Override // This set is already immutable!
    public @NonNull ImmutableContextSetImpl immutableCopy() {
        return this;
    }

    @Override
    public @NonNull MutableContextSet mutableCopy() {
        return new MutableContextSetImpl(this.map);
    }

    @Override
    public @NonNull Set<Context> toSet() {
        return ImmutableSet.copyOf(this.array);
    }

    @Override
    public @NonNull Map<String, Set<String>> toMap() {
        return Multimaps.asMap(this.map);
    }

    @Deprecated
    @Override
    public @NonNull Map<String, String> toFlattenedMap() {
        ImmutableMap.Builder<String, String> m = ImmutableMap.builder();
        for (Map.Entry<String, String> e : this.map.entries()) {
            m.put(e.getKey(), e.getValue());
        }
        return m.build();
    }

    @Override
    public Context[] toArray() {
        return this.array;
    }

    @Override
    protected boolean otherContainsAll(ContextSet other, ContextSatisfyMode mode) {
        switch (mode) {
            // Use other.contains
            case ALL_VALUES_PER_KEY: {
                Set<Map.Entry<String, String>> entries = this.map.entries();
                for (Map.Entry<String, String> e : entries) {
                    if (!other.contains(e.getKey(), e.getValue())) {
                        return false;
                    }
                }
                return true;
            }

            // Use other.containsAny
            case AT_LEAST_ONE_VALUE_PER_KEY: {
                Set<Map.Entry<String, Collection<String>>> entries = this.map.asMap().entrySet();
                for (Map.Entry<String, Collection<String>> e : entries) {
                    if (!other.containsAny(e.getKey(), e.getValue())) {
                        return false;
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

        // fast(er) path for ImmutableContextSet comparisons
        if (that instanceof ImmutableContextSetImpl) {
            ImmutableContextSetImpl immutableThat = (ImmutableContextSetImpl) that;
            if (this.hashCode != immutableThat.hashCode) return false;
        }

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
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "ImmutableContextSet(contexts=" + this.map + ")";
    }

    public static final class BuilderImpl implements ImmutableContextSet.Builder {
        private ImmutableSetMultimap.Builder<String, String> builder;

        public BuilderImpl() {

        }

        private synchronized ImmutableSetMultimap.Builder<String, String> builder() {
            if (this.builder == null) {
                this.builder = ImmutableSetMultimap.builder();
            }
            return this.builder;
        }

        private void put(String key, String value) {
            // special case for server=global and world=global
            if (isGlobalServerWorldEntry(key, value)) {
                return;
            }
            builder().put(key, value);
        }

        @Override
        public @NonNull BuilderImpl add(@NonNull String key, @NonNull String value) {
            put(sanitizeKey(key), sanitizeValue(value));
            return this;
        }

        @Override
        public @NonNull BuilderImpl addAll(@NonNull ContextSet contextSet) {
            Objects.requireNonNull(contextSet, "contextSet");
            if (contextSet instanceof AbstractContextSet) {
                AbstractContextSet other = ((AbstractContextSet) contextSet);
                if (!other.isEmpty()) {
                    builder().putAll(other.backing());
                }
            } else {
                addAll(contextSet.toSet());
            }
            return this;
        }

        @Override
        public @NonNull ImmutableContextSet build() {
            if (this.builder == null) {
                return EMPTY;
            } else {
                return new ImmutableContextSetImpl(this.builder.build());
            }
        }
    }
}
