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
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * A factory for creating {@link ContextSet}s.
 *
 * <p>Prefer using the static methods on {@link ImmutableContextSet} or {@link MutableContextSet}
 * instead of this interface.</p>
 */
@Internal
public interface ContextSetFactory {

    /**
     * Prefer using {@link ImmutableContextSet#builder()}.
     *
     * @return a immutable context set builder
     * @see ImmutableContextSet#builder()
     */
    ImmutableContextSet.@NonNull Builder immutableBuilder();

    /**
     * Prefer using {@link ImmutableContextSet#of(String, String)}.
     *
     * @param key the key
     * @param value the value
     * @return an immutable context set
     * @see ImmutableContextSet#of(String, String)
     */
    @NonNull ImmutableContextSet immutableOf(@NonNull String key, @NonNull String value);

    /**
     * Prefer using {@link ImmutableContextSet#empty()}.
     *
     * @return an empty immutable context set
     * @see ImmutableContextSet#empty()
     */
    @NonNull ImmutableContextSet immutableEmpty();

    /**
     * Prefer using {@link MutableContextSet#create()}.
     *
     * @return a new mutable context set
     * @see MutableContextSet#create()
     */
    @NonNull MutableContextSet mutable();

}
