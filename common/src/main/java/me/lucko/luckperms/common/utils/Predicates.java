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

package me.lucko.luckperms.common.utils;

import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A collection of predicate utilities used mostly in command classes
 */
@UtilityClass
public class Predicates {
    private static final Predicate FALSE = o -> false;
    private static final Predicate TRUE = o -> true;

    public static <T> Predicate<T> alwaysFalse() {
        //noinspection unchecked
        return FALSE;
    }

    public static <T> Predicate<T> alwaysTrue() {
        //noinspection unchecked
        return TRUE;
    }

    public static Predicate<Integer> notInRange(Integer start, Integer end) {
        return inverse(inRange(start, end));
    }

    public static Predicate<Integer> inRange(Integer start, Integer end) {
        return isOneOf(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toSet()));
    }

    public static <T> Predicate<T> notOneOf(Set<T> ts) {
        return inverse(isOneOf(ts));
    }

    public static <T> Predicate<T> isOneOf(Set<T> ta) {
        return t -> {
            for (T i : ta) {
                if (i.equals(t)) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <T> Predicate<T> not(T t) {
        return inverse(is(t));
    }

    public static <T> Predicate<T> is(T t) {
        return t::equals;
    }

    public static <T> Predicate<T> inverse(Predicate<T> t) {
        return t2 -> !t.test(t2);
    }
}
