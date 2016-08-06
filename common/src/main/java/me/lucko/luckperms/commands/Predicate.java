/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.commands;

import lombok.experimental.UtilityClass;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class Predicate {
    public static <T> java.util.function.Predicate<T> alwaysFalse() {
        return t -> false;
    }

    public static <T> java.util.function.Predicate<T> alwaysTrue() {
        return t -> true;
    }

    public static java.util.function.Predicate<Integer> notInRange(Integer start, Integer end) {
        return inverse(inRange(start, end));
    }

    public static java.util.function.Predicate<Integer> inRange(Integer start, Integer end) {
        return isOneOf(IntStream.rangeClosed(start, end).boxed().collect(Collectors.toSet()));
    }

    public static <T> java.util.function.Predicate<T> notOneOf(Set<T> ts) {
        return inverse(isOneOf(ts));
    }

    public static <T> java.util.function.Predicate<T> isOneOf(Set<T> ta) {
        return t -> {
            for (T i : ta) {
                if (i == t) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <T> java.util.function.Predicate<T> not(T t) {
        return inverse(is(t));
    }

    public static <T> java.util.function.Predicate<T> is(T t) {
        return t2 -> t == t2;
    }

    public static <T> java.util.function.Predicate<T> inverse(java.util.function.Predicate<T> t) {
        return t2 -> !t.test(t2);
    }
}
