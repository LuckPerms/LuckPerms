/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Holds contexts.
 * All contained contexts are immutable, and unlike {@link MutableContextSet}, contexts cannot be added or removed.
 *
 * @since 2.13
 */
public class ContextSet {

    /**
     * Make a singleton ContextSet from a context pair
     * @param key the key
     * @param value the value
     * @return a new ContextSet containing one KV pair
     * @throws NullPointerException if key or value is null
     */
    public static ContextSet singleton(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        MutableContextSet set = new MutableContextSet();
        set.add(key, value);
        return set.immutableCopy();
    }

    /**
     * Creates a ContextSet from an existing map
     * @param map the map to copy from
     * @return a new ContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    public static ContextSet fromMap(Map<String, String> map) {
        if (map == null) {
            throw new NullPointerException("map");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(map);
        return set.immutableCopy();
    }

    /**
     * Creates a ContextSet from an existing iterable of Map Entries
     * @param iterable the iterable to copy from
     * @return a new ContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    public static ContextSet fromEntries(Iterable<Map.Entry<String, String>> iterable) {
        if (iterable == null) {
            throw new NullPointerException("iterable");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(iterable);
        return set.immutableCopy();
    }

    /**
     * Creates a new ContextSet from an existing set.
     * Only really useful for converting between mutable and immutable types.
     * @param contextSet the context set to copy from
     * @return a new ContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    public static ContextSet fromSet(ContextSet contextSet) {
        if (contextSet == null) {
            throw new NullPointerException("contextSet");
        }

        MutableContextSet set = new MutableContextSet();
        set.addAll(contextSet.toSet());
        return set.immutableCopy();
    }

    /**
     * Creates a new empty ContextSet.
     * @return a new ContextSet
     */
    public static ContextSet empty() {
        return new ContextSet();
    }

    final Set<Map.Entry<String, String>> contexts;

    public ContextSet() {
        this.contexts = new HashSet<>();
    }

    protected ContextSet(Set<Map.Entry<String, String>> contexts) {
        this.contexts = contexts;
    }

    /**
     * Check to see if this set is in an immutable form
     * @return true if the set is immutable
     */
    public boolean isImmutable() {
        return true;
    }

    /**
     * If the set is mutable, this method will return an immutable copy. Otherwise just returns itself.
     * @return an immutable ContextSet
     */
    public ContextSet makeImmutable() {
        return this;
    }

    /**
     * Converts this ContextSet to an immutable {@link Set} of {@link Map.Entry}s.
     * @return an immutable set
     */
    public Set<Map.Entry<String, String>> toSet() {
        synchronized (contexts) {
            return ImmutableSet.copyOf(contexts);
        }
    }

    /**
     * Converts this ContextSet to an immutable {@link Map}
     *
     * <b>NOTE: Use of this method may result in data being lost. ContextSets can contain lots of different values for
     * one key.</b>
     *
     * @return an immutable map
     */
    public Map<String, String> toMap() {
        synchronized (contexts) {
            return ImmutableMap.copyOf(contexts.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    /**
     * Check if the set contains at least one value for the given key.
     * @param key the key to check for
     * @return true if the set contains a value for the key
     * @throws NullPointerException if the key is null
     */
    public boolean containsKey(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        synchronized (contexts) {
            for (Map.Entry<String, String> e : contexts) {
                if (e.getKey().equalsIgnoreCase(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a set of all of the values mapped to the given key
     * @param key the key to find values for
     * @return a set of values
     * @throws NullPointerException if the key is null
     */
    public Set<String> getValues(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        synchronized (contexts) {
            return ImmutableSet.copyOf(contexts.stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(key))
                    .map(Map.Entry::getValue)
                    .collect(Collectors.toSet())
            );
        }
    }

    /**
     * Check if thr set contains a given key mapped to a given value
     * @param key the key to look for
     * @param value the value to look for (case sensitive)
     * @return true if the set contains the KV pair
     * @throws NullPointerException if the key or value is null
     */
    public boolean has(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        synchronized (contexts) {
            for (Map.Entry<String, String> e : contexts) {
                if (!e.getKey().equalsIgnoreCase(key)) {
                    continue;
                }

                if (!e.getValue().equals(value)) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Same as {@link #has(String, String)}, except ignores the case of the value.
     * @param key the key to look for
     * @param value the value to look for
     * @return true if the set contains the KV pair
     * @throws NullPointerException if the key or value is null
     */
    public boolean hasIgnoreCase(String key, String value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }

        synchronized (contexts) {
            for (Map.Entry<String, String> e : contexts) {
                if (!e.getKey().equalsIgnoreCase(key)) {
                    continue;
                }

                if (!e.getValue().equalsIgnoreCase(value)) {
                    continue;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Check if the set is empty
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        synchronized (contexts) {
            return contexts.isEmpty();
        }
    }

    /**
     * Gets the number of key-value context pairs in the set
     * @return the size of the set
     */
    public int size() {
        synchronized (contexts) {
            return contexts.size();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextSet)) return false;
        final ContextSet other = (ContextSet) o;

        final Object thisContexts = this.contexts;
        final Object otherContexts = other.contexts;
        return thisContexts == null ? otherContexts == null : thisContexts.equals(otherContexts);
    }

    @Override
    public int hashCode() {
        return 59 + (this.contexts == null ? 43 : this.contexts.hashCode());
    }
}
