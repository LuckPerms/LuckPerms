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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A mutable implementation of {@link ContextSet}.
 *
 * @since 2.16
 */
public final class MutableContextSet implements ContextSet {

    /**
     * Make a singleton MutableContextSet from a context pair
     *
     * @param key   the key
     * @param value the value
     * @return a new MutableContextSet containing one KV pair
     * @throws NullPointerException if key or value is null
     */
    public static MutableContextSet singleton(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        MutableContextSet set = new MutableContextSet();
        set.add(key, value);
        return set;
    }

    /**
     * Makes a MutableContextSet from two context pairs
     *
     * @param key1 the first key
     * @param value1 the first value
     * @param key2 the second key
     * @param value2 the second value
     * @return a new MutableContextSet containing the two pairs
     * @throws NullPointerException if any of the keys or values are null
     * @since 3.1
     */
    public static MutableContextSet of(String key1, String value1, String key2, String value2) {
        if (key1 == null) {
            throw new NullPointerException("key1");
        }
        if (value1 == null) {
            throw new NullPointerException("value1");
        }
        if (key2 == null) {
            throw new NullPointerException("key2");
        }
        if (value2 == null) {
            throw new NullPointerException("value2");
        }

        MutableContextSet ret = singleton(key1, value1);
        ret.add(key2, value2);
        return ret;
    }

    /**
     * Creates a MutableContextSet from an existing map
     *
     * @param map the map to copy from
     * @return a new MutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    public static MutableContextSet fromMap(Map<String, String> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(map);
        return set;
    }

    /**
     * Creates a MutableContextSet from an existing iterable of Map Entries
     *
     * @param iterable the iterable to copy from
     * @return a new MutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    public static MutableContextSet fromEntries(Iterable<? extends Map.Entry<String, String>> iterable) {
        if (iterable == null) {
            throw new NullPointerException("iterable");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(iterable);
        return set;
    }

    /**
     * Creates a MutableContextSet from an existing multimap
     *
     * @param multimap the multimap to copy from
     * @return a new MutableContextSet representing the pairs in the multimap
     * @throws NullPointerException if the multimap is null
     */
    public static MutableContextSet fromMultimap(Multimap<String, String> multimap) {
        if (multimap == null) {
            throw new NullPointerException("multimap");
        }

        return fromEntries(multimap.entries());
    }

    /**
     * Creates a new MutableContextSet from an existing set.
     * Only really useful for converting between mutable and immutable types.
     *
     * @param contextSet the context set to copy from
     * @return a new MutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    public static MutableContextSet fromSet(ContextSet contextSet) {
        if (contextSet == null) {
            throw new NullPointerException("contextSet");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(contextSet.toSet());
        return set;
    }

    /**
     * Creates a new empty MutableContextSet.
     *
     * @return a new MutableContextSet
     */
    public static MutableContextSet create() {
        return new MutableContextSet();
    }

    private final SetMultimap<String, String> map;

    public MutableContextSet() {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    }

    private MutableContextSet(Multimap<String, String> contexts) {
        this.map = Multimaps.synchronizedSetMultimap(HashMultimap.create(contexts));
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public ImmutableContextSet makeImmutable() {
        return new ImmutableContextSet(map);
    }

    @Override
    public MutableContextSet mutableCopy() {
        return new MutableContextSet(map);
    }

    @Override
    public Set<Map.Entry<String, String>> toSet() {
        return ImmutableSet.copyOf(map.entries());
    }

    @Override
    public Map<String, String> toMap() {
        ImmutableMap.Builder<String, String> m = ImmutableMap.builder();
        for (Map.Entry<String, String> e : map.entries()) {
            m.put(e.getKey(), e.getValue());
        }

        return m.build();
    }

    @Override
    public Multimap<String, String> toMultimap() {
        return ImmutableSetMultimap.copyOf(map);
    }

    @Override
    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        return map.containsKey(key);
    }

    @Override
    public Set<String> getValues(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        Collection<String> c = map.get(key);
        return c != null && !c.isEmpty() ? ImmutableSet.copyOf(c) : ImmutableSet.of();
    }

    @Override
    public boolean has(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        return map.containsEntry(key, value);
    }

    @Override
    public boolean hasIgnoreCase(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

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

    /**
     * Adds a new key value pair to the set
     *
     * @param key   the key to add
     * @param value the value to add
     * @throws NullPointerException if the key or value is null
     */
    public void add(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        map.put(key.toLowerCase(), value);
    }

    /**
     * Adds a new key value pair to the set
     *
     * @param entry the entry to add
     * @throws NullPointerException if the entry is null
     */
    public void add(Map.Entry<String, String> entry) {
        if (entry == null) {
            throw new NullPointerException("context");
        }

        map.put(entry.getKey().toLowerCase(), entry.getValue());
    }

    /**
     * Adds an iterable containing contexts to the set
     *
     * @param iterable an iterable of key value context pairs
     * @throws NullPointerException if iterable is null
     */
    public void addAll(Iterable<? extends Map.Entry<String, String>> iterable) {
        if (iterable == null) {
            throw new NullPointerException("iterable");
        }

        for (Map.Entry<String, String> e : iterable) {
            this.map.put(e.getKey().toLowerCase(), e.getValue());
        }
    }

    /**
     * Adds the entry set of a map to the set
     *
     * @param map the map to add from
     * @throws NullPointerException if the map is null
     */
    public void addAll(Map<String, String> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }
        addAll(map.entrySet());
    }

    /**
     * Adds of of the values in another ContextSet to this set
     *
     * @param contextSet the set to add from
     * @throws NullPointerException if the contextSet is null
     */
    public void addAll(ContextSet contextSet) {
        if (contextSet == null) {
            throw new NullPointerException("contextSet");
        }

        this.map.putAll(contextSet.toMultimap());
    }

    /**
     * Remove a key value pair from this set
     *
     * @param key   the key to remove
     * @param value the value to remove (case sensitive)
     * @throws NullPointerException if the key or value is null
     */
    public void remove(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        map.entries().removeIf(entry -> entry.getKey().equals(key) && entry.getValue().equals(value));
    }

    /**
     * Same as {@link #remove(String, String)}, except ignores the case of the value
     *
     * @param key   the key to remove
     * @param value the value to remove
     * @throws NullPointerException if the key or value is null
     */
    public void removeIgnoreCase(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        map.entries().removeIf(e -> e.getKey().equalsIgnoreCase(key) && e.getValue().equalsIgnoreCase(value));
    }

    /**
     * Removes all pairs with the given key
     *
     * @param key the key to remove
     * @throws NullPointerException if the key is null
     */
    public void removeAll(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        map.removeAll(key.toLowerCase());
    }

    /**
     * Clears the set
     */
    public void clear() {
        map.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet other = (ContextSet) o;

        final Multimap<String, String> thisContexts = this.toMultimap();
        final Multimap<String, String> otherContexts = other.toMultimap();
        return thisContexts == null ? otherContexts == null : thisContexts.equals(otherContexts);
    }

    @Override
    public int hashCode() {
        return 59 + (this.map == null ? 43 : this.map.hashCode());
    }

    @Override
    public String toString() {
        return "MutableContextSet(contexts=" + this.map + ")";
    }
}
