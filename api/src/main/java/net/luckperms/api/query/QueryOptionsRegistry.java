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

package net.luckperms.api.query;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A registry providing useful {@link QueryOptions} instances.
 *
 * @since 5.1
 */
public interface QueryOptionsRegistry {

    /**
     * Gets the default {@link QueryMode#CONTEXTUAL contextual}
     * query options.
     *
     * <p>Prefer using the {@link QueryOptions#defaultContextualOptions()} accessor.</p>
     *
     * @return the default contextual query options
     * @see QueryOptions#defaultContextualOptions()
     */
    @NonNull QueryOptions defaultContextualOptions();

    /**
     * Gets the default {@link QueryMode#NON_CONTEXTUAL non contextual}
     * query options.
     *
     * <p>Prefer using the {@link QueryOptions#nonContextual()} accessor.</p>
     *
     * @return the default non contextual query options
     * @see QueryOptions#nonContextual()
     */
    @NonNull QueryOptions defaultNonContextualOptions();

}
