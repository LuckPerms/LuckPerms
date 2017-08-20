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

package me.lucko.luckperms.api.context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * An immutable implementation of {@link ContextSet}.
 *
 * @since 2.16
 */
public final class ImmutableContextSet implements ContextSet {
    private static final ImmutableContextSet EMPTY = new ImmutableContextSet(ImmutableSetMultimap.of());

    /**
     * Creates an ImmutableContextSet from a context pair
     *
     * @param key   the key
     * @param value the value
     * @return a new ImmutableContextSet containing one KV pair
     * @throws NullPointerException if key or value is null
     */
    @Nonnull
    public static ImmutableContextSet singleton(@Nonnull String key, @Nonnull String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        return new ImmutableContextSet(ImmutableSetMultimap.of(key.toLowerCase(), value));
    }

    /**
     * Creates an ImmutableContextSet from two context pairs
     *
     * @param key1 the first key
     * @param value1 the first value
     * @param key2 the second key
     * @param value2 the second value
     * @return a new ImmutableContextSet containing the two pairs
     * @throws NullPointerException if any of the keys or values are null
     * @since 3.1
     */
    @Nonnull
    public static ImmutableContextSet of(@Nonnull String key1, @Nonnull String value1, @Nonnull String key2, @Nonnull String value2) {
        Preconditions.checkNotNull(key1, "key1");
        Preconditions.checkNotNull(value1, "value1");
        Preconditions.checkNotNull(key2, "key2");
        Preconditions.checkNotNull(value2, "value2");
        return new ImmutableContextSet(ImmutableSetMultimap.of(key1.toLowerCase(), value1, key2.toLowerCase(), value2));
    }

    /**
     * Creates an ImmutableContextSet from an existing map
     *
     * @param map the map to copy from
     * @return a new ImmutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    @Nonnull
    public static ImmutableContextSet fromMap(@Nonnull Map<String, String> map) {
        Preconditions.checkNotNull(map, "map");

        ImmutableSetMultimap.Builder<String, String> b = ImmutableSetMultimap.builder();
        for (Map.Entry<String, String> e : map.entrySet()) {
            b.put(e.getKey().toLowerCase(), e.getValue());
        }

        return new ImmutableContextSet(b.build());
    }

    /**
     * Creates an ImmutableContextSet from an existing iterable of Map Entries
     *
     * @param iterable the iterable to copy from
     * @return a new ImmutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    @Nonnull
    public static ImmutableContextSet fromEntries(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
        Preconditions.checkNotNull(iterable, "iterable");
        return MutableContextSet.fromEntries(iterable).makeImmutable();
    }

    /**
     * Creates an ImmutableContextSet from an existing multimap
     *
     * @param multimap the multimap to copy from
     * @return a new ImmutableContextSet representing the pairs in the multimap
     * @throws NullPointerException if the multimap is null
     */
    @Nonnull
    public static ImmutableContextSet fromMultimap(@Nonnull Multimap<String, String> multimap) {
        Preconditions.checkNotNull(multimap, "multimap");
        return MutableContextSet.fromMultimap(multimap).makeImmutable();
    }

    /**
     * Creates a new ImmutableContextSet from an existing set.
     * Only really useful for converting between mutable and immutable types.
     *
     * @param contextSet the context set to copy from
     * @return a new ImmutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    @Nonnull
    public static ImmutableContextSet fromSet(@Nonnull ContextSet contextSet) {
        return Preconditions.checkNotNull(contextSet, "contextSet").makeImmutable();
    }

    /**
     * Creates an new empty ContextSet.
     *
     * @return a new ContextSet
     */
    @Nonnull
    public static ImmutableContextSet empty() {
        return EMPTY;
    }

    private final SetMultimap<String, String> map;

    ImmutableContextSet(Multimap<String, String> contexts) {
        this.map = ImmutableSetMultimap.copyOf(contexts);
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    @Deprecated // This set is already immutable!
    @Nonnull
    public ImmutableContextSet makeImmutable() {
        return this;
    }

    @Override
    @Nonnull
    public MutableContextSet mutableCopy() {
        return MutableContextSet.fromSet(this);
    }

    @Override
    @Nonnull
    public Set<Map.Entry<String, String>> toSet() {
        return ImmutableSet.copyOf(map.entries());
    }

    @Override
    @Nonnull
    public Map<String, String> toMap() {
        ImmutableMap.Builder<String, String> m = ImmutableMap.builder();
        for (Map.Entry<String, String> e : map.entries()) {
            m.put(e.getKey(), e.getValue());
        }

        return m.build();
    }

    @Override
    @Nonnull
    public Multimap<String, String> toMultimap() {
        return map;
    }

    @Override
    public boolean containsKey(@Nonnull String key) {
        Preconditions.checkNotNull(key, "key");
        return map.containsKey(key);
    }

    @Override
    public Set<String> getValues(@Nonnull String key) {
        Preconditions.checkNotNull(key, "key");
        Collection<String> c = map.get(key);
        return c != null && !c.isEmpty() ? ImmutableSet.copyOf(c) : ImmutableSet.of();
    }

    @Override
    public boolean has(@Nonnull String key, @Nonnull String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");
        return map.containsEntry(key, value);
    }

    @Override
    public boolean hasIgnoreCase(@Nonnull String key, @Nonnull String value) {
        Preconditions.checkNotNull(key, "key");
        Preconditions.checkNotNull(value, "value");

        Collection<String> c = map.get(key);
        if (c == null || c.isEmpty()) {
            return false;
        }

        for (String val : c) {
            if (val.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet other = (ContextSet) o;

        final Multimap<String, String> thisContexts = this.toMultimap();
        final Multimap<String, String> otherContexts = other.toMultimap();
        return thisContexts.equals(otherContexts);
    }

    @Override
    public int hashCode() {
        return 59 + (this.map == null ? 43 : this.map.hashCode());
    }

    @Override
    public String toString() {
        return "ImmutableContextSet(contexts=" + this.map + ")";
    }
}
