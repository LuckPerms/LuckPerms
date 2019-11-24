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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A set of contexts.
 *
 * <p>Context in the most basic sense simply means the circumstances where
 * something will apply.</p>
 *
 * <p>A single "context" consists of a key and a value, both strings. The key
 * represents the type of context, and the value represents the setting of the
 * context key.</p>
 *
 * <p>Contexts can be combined with each other to form so called
 * "context sets" - simply a collection of context pairs.</p>
 *
 * <p>Context keys and values are case-insensitive, and will be converted to
 * {@link String#toLowerCase() lowercase} by all implementations.</p>
 *
 * <p>Context keys and values may not be null or empty. A key/value will be
 * deemed empty if it's length is zero, or if it consists of only space
 * characters.</p>
 *
 * <p>Two default ContextSet implementations are provided.
 * {@link MutableContextSet} allows the addition and removal of context keys
 * after construction, and {@link ImmutableContextSet} does not.</p>
 */
public interface ContextSet extends Iterable<Context> {

    /**
     * Gets if this {@link ContextSet} is immutable.
     *
     * <p>The state of immutable instances will never change.</p>
     *
     * @return true if the set is immutable
     */
    boolean isImmutable();

    /**
     * Returns an immutable representation of this {@link ContextSet}.
     *
     * <p>If the set is already immutable, the same object will be returned.
     * If the set is mutable, an immutable copy will be made.</p>
     *
     * @return an immutable representation of this set
     */
    @NonNull ImmutableContextSet immutableCopy();

    /**
     * Creates a mutable copy of this {@link ContextSet}.
     *
     * <p>A new copy is returned regardless of the
     * {@link #isImmutable() mutability} of this set.</p>
     *
     * @return a mutable ContextSet
     */
    @NonNull MutableContextSet mutableCopy();

    /**
     * Returns a {@link Set} of {@link Context}s representing the current
     * state of this {@link ContextSet}.
     *
     * <p>The returned set is immutable, and is a copy of the current set.
     * (will not update live)</p>
     *
     * @return an immutable set
     */
    @NonNull Set<Context> toSet();

    /**
     * Returns a {@link Map} representing the current state of this
     * {@link ContextSet}.
     *
     * <p>The returned set is immutable, and is a copy of the current set.
     * (will not update live)</p>
     *
     * @return a map
     */
    @NonNull Map<String, Set<String>> toMap();

    /**
     * Returns a {@link Map} <b>loosely</b> representing the current state of
     * this {@link ContextSet}.
     *
     * <p>The returned map is immutable, and is a copy of the current set.
     * (will not update live)</p>
     *
     * <p>As a single context key can be mapped to multiple values, this method
     * may not be a true representation of the set.</p>
     *
     * @return an immutable map
     * @deprecated because the resultant map may not contain all data in the ContextSet
     */
    @Deprecated
    @NonNull Map<String, String> toFlattenedMap();

    /**
     * Returns an {@link Iterator} over each of the context pairs in this set.
     *
     * <p>The returned iterator represents the state of the set at the time of creation. It is not
     * updated as the set changes.</p>
     *
     * <p>The iterator does not support {@link Iterator#remove()} calls.</p>
     *
     * @return an iterator
     */
    @Override
    @NonNull Iterator<Context> iterator();

    /**
     * Returns if the {@link ContextSet} contains at least one value for the
     * given key.
     *
     * @param key the key to check for
     * @return true if the set contains a value for the key
     * @throws NullPointerException if the key is null
     */
    boolean containsKey(@NonNull String key);

    /**
     * Returns a {@link Set} of the values mapped to the given key.
     *
     * <p>The returned set is immutable, and only represents the current state
     * of the {@link ContextSet}. (will not update live)</p>
     *
     * @param key the key to get values for
     * @return a set of values
     * @throws NullPointerException if the key is null
     */
    @NonNull Set<String> getValues(@NonNull String key);

    /**
     * Returns any value from this {@link ContextSet} matching the key, if present.
     *
     * <p>Note that context keys can be mapped to multiple values.
     * Use {@link #getValues(String)} to retrieve all associated values.</p>
     *
     * @param key the key to find values for
     * @return an optional containing any match
     */
    default @NonNull Optional<String> getAnyValue(@NonNull String key) {
        return getValues(key).stream().findAny();
    }

    /**
     * Returns if the {@link ContextSet} contains a given context pairing.
     *
     * @param key   the key to look for
     * @param value the value to look for
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    boolean contains(@NonNull String key, @NonNull String value);

    /**
     * Returns if the {@link ContextSet} contains a given context pairing.
     *
     * @param entry the entry to look for
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    default boolean contains(@NonNull Context entry) {
        Objects.requireNonNull(entry, "entry");
        return contains(entry.getKey(), entry.getValue());
    }

    /**
     * Returns if this {@link ContextSet} is fully "satisfied" by another set.
     *
     * <p>For a context set to "satisfy" another, it must itself contain all of
     * the context pairings in the other set.</p>
     *
     * <p>Mathematically, this method returns true if this set is a <b>subset</b> of the other.</p>
     *
     * @param other the other set to check
     * @return true if all entries in this set are also in the other set
     */
    boolean isSatisfiedBy(@NonNull ContextSet other);

    /**
     * Returns if the {@link ContextSet} is empty.
     *
     * @return true if the set is empty
     */
    boolean isEmpty();

    /**
     * Gets the number of context pairs in the {@link ContextSet}.
     *
     * @return the size of the set
     */
    int size();

}
