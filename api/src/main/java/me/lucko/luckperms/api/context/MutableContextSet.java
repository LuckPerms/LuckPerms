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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A mutable implementation of {@link ContextSet}.
 *
 * @since 2.16
 */
public final class MutableContextSet extends AbstractContextSet implements ContextSet {

    /**
     * Creates a {@link MutableContextSet} from a context pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new MutableContextSet containing one context pair
     * @throws NullPointerException if key or value is null
     */
    @Nonnull
    public static MutableContextSet singleton(@Nonnull String key, @Nonnull String value) {
        checkNotNull(key, "key");
        checkNotNull(value, "value");
        MutableContextSet set = MutableContextSet.create();
        set.add(key, value);
        return set;
    }

    /**
     * Creates a {@link MutableContextSet} from two context pairs.
     *
     * @param key1 the first key
     * @param value1 the first value
     * @param key2 the second key
     * @param value2 the second value
     * @return a new MutableContextSet containing the two pairs
     * @throws NullPointerException if any of the keys or values are null
     * @since 3.1
     */
    @Nonnull
    public static MutableContextSet of(@Nonnull String key1, @Nonnull String value1, @Nonnull String key2, @Nonnull String value2) {
        checkNotNull(key1, "key1");
        checkNotNull(value1, "value1");
        checkNotNull(key2, "key2");
        checkNotNull(value2, "value2");
        MutableContextSet set = create();
        set.add(key1, value1);
        set.add(key2, value2);
        return set;
    }

    /**
     * Creates a {@link MutableContextSet} from an existing {@link Iterable} of {@link Map.Entry}s.
     *
     * @param iterable the iterable to copy from
     * @return a new MutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    @Nonnull
    public static MutableContextSet fromEntries(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
        checkNotNull(iterable, "iterable");
        MutableContextSet set = create();
        set.addAll(iterable);
        return set;
    }

    /**
     * Creates a {@link MutableContextSet} from an existing {@link Map}.
     *
     * @param map the map to copy from
     * @return a new MutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    @Nonnull
    public static MutableContextSet fromMap(@Nonnull Map<String, String> map) {
        checkNotNull(map, "map");
        MutableContextSet set = create();
        set.addAll(map);
        return set;
    }

    /**
     * Creates a {@link MutableContextSet} from an existing {@link Multimap}.
     *
     * @param multimap the multimap to copy from
     * @return a new MutableContextSet representing the pairs in the multimap
     * @throws NullPointerException if the multimap is null
     * @since 2.16
     */
    @Nonnull
    public static MutableContextSet fromMultimap(@Nonnull Multimap<String, String> multimap) {
        checkNotNull(multimap, "multimap");
        MutableContextSet set = create();
        set.addAll(multimap);
        return set;
    }

    /**
     * Creates a new {@link MutableContextSet} from an existing {@link Set}.
     *
     * <p>Only really useful for converting between mutable and immutable types.</p>
     *
     * @param contextSet the context set to copy from
     * @return a new MutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    @Nonnull
    public static MutableContextSet fromSet(@Nonnull ContextSet contextSet) {
        Preconditions.checkNotNull(contextSet, "contextSet");
        MutableContextSet set = create();
        set.addAll(contextSet);
        return set;
    }

    /**
     * Creates a new empty MutableContextSet.
     *
     * @return a new MutableContextSet
     */
    @Nonnull
    public static MutableContextSet create() {
        return new MutableContextSet();
    }

    private final SetMultimap<String, String> map;

    public MutableContextSet() {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    private MutableContextSet(MutableContextSet other) {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create(other.map));
    }

    @Override
    protected Multimap<String, String> backing() {
        return map;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Nonnull
    @Override
    public ImmutableContextSet makeImmutable() {
        // if the map is empty, don't create a new instance
        if (map.isEmpty()) {
            return ImmutableContextSet.empty();
        }
        return new ImmutableContextSet(ImmutableSetMultimap.copyOf(map));
    }

    @Nonnull
    @Override
    public MutableContextSet mutableCopy() {
        return new MutableContextSet(this);
    }

    @Nonnull
    @Override
    public Set<Map.Entry<String, String>> toSet() {
        return ImmutableSet.copyOf(map.entries());
    }

    @Nonnull
    @Override
    public Multimap<String, String> toMultimap() {
        return ImmutableSetMultimap.copyOf(map);
    }

    /**
     * Adds a context to this set.
     *
     * @param key   the key to add
     * @param value the value to add
     * @throws NullPointerException if the key or value is null
     */
    public void add(@Nonnull String key, @Nonnull String value) {
        map.put(sanitizeKey(key), sanitizeValue(value));
    }

    /**
     * Adds a context to this set.
     *
     * @param entry the entry to add
     * @throws NullPointerException if the entry is null
     */
    public void add(@Nonnull Map.Entry<String, String> entry) {
        checkNotNull(entry, "entry");
        add(entry.getKey(), entry.getValue());
    }

    /**
     * Adds the contexts contained in the given {@link Iterable} to this set.
     *
     * @param iterable an iterable of key value context pairs
     * @throws NullPointerException if iterable is null
     */
    public void addAll(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
        for (Map.Entry<String, String> e : checkNotNull(iterable, "iterable")) {
            add(e);
        }
    }

    /**
     * Adds the contexts contained in the given {@link Map} to this set.
     *
     * @param map the map to add from
     * @throws NullPointerException if the map is null
     */
    public void addAll(@Nonnull Map<String, String> map) {
        addAll(checkNotNull(map, "map").entrySet());
    }

    /**
     * Adds the contexts contained in the given {@link Multimap} to this set.
     *
     * @param multimap the multimap to add from
     * @throws NullPointerException if the map is null
     * @since 3.4
     */
    public void addAll(@Nonnull Multimap<String, String> multimap) {
        addAll(checkNotNull(multimap, "multimap").entries());
    }

    /**
     * Adds of of the contexts in another {@link ContextSet} to this set.
     *
     * @param contextSet the set to add from
     * @throws NullPointerException if the contextSet is null
     */
    public void addAll(@Nonnull ContextSet contextSet) {
        checkNotNull(contextSet, "contextSet");
        if (contextSet instanceof AbstractContextSet) {
            AbstractContextSet other = ((AbstractContextSet) contextSet);
            this.map.putAll(other.backing());
        } else {
            addAll(contextSet.toMultimap());
        }
    }

    /**
     * Removes a context from this set.
     *
     * @param key   the key to remove
     * @param value the value to remove (case sensitive)
     * @throws NullPointerException if the key or value is null
     */
    public void remove(@Nonnull String key, @Nonnull String value) {
        map.remove(sanitizeKey(key), sanitizeValue(value));
    }

    /**
     * Removes a context from this set. (case-insensitive)
     *
     * @param key   the key to remove
     * @param value the value to remove
     * @throws NullPointerException if the key or value is null
     */
    public void removeIgnoreCase(@Nonnull String key, @Nonnull String value) {
        String v = sanitizeValue(value);
        Collection<String> strings = map.asMap().get(sanitizeKey(key));
        if (strings != null) {
            strings.removeIf(e -> e.equalsIgnoreCase(v));
        }
    }

    /**
     * Removes all contexts from this set with the given key.
     *
     * @param key the key to remove
     * @throws NullPointerException if the key is null
     */
    public void removeAll(@Nonnull String key) {
        map.removeAll(sanitizeKey(key));
    }

    /**
     * Removes all contexts from the set.
     */
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return "MutableContextSet(contexts=" + this.map + ")";
    }
}
