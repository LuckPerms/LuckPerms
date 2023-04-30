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

package net.luckperms.api.context;

import net.luckperms.api.LuckPermsProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

/**
 * A mutable implementation of {@link ContextSet}.
 */
public interface MutableContextSet extends ContextSet {

    /**
     * Creates a new empty MutableContextSet.
     *
     * @return a new MutableContextSet
     */
    static @NonNull MutableContextSet create() {
        return LuckPermsProvider.get().getContextManager().getContextSetFactory().mutable();
    }

    /**
     * Creates a {@link MutableContextSet} from a context pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new MutableContextSet containing one context pair
     * @throws NullPointerException if key or value is null
     */
    static @NonNull MutableContextSet of(@NonNull String key, @NonNull String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        MutableContextSet set = create();
        set.add(key, value);
        return set;
    }

    /**
     * Adds a context to this set.
     *
     * @param key   the key to add
     * @param value the value to add
     * @throws NullPointerException if the key or value is null
     */
    void add(@NonNull String key, @NonNull String value);

    /**
     * Adds a context to this set.
     *
     * @param entry the entry to add
     * @throws NullPointerException if the entry is null
     */
    default void add(@NonNull Context entry) {
        Objects.requireNonNull(entry, "entry");
        add(entry.getKey(), entry.getValue());
    }

    /**
     * Adds the contexts contained in the given {@link Iterable} to this set.
     *
     * @param iterable an iterable of key value context pairs
     * @throws NullPointerException if iterable is null
     */
    default void addAll(@NonNull Iterable<Context> iterable) {
        for (Context e : Objects.requireNonNull(iterable, "iterable")) {
            add(e);
        }
    }

    /**
     * Adds all the contexts in another {@link ContextSet} to this set.
     *
     * @param contextSet the set to add from
     * @throws NullPointerException if the contextSet is null
     */
    void addAll(@NonNull ContextSet contextSet);

    /**
     * Removes a context from this set.
     *
     * @param key   the key to remove
     * @param value the value to remove
     * @throws NullPointerException if the key or value is null
     */
    void remove(@NonNull String key, @NonNull String value);

    /**
     * Removes all contexts from this set with the given key.
     *
     * @param key the key to remove
     * @throws NullPointerException if the key is null
     */
    void removeAll(@NonNull String key);

    /**
     * Removes all contexts from the set.
     */
    void clear();

}
