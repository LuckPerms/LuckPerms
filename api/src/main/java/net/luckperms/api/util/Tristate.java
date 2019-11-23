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

package net.luckperms.api.util;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents three different states of a setting.
 *
 * <p>Possible values:</p>
 * <p></p>
 * <ul>
 *     <li>{@link #TRUE} - a positive setting</li>
 *     <li>{@link #FALSE} - a negative (negated) setting</li>
 *     <li>{@link #UNDEFINED} - a non-existent setting</li>
 * </ul>
 */
public enum Tristate {

    /**
     * A value indicating a positive setting
     */
    TRUE(true),

    /**
     * A value indicating a negative (negated) setting
     */
    FALSE(false),

    /**
     * A value indicating a non-existent setting
     */
    UNDEFINED(false);

    /**
     * Returns a {@link Tristate} from a boolean
     *
     * @param val the boolean value
     * @return {@link #TRUE} or {@link #FALSE}, if the value is <code>true</code> or <code>false</code>, respectively.
     */
    public static @NonNull Tristate of(boolean val) {
        return val ? TRUE : FALSE;
    }

    /**
     * Returns a {@link Tristate} from a nullable boolean.
     *
     * <p>Unlike {@link #of(boolean)}, this method returns {@link #UNDEFINED}
     * if the value is null.</p>
     *
     * @param val the boolean value
     * @return {@link #UNDEFINED}, {@link #TRUE} or {@link #FALSE}, if the value
     *         is <code>null</code>, <code>true</code> or <code>false</code>, respectively.
     */
    public static @NonNull Tristate of(Boolean val) {
        return val == null ? UNDEFINED : val ? TRUE : FALSE;
    }

    private final boolean booleanValue;

    Tristate(boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    /**
     * Returns the value of the Tristate as a boolean.
     *
     * <p>A value of {@link #UNDEFINED} converts to false.</p>
     *
     * @return a boolean representation of the Tristate.
     */
    public boolean asBoolean() {
        return this.booleanValue;
    }
}
