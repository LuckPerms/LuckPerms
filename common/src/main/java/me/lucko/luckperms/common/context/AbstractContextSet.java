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

package me.lucko.luckperms.common.context;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Objects;

public abstract class AbstractContextSet implements ContextSet {

    @Override
    public boolean isSatisfiedBy(@NonNull ContextSet other, @NonNull ContextSatisfyMode mode) {
        if (this == other) {
            return true;
        }

        Objects.requireNonNull(other, "other");
        Objects.requireNonNull(mode, "mode");

        // this is empty, it is always satisfied.
        if (this.isEmpty()) {
            return true;
        }

        // if this set isn't empty, but the other one is, then it can't be satisfied by it.
        if (other.isEmpty()) {
            return false;
        }

        // if mode is ALL_VALUES & this set has more entries than the other one, then it can't be satisfied by it.
        if (mode == ContextSatisfyMode.ALL_VALUES_PER_KEY && this.size() > other.size()) {
            return false;
        }

        // return true if 'other' contains all of 'this', according to the mode.
        return otherContainsAll(other, mode);
    }

    protected abstract boolean otherContainsAll(ContextSet other, ContextSatisfyMode mode);

    static String sanitizeKey(String key) {
        Objects.requireNonNull(key, "key is null");
        if (!Context.isValidKey(key)) {
            throw new IllegalArgumentException("key is (effectively) empty");
        }
        return key.toLowerCase(Locale.ROOT);
    }

    static String sanitizeValue(String value) {
        Objects.requireNonNull(value, "value is null");
        if (!Context.isValidValue(value)) {
            throw new IllegalArgumentException("value is (effectively) empty");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public static boolean isGlobalServerWorldEntry(String key, String value) {
        return (key.equals(DefaultContextKeys.SERVER_KEY) || key.equals(DefaultContextKeys.WORLD_KEY)) && value.equals("global");
    }

}
