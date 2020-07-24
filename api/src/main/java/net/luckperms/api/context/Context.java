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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an individual context pair.
 *
 * <p>Context keys and values may not be null or empty. A key/value will be
 * deemed empty if it's length is zero, or if it consists of only space
 * characters.</p>
 *
 * @see ContextSet
 */
public interface Context {

    /**
     * Tests whether {@code key} is valid.
     *
     * <p>Context keys and values may not be null or empty. A key/value will be
     * deemed empty if it's length is zero, or if it consists of only space
     * characters.</p>
     *
     * <p>An exception is thrown when an invalid key is added to a {@link ContextSet}.</p>
     *
     * @param key the key to test
     * @return true if valid, false otherwise.
     * @since 5.1
     */
    static boolean isValidKey(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        // look for a non-whitespace character
        for (int i = 0, n = key.length(); i < n; i++) {
            if (key.charAt(i) != ' ') {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether {@code value} is valid.
     *
     * <p>Context keys and values may not be null or empty. A key/value will be
     * deemed empty if it's length is zero, or if it consists of only space
     * characters.</p>
     *
     * <p>An exception is thrown when an invalid value is added to a {@link ContextSet}.</p>
     *
     * @param value the value to test
     * @return true if valid, false otherwise.
     * @since 5.1
     */
    static boolean isValidValue(@Nullable String value) {
        return isValidKey(value); // the same for now...
    }

    /**
     * Gets the context key.
     *
     * @return the key
     */
    @NonNull String getKey();

    /**
     * Gets the context value
     *
     * @return the value
     */
    @NonNull String getValue();

}
