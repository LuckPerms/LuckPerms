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

package me.lucko.luckperms.api;

/**
 * Represents a permission setting.
 *
 * <p>Consider a value of {@link #TRUE} to be a positive setting, {@link #FALSE} to be a "negated" setting,
 * and a value of {@link #UNDEFINED} to be a non-existent value.</p>
 */
public enum Tristate {

    /**
     * A value indicating a holder has a permission set.
     */
    TRUE(true),

    /**
     * A value indicating a holder has a negated value for a permission.
     */
    FALSE(false),

    /**
     * A value indicating a holder doesn't have a value for a permission set.
     */
    UNDEFINED(false);

    /**
     * Converts from {@link Boolean} a boolean
     *
     * @param b the boolean
     * @return {@link #TRUE} or {@link #FALSE}, depending on the value of the boolean.
     */
    public static Tristate fromBoolean(boolean b) {
        return b ? TRUE : FALSE;
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
        return booleanValue;
    }
}
