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
import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

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
 * <p>Context keys are case-insensitive, and will be converted to
 * {@link String#toLowerCase() lowercase} by all implementations.
 * Values however are case-sensitive.</p>
 *
 * <p>Context keys and values may not be null.</p>
 *
 * <p>Two default ContextSet implementations are provided.
 * {@link MutableContextSet} allows the addition and removal of context keys
 * after construction, and {@link ImmutableContextSet} does not.</p>
 *
 * @since 2.13
 */
public interface ContextSet {

    /**
     * Creates an {@link ImmutableContextSet} from a context pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new ImmutableContextSet containing one context pair
     * @throws NullPointerException if key or value is null
     */
    @Nonnull
    static ImmutableContextSet singleton(@Nonnull String key, @Nonnull String value) {
        return ImmutableContextSet.singleton(key, value);
    }

    /**
     * Creates an {@link ImmutableContextSet} from two context pairs.
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
    static ImmutableContextSet of(@Nonnull String key1, @Nonnull String value1, @Nonnull String key2, @Nonnull String value2) {
        return ImmutableContextSet.of(key1, value1, key2, value2);
    }

    /**
     * Creates an {@link ImmutableContextSet} from an existing {@link Iterable} of {@link Map.Entry}s.
     *
     * @param iterable the iterable to copy from
     * @return a new ImmutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    @Nonnull
    static ImmutableContextSet fromEntries(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
        return ImmutableContextSet.fromEntries(iterable);
    }

    /**
     * Creates an {@link ImmutableContextSet} from an existing {@link Map}.
     *
     * @param map the map to copy from
     * @return a new ImmutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    @Nonnull
    static ImmutableContextSet fromMap(@Nonnull Map<String, String> map) {
        return ImmutableContextSet.fromMap(map);
    }

    /**
     * Creates an {@link ImmutableContextSet} from an existing {@link Multimap}.
     *
     * @param multimap the multimap to copy from
     * @return a new ImmutableContextSet representing the pairs in the multimap
     * @throws NullPointerException if the multimap is null
     * @since 2.16
     */
    @Nonnull
    static ImmutableContextSet fromMultimap(@Nonnull Multimap<String, String> multimap) {
        return ImmutableContextSet.fromMultimap(multimap);
    }

    /**
     * Creates an new {@link ImmutableContextSet} from an existing {@link Set}.
     *
     * <p>Only really useful for converting between mutable and immutable types.</p>
     *
     * @param contextSet the context set to copy from
     * @return a new ImmutableContextSet with the same content and the one provided
     * @throws NullPointerException if contextSet is null
     */
    @Nonnull
    static ImmutableContextSet fromSet(@Nonnull ContextSet contextSet) {
        return ImmutableContextSet.fromSet(contextSet);
    }

    /**
     * Returns an empty {@link ImmutableContextSet}.
     *
     * @return an empty ImmutableContextSet
     */
    @Nonnull
    static ImmutableContextSet empty() {
        return ImmutableContextSet.empty();
    }

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
    @Nonnull
    ImmutableContextSet makeImmutable();

    /**
     * Creates a mutable copy of this {@link ContextSet}.
     *
     * <p>A new copy is returned regardless of the
     * {@link #isImmutable() mutability} of this set.</p>
     *
     * @return a mutable ContextSet
     * @since 2.16
     */
    @Nonnull
    MutableContextSet mutableCopy();

    /**
     * Returns a {@link Set} of {@link Map.Entry}s representing the current
     * state of this {@link ContextSet}.
     *
     * <p>The returned set is immutable, and is a copy of the current set.
     * (will not update live)</p>
     *
     * @return an immutable set
     */
    @Nonnull
    Set<Map.Entry<String, String>> toSet();

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
     * <p>If you need a representation of the set in a Java collection instance,
     * use {@link #toSet()} or {@link #toMultimap()} followed by
     * {@link Multimap#asMap()}.</p>
     *
     * @return an immutable map
     * @deprecated because the resultant map may not contain all data in the ContextSet
     */
    @Nonnull
    @Deprecated
    Map<String, String> toMap();

    /**
     * Returns a {@link Multimap} representing the current state of this
     * {@link ContextSet}.
     *
     * <p>The returned multimap is immutable, and is a copy of the current set.
     * (will not update live)</p>
     *
     * @return a multimap
     * @since 2.16
     */
    @Nonnull
    Multimap<String, String> toMultimap();

    /**
     * Returns if the {@link ContextSet} contains at least one value for the
     * given key.
     *
     * @param key the key to check for
     * @return true if the set contains a value for the key
     * @throws NullPointerException if the key is null
     */
    boolean containsKey(@Nonnull String key);

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
    @Nonnull
    Set<String> getValues(@Nonnull String key);

    /**
     * Returns any value from this {@link ContextSet} matching the key, if present.
     *
     * <p>Note that context keys can be mapped to multiple values.
     * Use {@link #getValues(String)} to retrieve all associated values.</p>
     *
     * @param key the key to find values for
     * @return an optional containing any match
     * @since 3.1
     */
    @Nonnull
    default Optional<String> getAnyValue(@Nonnull String key) {
        return getValues(key).stream().findAny();
    }

    /**
     * Returns if the {@link ContextSet} contains a given context pairing.
     *
     * <p>This lookup is case-sensitive on the value.</p>
     *
     * @param key   the key to look for
     * @param value the value to look for (case sensitive)
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    boolean has(@Nonnull String key, @Nonnull String value);

    /**
     * Returns if the {@link ContextSet} contains a given context pairing,
     * ignoring the case of values.
     *
     * <p>This lookup is case-insensitive on the value.</p>
     *
     * @param key   the key to look for
     * @param value the value to look for
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    boolean hasIgnoreCase(@Nonnull String key, @Nonnull String value);

    /**
     * Returns if the {@link ContextSet} contains a given context pairing.
     *
     * <p>This lookup is case-sensitive on the value.</p>
     *
     * @param entry the entry to look for
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    default boolean has(@Nonnull Map.Entry<String, String> entry) {
        Preconditions.checkNotNull(entry, "entry");
        return has(entry.getKey(), entry.getValue());
    }

    /**
     * Returns if the {@link ContextSet} contains a given context pairing,
     * ignoring the case of values.
     *
     * <p>This lookup is case-insensitive on the value.</p>
     *
     * @param entry the entry to look for
     * @return true if the set contains the context pair
     * @throws NullPointerException if the key or value is null
     */
    default boolean hasIgnoreCase(@Nonnull Map.Entry<String, String> entry) {
        Preconditions.checkNotNull(entry, "entry");
        return hasIgnoreCase(entry.getKey(), entry.getValue());
    }

    /**
     * Returns if this {@link ContextSet} is fully "satisfied" by another set.
     *
     * <p>For a context set to "satisfy" another, it must itself contain all of
     * the context pairings in the other set.</p>
     *
     * <p>Mathematically, this method returns true if this set is a <b>subset</b> of the other.</p>
     *
     * <p>This check is case-sensitive. For a case-insensitive check,
     * use {@link #isSatisfiedBy(ContextSet, boolean)}.</p>
     *
     * @param other the other set to check
     * @return true if all entries in this set are also in the other set
     * @since 3.1
     */
    default boolean isSatisfiedBy(@Nonnull ContextSet other) {
        return isSatisfiedBy(other, true);
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
     * @param caseSensitive if the check should be case sensitive
     * @return true if all entries in this set are also in the other set
     * @since 3.4
     */
    default boolean isSatisfiedBy(@Nonnull ContextSet other, boolean caseSensitive) {
        if (this == other) {
            return true;
        }

        Preconditions.checkNotNull(other, "other");
        if (this.isEmpty()) {
            // this is empty, so is therefore always satisfied.
            return true;
        } else if (other.isEmpty()) {
            // this set isn't empty, but the other one is
            return false;
        } else if (this.size() > other.size()) {
            // this set has more unique entries than the other set, so there's no way this can be satisfied.
            return false;
        } else {
            // neither are empty, we need to compare the individual entries
            for (Map.Entry<String, String> context : toSet()) {
                if (caseSensitive) {
                    if (!other.has(context)) {
                        return false;
                    }
                } else {
                    if (!other.hasIgnoreCase(context)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

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
