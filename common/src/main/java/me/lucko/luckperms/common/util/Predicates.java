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

package me.lucko.luckperms.common.util;

import com.google.common.collect.Range;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.function.Predicate;

/**
 * A collection of predicate utilities used mostly in command classes
 */
public final class Predicates {
    private Predicates() {}

    @SuppressWarnings("rawtypes")
    private static final Predicate FALSE = new Predicate() {
        @Override public boolean test(Object o) { return false; }
        @Override
        public @NonNull Predicate and(@NonNull Predicate other) { return this; }
        @Override
        public @NonNull Predicate or(@NonNull Predicate other) { return other; }
        @Override
        public @NonNull Predicate negate() { return TRUE; }
    };
    @SuppressWarnings("rawtypes")
    private static final Predicate TRUE = new Predicate() {
        @Override public boolean test(Object o) { return true; }
        @Override
        public @NonNull Predicate and(@NonNull Predicate other) { return other; }
        @Override
        public @NonNull Predicate or(@NonNull Predicate other) { return this; }
        @Override
        public @NonNull Predicate negate() { return FALSE; }
    };

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return FALSE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return TRUE;
    }

    public static Predicate<Integer> notInRange(int start, int end) {
        Range<Integer> range = Range.closed(start, end);
        return value -> !range.contains(value);
    }

    public static Predicate<Integer> inRange(int start, int end) {
        Range<Integer> range = Range.closed(start, end);
        return range::contains;
    }

    public static <T> Predicate<T> not(T t) {
        return obj -> !t.equals(obj);
    }

    public static <T> Predicate<T> is(T t) {
        return t::equals;
    }

    public static Predicate<String> startsWithIgnoreCase(String prefix) {
        return string -> {
            if (string.length() < prefix.length()) {
                return false;
            }
            return string.regionMatches(true, 0, prefix, 0, prefix.length());
        };
    }

    public static Predicate<String> containsIgnoreCase(String substring) {
        return string -> {
            if (string.length() < substring.length()) {
                return false;
            }
            return string.toLowerCase(Locale.ROOT).contains(substring.toLowerCase(Locale.ROOT));
        };
    }

}
