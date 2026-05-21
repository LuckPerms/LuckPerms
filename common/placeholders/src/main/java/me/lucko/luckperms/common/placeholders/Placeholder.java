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

package me.lucko.luckperms.common.placeholders;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * A placeholder definition.
 */
public interface Placeholder {

    /**
     * Get the id of the placeholder.
     *
     * @return the id
     */
    @NonNull String id();

    /**
     * A placeholder function that uses the standard {@link PlaceholderContext}
     */
    interface BasicPlaceholderFunction {
        /**
         * Resolve the value of this placeholder with some given context.
         *
         * @param ctx the context
         * @return the resolved value
         */
        @NonNull String resolve(@NonNull PlaceholderContext ctx);
    }

    /**
     * A placeholder function that uses the extended {@link PlaceholderContext.WithArgument}
     */
    interface UsingArgumentPlaceholderFunction {
        /**
         * Resolve the value of this placeholder with some given context.
         *
         * @param ctx the context
         * @return the resolved value
         */
        @NonNull String resolve(PlaceholderContext.@NonNull WithArgument ctx);
    }

    /** A basic placeholder */
    interface Basic extends Placeholder, BasicPlaceholderFunction {}

    /** A placeholder that uses an argument */
    interface UsingArgument extends Placeholder, UsingArgumentPlaceholderFunction {}

    /**
     * Create a standard placeholder using the given resolver function.
     *
     * @param id the id
     * @param resolver the resolver function
     * @return the placeholder
     */
    static Basic basic(@NonNull String id, Placeholder.@NonNull BasicPlaceholderFunction resolver) {
        return new Basic() {
            @Override
            public @NonNull String id() {
                return id;
            }

            @Override
            public @NotNull String resolve(@NotNull PlaceholderContext ctx) {
                return resolver.resolve(ctx);
            }
        };
    }

    /**
     * Create a dynamic placeholder using the given resolver function.
     *
     * @param id the id
     * @param resolver the resolver function
     * @return the placeholder
     */
    static UsingArgument usingArgument(@NonNull String id, Placeholder.@NonNull UsingArgumentPlaceholderFunction resolver) {
        return new UsingArgument() {
            @Override
            public @NonNull String id() {
                return id;
            }

            @Override
            public @NotNull String resolve(PlaceholderContext.@NotNull WithArgument ctx) {
                return resolver.resolve(ctx);
            }
        };
    }

}
