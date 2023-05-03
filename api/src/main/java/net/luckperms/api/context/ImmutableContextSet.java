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
 * An immutable implementation of {@link ContextSet}.
 */
public interface ImmutableContextSet extends ContextSet {

    /**
     * Creates an {@link ImmutableContextSet.Builder}.
     *
     * @return a new ImmutableContextSet builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getContextManager().getContextSetFactory().immutableBuilder();
    }

    /**
     * Returns an empty {@link ImmutableContextSet}.
     *
     * @return an empty ImmutableContextSet
     */
    static @NonNull ImmutableContextSet empty() {
        return LuckPermsProvider.get().getContextManager().getContextSetFactory().immutableEmpty();
    }

    /**
     * Creates an {@link ImmutableContextSet} from a context pair.
     *
     * @param key   the key
     * @param value the value
     * @return a new ImmutableContextSet containing one context pair
     * @throws NullPointerException if key or value is null
     */
    static @NonNull ImmutableContextSet of(@NonNull String key, @NonNull String value) {
        return LuckPermsProvider.get().getContextManager().getContextSetFactory().immutableOf(key, value);
    }

    /**
     * @deprecated This context set is already immutable!
     */
    @Override
    @Deprecated
    @NonNull ImmutableContextSet immutableCopy();

    /**
     * A builder for {@link ImmutableContextSet}.
     */
    interface Builder {

        /**
         * Adds a context to the set.
         *
         * @param key   the key to add
         * @param value the value to add
         * @return the builder
         * @throws NullPointerException if the key or value is null
         * @see MutableContextSet#add(String, String)
         */
        @NonNull Builder add(@NonNull String key, @NonNull String value);

        /**
         * Adds a context to the set.
         *
         * @param entry the entry to add
         * @return the builder
         * @throws NullPointerException if the entry is null
         * @see MutableContextSet#add(Context)
         */
        default @NonNull Builder add(@NonNull Context entry) {
            Objects.requireNonNull(entry, "entry");
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
        default @NonNull Builder addAll(@NonNull Iterable<Context> iterable) {
            for (Context e : Objects.requireNonNull(iterable, "iterable")) {
                add(e);
            }
            return this;
        }

        /**
         * Adds all the contexts in another {@link ContextSet} to the set.
         *
         * @param contextSet the set to add from
         * @return the builder
         * @throws NullPointerException if the contextSet is null
         * @see MutableContextSet#addAll(ContextSet)
         */
        @NonNull Builder addAll(@NonNull ContextSet contextSet);

        /**
         * Creates a {@link ImmutableContextSet} from the values previously
         * added to the builder.
         *
         * @return an {@link ImmutableContextSet} from the builder
         */
        @NonNull ImmutableContextSet build();
    }
}
