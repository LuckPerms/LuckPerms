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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimaps;
import me.lucko.luckperms.common.context.comparator.ContextComparator;
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

public final class ImmutableContextSetImpl extends AbstractContextSet implements ImmutableContextSet, ContextSet {
    public static final ImmutableContextSetImpl EMPTY = new ImmutableContextSetImpl(new Context[0]);

    public static ImmutableContextSet of(String key, String value) {
        key = sanitizeKey(key);
        value = sanitizeValue(value);

        // special case for 'server=global' and 'world=global'
        if (isGlobalServerWorldEntry(key, value)) {
            return EMPTY;
        }

        return new ImmutableContextSetImpl(new Context[]{new ContextImpl(key, value)});
    }

    private final Context[] array;
    private final int size;
    private final int hashCode;

    private ImmutableSetMultimap<String, String> cachedMap;

    ImmutableContextSetImpl(Context[] contexts) {
        this.array = contexts; // always sorted
        this.size = this.array.length;
        this.hashCode = Arrays.hashCode(this.array);
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

    public ImmutableSetMultimap<String, String> toMultimap() {
        if (this.cachedMap == null) {
            ImmutableSetMultimap.Builder<String, String> builder = ImmutableSetMultimap.builder();
            for (Context entry : this.array) {
                builder.put(entry.getKey(), entry.getValue());
            }
            this.cachedMap = builder.build();
        }
        return this.cachedMap;
    }

    @Override
    public @NonNull MutableContextSet mutableCopy() {
        return new MutableContextSetImpl(toMultimap());
    }

    @Override
    public @NonNull Set<Context> toSet() {
        return ImmutableSet.copyOf(this.array);
    }

    @Override
    public @NonNull Map<String, Set<String>> toMap() {
        return Multimaps.asMap(toMultimap());
    }

    @Deprecated
    @Override
    public @NonNull Map<String, String> toFlattenedMap() {
        ImmutableMap.Builder<String, String> m = ImmutableMap.builder();
        for (Context e : this.array) {
            m.put(e.getKey(), e.getValue());
        }
        return m.build();
    }

    public Context[] toArray() {
        return this.array; // only used read-only & internally
    }

    @Override
    protected boolean otherContainsAll(ContextSet other, ContextSatisfyMode mode) {
        switch (mode) {
            // Use other.contains
            case ALL_VALUES_PER_KEY: {
                for (Context e : this.array) {
                    if (!other.contains(e.getKey(), e.getValue())) {
                        return false;
                    }
                }
                return true;
            }

            // Use other.containsAny
            case AT_LEAST_ONE_VALUE_PER_KEY: {
                // exploit the ordered nature to only scan through the array once.
                Context[] array = this.array;
                for (int i = 0, len = array.length; i < len; i++) {
                    Context e = array[i];

                    boolean otherContains = other.contains(e.getKey(), e.getValue());
                    if (otherContains) {
                        // skip forward past any other entries with the same key
                        while (i+1 < len && array[i+1].getKey().equals(e.getKey())) {
                            i++;
                        }
                    } else {
                        // if this is the last one of the key, return false
                        int next = i + 1;
                        if (next >= len || !array[next].getKey().equals(e.getKey())) {
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

        // fast(er) path for ImmutableContextSet comparisons
        if (that instanceof ImmutableContextSetImpl) {
            ImmutableContextSetImpl immutableThat = (ImmutableContextSetImpl) that;
            if (this.hashCode != immutableThat.hashCode) return false;
            return Arrays.equals(this.array, immutableThat.array);
        }

        return this.size() == that.size() && otherContainsAll(that, ContextSatisfyMode.ALL_VALUES_PER_KEY);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "ImmutableContextSet(" + Arrays.toString(this.array) + ")";
    }

    @Override
    public boolean containsKey(@NonNull String key) {
        Objects.requireNonNull(key, "key");
        return Arrays.binarySearch(this.array, new ContextImpl(key, null), ContextComparator.ONLY_KEY) >= 0;
    }

    @Override
    public @NonNull Set<String> getValues(@NonNull String key) {
        Collection<String> values = toMap().get(sanitizeKey(key));
        return values != null ? ImmutableSet.copyOf(values) : ImmutableSet.of();
    }

    @Override
    public boolean contains(@NonNull Context entry) {
        Objects.requireNonNull(entry, "entry");
        return Arrays.binarySearch(this.array, entry) >= 0;
    }

    @Override
    public boolean contains(@NonNull String key, @NonNull String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        return contains(new ContextImpl(key, value));
    }

    @Override
    public @NonNull Iterator<Context> iterator() {
        return Iterators.forArray(this.array);
    }

    @Override
    public Spliterator<Context> spliterator() {
        return Arrays.spliterator(this.array);
    }

    @Override
    public boolean isEmpty() {
        return this.size == 0;
    }

    @Override
    public int size() {
        return this.size;
    }

    public static final class BuilderImpl implements ImmutableContextSet.Builder {
        private static final int INITIAL_SIZE = 16;
        private Context[] builder = EMPTY.array;
        private int size = 0;

        public BuilderImpl() {

        }

        private void put(String key, String value) {
            ContextImpl context = new ContextImpl(key, value);

            int pos = Arrays.binarySearch(this.builder, 0, this.size, context);
            if (pos >= 0) {
                return;
            }

            int insertPos = -pos - 1;

            Context[] dest;
            if (this.builder.length == this.size) {
                // grow
                dest = new Context[Math.max(this.builder.length * 2, INITIAL_SIZE)];
                System.arraycopy(this.builder, 0, dest, 0, insertPos);
            } else {
                dest = this.builder;
            }

            System.arraycopy(this.builder, insertPos, dest, insertPos + 1, this.size - insertPos); // shift
            dest[insertPos] = context; // insert

            this.size++;
            this.builder = dest;
        }

        @Override
        public @NonNull BuilderImpl add(@NonNull String key, @NonNull String value) {
            key = sanitizeKey(key);
            value = sanitizeValue(value);

            // special case for server=global and world=global
            if (isGlobalServerWorldEntry(key, value)) {
                return this;
            }

            put(key, value);
            return this;
        }

        @Override
        public @NonNull BuilderImpl addAll(@NonNull ContextSet contextSet) {
            Objects.requireNonNull(contextSet, "contextSet");
            addAll(contextSet.toSet());
            return this;
        }

        @Override
        public @NonNull ImmutableContextSet build() {
            if (this.builder.length == 0) {
                return EMPTY;
            } else {
                return new ImmutableContextSetImpl(Arrays.copyOf(this.builder, this.size));
            }
        }
    }
}
