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

import com.google.common.collect.Maps;

import java.util.HashSet;
import java.util.Map;

/**
 * Holds contexts
 * All contained contexts are immutable, but contexts can be added or removed from the set.
 *
 * @since 2.13
 */
public class MutableContextSet extends ContextSet {

    /**
     * Make a singleton MutableContextSet from a context pair
     * @param key the key
     * @param value the value
     * @return a new MutableContextSet containing one KV pair
     * @throws NullPointerException if key or value is null
     */
    public static MutableContextSet singleton(String key, String value) {
        MutableContextSet set = new MutableContextSet();
        set.add(key, value);
        return set;
    }

    /**
     * Creates a MutableContextSet from an existing map
     * @param map the map to copy from
     * @return a new MutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    public static MutableContextSet fromMap(Map<String, String> map) {
        MutableContextSet set = new MutableContextSet();
        set.addAll(map);
        return set;
    }

    /**
     * Creates a MutableContextSet from an existing iterable of Map Entries
     * @param iterable the iterable to copy from
     * @return a new MutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    public static MutableContextSet fromEntries(Iterable<Map.Entry<String, String>> iterable) {
        MutableContextSet set = new MutableContextSet();
        set.addAll(iterable);
        return set;
    }

    /**
     * Creates a new MutableContextSet from an existing set.
     * Only really useful for converting between mutable and immutable types.
     * @param contextSet the context set to copy from
     * @return a new MutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    public static MutableContextSet fromSet(ContextSet contextSet) {
        MutableContextSet set = new MutableContextSet();
        set.addAll(contextSet.toSet());
        return set;
    }

    /**
     * Creates a new empty MutableContextSet.
     * @return a new MutableContextSet
     */
    public static MutableContextSet empty() {
        return new MutableContextSet();
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public ContextSet makeImmutable() {
        return immutableCopy();
    }

    /**
     * Returns an immutable copy of this set.
     * @return an immutable copy of this set
     */
    public ContextSet immutableCopy() {
        synchronized (contexts) {
            return new ContextSet(new HashSet<>(contexts));
        }
    }

    /**
     * Adds a new key value pair to the set
     * @param key the key to add
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

        synchronized (contexts) {
            contexts.add(Maps.immutableEntry(key, value));
        }
    }

    /**
     * Adds a new key value pair to the set
     * @param entry the entry to add
     * @throws NullPointerException if the entry is null
     */
    public void add(Map.Entry<String, String> entry) {
        if (entry == null) {
            throw new NullPointerException("context");
        }

        synchronized (contexts) {
            contexts.add(Maps.immutableEntry(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Adds an iterable containing contexts to the set
     * @param iterable an iterable of key value context pairs
     * @throws NullPointerException if iterable is null
     */
    public void addAll(Iterable<Map.Entry<String, String>> iterable) {
        if (iterable == null) {
            throw new NullPointerException("contexts");
        }

        synchronized (this.contexts) {
            for (Map.Entry<String, String> e : iterable) {
                this.contexts.add(Maps.immutableEntry(e.getKey(), e.getValue()));
            }
        }
    }

    /**
     * Adds the entry set of a map to the set
     * @param map the map to add from
     * @throws NullPointerException if the map is null
     */
    public void addAll(Map<String, String> map) {
        if (map == null) {
            throw new NullPointerException("contexts");
        }
        addAll(map.entrySet());
    }

    /**
     * Adds of of the values in another ContextSet to this set
     * @param contextSet the set to add from
     * @throws NullPointerException if the contextSet is null
     */
    public void addAll(ContextSet contextSet) {
        if (contextSet == null) {
            throw new NullPointerException("contextSet");
        }

        synchronized (this.contexts) {
            this.contexts.addAll(contextSet.toSet());
        }
    }

    /**
     * Remove a key value pair from this set
     * @param key the key to remove
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

        synchronized (contexts) {
            contexts.removeIf(e -> e.getKey().equalsIgnoreCase(key) && e.getValue().equals(value));
        }
    }

    /**
     * Same as {@link #remove(String, String)}, except ignores the case of the value
     * @param key the key to remove
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

        synchronized (contexts) {
            contexts.removeIf(e -> e.getKey().equalsIgnoreCase(key) && e.getValue().equalsIgnoreCase(value));
        }
    }

    /**
     * Removes all pairs with the given key
     * @param key the key to remove
     * @throws NullPointerException if the key is null
     */
    public void removeAll(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        synchronized (contexts) {
            contexts.removeIf(e -> e.getKey().equalsIgnoreCase(key));
        }
    }

    /**
     * Clears the set
     */
    public void clear() {
        synchronized (contexts) {
            contexts.clear();
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
