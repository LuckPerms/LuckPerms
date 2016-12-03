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

import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Set;

/**
 * Holds contexts.
 * Implementations may be either mutable or immutable.
 *
 * @since 2.13
 */
public interface ContextSet {

    /**
     * Make a singleton ImmutableContextSet from a context pair
     *
     * @param key   the key
     * @param value the value
     * @return a new ImmutableContextSet containing one KV pair
     * @throws NullPointerException if key or value is null
     */
    static ImmutableContextSet singleton(String key, String value) {
        return ImmutableContextSet.singleton(key, value);
    }

    /**
     * Creates a ImmutableContextSet from an existing map
     *
     * @param map the map to copy from
     * @return a new ImmutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    static ImmutableContextSet fromMap(Map<String, String> map) {
        return ImmutableContextSet.fromMap(map);
    }

    /**
     * Creates a ImmutableContextSet from an existing iterable of Map Entries
     *
     * @param iterable the iterable to copy from
     * @return a new ImmutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    static ImmutableContextSet fromEntries(Iterable<? extends Map.Entry<String, String>> iterable) {
        return ImmutableContextSet.fromEntries(iterable);
    }

    /**
     * Creates a ImmutableContextSet from an existing multimap
     *
     * @param multimap the multimap to copy from
     * @return a new ImmutableContextSet representing the pairs in the multimap
     * @throws NullPointerException if the multimap is null
     * @since 2.16
     */
    static ImmutableContextSet fromMultimap(Multimap<String, String> multimap) {
        return ImmutableContextSet.fromMultimap(multimap);
    }

    /**
     * Creates a new ImmutableContextSet from an existing set.
     * Only really useful for converting between mutable and immutable types.
     *
     * @param contextSet the context set to copy from
     * @return a new ImmutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    static ImmutableContextSet fromSet(ContextSet contextSet) {
        return ImmutableContextSet.fromSet(contextSet);
    }

    /**
     * Creates a new empty ImmutableContextSet.
     *
     * @return a new ImmutableContextSet
     */
    static ImmutableContextSet empty() {
        return ImmutableContextSet.empty();
    }

    /**
     * Check to see if this set is in an immutable form
     *
     * @return true if the set is immutable
     */
    boolean isImmutable();

    /**
     * If the set is mutable, this method will return an immutable copy. Otherwise just returns itself.
     *
     * @return an immutable ContextSet
     */
    ImmutableContextSet makeImmutable();

    /**
     * Creates a mutable copy of this set.
     *
     * @return a mutable ContextSet
     * @since 2.16
     */
    MutableContextSet mutableCopy();

    /**
     * Converts this ContextSet to an immutable {@link Set} of {@link Map.Entry}s.
     *
     * @return an immutable set
     */
    Set<Map.Entry<String, String>> toSet();

    /**
     * Converts this ContextSet to an immutable {@link Map}
     *
     * <b>NOTE: Use of this method may result in data being lost. ContextSets can contain lots of different values for
     * one key.</b>
     *
     * @return an immutable map
     */
    Map<String, String> toMap();

    /**
     * Converts this ContextSet to an immutable {@link Multimap}
     *
     * @return a multimap
     * @since 2.16
     */
    Multimap<String, String> toMultimap();

    /**
     * Check if the set contains at least one value for the given key.
     *
     * @param key the key to check for
     * @return true if the set contains a value for the key
     * @throws NullPointerException if the key is null
     */
    boolean containsKey(String key);

    /**
     * Gets a set of all of the values mapped to the given key
     *
     * @param key the key to find values for
     * @return a set of values
     * @throws NullPointerException if the key is null
     */
    Set<String> getValues(String key);

    /**
     * Check if thr set contains a given key mapped to a given value
     *
     * @param key   the key to look for
     * @param value the value to look for (case sensitive)
     * @return true if the set contains the KV pair
     * @throws NullPointerException if the key or value is null
     */
    boolean has(String key, String value);

    /**
     * Same as {@link #has(String, String)}, except ignores the case of the value.
     *
     * @param key   the key to look for
     * @param value the value to look for
     * @return true if the set contains the KV pair
     * @throws NullPointerException if the key or value is null
     */
    boolean hasIgnoreCase(String key, String value);

    /**
     * Check if the set is empty
     *
     * @return true if the set is empty
     */
    boolean isEmpty();

    /**
     * Gets the number of key-value context pairs in the set
     *
     * @return the size of the set
     */
    int size();

}
