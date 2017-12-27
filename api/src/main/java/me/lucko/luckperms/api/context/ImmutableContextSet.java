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

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An immutable implementation of {@link ContextSet}.
 *
 * @since 2.16
 */
@Immutable
public final class ImmutableContextSet extends AbstractContextSet implements ContextSet {
    private static final ImmutableContextSet EMPTY = new ImmutableContextSet(ImmutableSetMultimap.of());

    /**
     * Creates an {@link ImmutableContextSet.Builder}.
     *
     * @return a new ImmutableContextSet builder
     * @since 4.1
     */
    @Nonnull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an {@link ImmutableContextSet} from a context pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new ImmutableContextSet containing one context pair
     * @throws NullPointerException if key or value is null
     */
    @Nonnull
    public static ImmutableContextSet singleton(@Nonnull String key, @Nonnull String value) {
        return new ImmutableContextSet(ImmutableSetMultimap.of(sanitizeKey(key), sanitizeValue(value)));
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
    public static ImmutableContextSet of(@Nonnull String key1, @Nonnull String value1, @Nonnull String key2, @Nonnull String value2) {
        return new ImmutableContextSet(ImmutableSetMultimap.of(
                sanitizeKey(key1),
                sanitizeValue(value1),
                sanitizeKey(key2),
                sanitizeValue(value2)
        ));
    }

    /**
     * Creates an {@link ImmutableContextSet} from an existing {@link Iterable} of {@link Map.Entry}s.
     *
     * @param iterable the iterable to copy from
     * @return a new ImmutableContextSet representing the pairs in the iterable
     * @throws NullPointerException if the iterable is null
     */
    @Nonnull
    public static ImmutableContextSet fromEntries(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
        checkNotNull(iterable, "iterable");
        ImmutableContextSet.Builder builder = builder();
        for (Map.Entry<String, String> entry : iterable) {
            builder.add(entry);
        }
        return builder.build();
    }

    /**
     * Creates an {@link ImmutableContextSet} from an existing {@link Map}.
     *
     * @param map the map to copy from
     * @return a new ImmutableContextSet representing the pairs from the map
     * @throws NullPointerException if the map is null
     */
    @Nonnull
    public static ImmutableContextSet fromMap(@Nonnull Map<String, String> map) {
        return fromEntries(checkNotNull(map, "map").entrySet());
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
    public static ImmutableContextSet fromMultimap(@Nonnull Multimap<String, String> multimap) {
        return fromEntries(checkNotNull(multimap, "multimap").entries());
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
    public static ImmutableContextSet fromSet(@Nonnull ContextSet contextSet) {
        return checkNotNull(contextSet, "contextSet").makeImmutable();
    }

    /**
     * Returns an empty {@link ImmutableContextSet}.
     *
     * @return an empty ImmutableContextSet
     */
    @Nonnull
    public static ImmutableContextSet empty() {
        return EMPTY;
    }

    private final ImmutableSetMultimap<String, String> map;
    private final int hashCode;

    ImmutableContextSet(ImmutableSetMultimap<String, String> contexts) {
        this.map = contexts;
        this.hashCode = map.hashCode();
    }

    @Override
    protected Multimap<String, String> backing() {
        return map;
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Nonnull
    @Override
    @Deprecated // This set is already immutable!
    public ImmutableContextSet makeImmutable() {
        return this;
    }

    @Nonnull
    @Override
    public MutableContextSet mutableCopy() {
        return MutableContextSet.fromSet(this);
    }

    @Nonnull
    @Override
    public Set<Map.Entry<String, String>> toSet() {
        return map.entries();
    }

    @Nonnull
    @Override
    public Multimap<String, String> toMultimap() {
        return map;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "ImmutableContextSet(contexts=" + this.map + ")";
    }

    /**
     * A builder for {@link ImmutableContextSet}.
     *
     * @since 4.1
     */
    public static final class Builder {
        private ImmutableSetMultimap.Builder<String, String> builder;

        private Builder() {

        }

        private synchronized ImmutableSetMultimap.Builder<String, String> builder() {
            if (builder == null) {
                builder = ImmutableSetMultimap.builder();
            }
            return builder;
        }

        private void put(String key, String value) {
            builder().put(key, value);
        }

        /**
         * Adds a context to the set.
         *
         * @param key   the key to add
         * @param value the value to add
         * @return the builder
         * @throws NullPointerException if the key or value is null
         * @see MutableContextSet#add(String, String)
         */
        @Nonnull
        public Builder add(@Nonnull String key, @Nonnull String value) {
            put(sanitizeKey(key), sanitizeValue(value));
            return this;
        }

        /**
         * Adds a context to the set.
         *
         * @param entry the entry to add
         * @return the builder
         * @throws NullPointerException if the entry is null
         * @see MutableContextSet#add(Map.Entry)
         */
        @Nonnull
        public Builder add(@Nonnull Map.Entry<String, String> entry) {
            checkNotNull(entry, "entry");
            add(entry.getKey(), entry.getValue());
            return this;
        }

        /**
         * Adds the contexts contained in the given {@link Iterable} to the set.
         *
         * @param iterable an iterable of key value context pairs
         * @return the builder
         * @throws NullPointerException if iterable is null
         * @see MutableContextSet#addAll(Iterable)
         */
        @Nonnull
        public Builder addAll(@Nonnull Iterable<? extends Map.Entry<String, String>> iterable) {
            for (Map.Entry<String, String> e : checkNotNull(iterable, "iterable")) {
                add(e);
            }
            return this;
        }

        /**
         * Adds the contexts contained in the given {@link Map} to the set.
         *
         * @param map the map to add from
         * @return the builder
         * @throws NullPointerException if the map is null
         * @see MutableContextSet#addAll(Map)
         */
        @Nonnull
        public Builder addAll(@Nonnull Map<String, String> map) {
            addAll(checkNotNull(map, "map").entrySet());
            return this;
        }

        /**
         * Adds the contexts contained in the given {@link Multimap} to the set.
         *
         * @param multimap the multimap to add from
         * @return the builder
         * @throws NullPointerException if the map is null
         * @since 3.4
         * @see MutableContextSet#addAll(Multimap)
         */
        @Nonnull
        public Builder addAll(@Nonnull Multimap<String, String> multimap) {
            addAll(checkNotNull(multimap, "multimap").entries());
            return this;
        }

        /**
         * Adds of of the contexts in another {@link ContextSet} to the set.
         *
         * @param contextSet the set to add from
         * @return the builder
         * @throws NullPointerException if the contextSet is null
         * @see MutableContextSet#addAll(ContextSet)
         */
        @Nonnull
        public Builder addAll(@Nonnull ContextSet contextSet) {
            checkNotNull(contextSet, "contextSet");
            if (contextSet instanceof AbstractContextSet) {
                AbstractContextSet other = ((AbstractContextSet) contextSet);
                if (!other.isEmpty()) {
                    builder().putAll(other.backing());
                }
            } else {
                addAll(contextSet.toMultimap());
            }
            return this;
        }

        /**
         * Creates a {@link ImmutableContextSet} from the values previously
         * added to the builder.
         *
         * @return an {@link ImmutableContextSet} from the builder
         */
        @Nonnull
        public ImmutableContextSet build() {
            if (builder == null) {
                return empty();
            } else {
                return new ImmutableContextSet(builder.build());
            }
        }
    }
}
