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

package me.lucko.luckperms.common.node.utils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility to expand shorthand nodes
 */
public enum ShorthandParser {

    /**
     * Expands "1-4" to ["1", "2", "3", "4"]
     */
    NUMERIC_RANGE {
        @Override
        public Iterator<String> extract(String input) throws NumberFormatException {
            int index = input.indexOf(RANGE_SEPARATOR);
            if (index == -1 || index == 0 || index == input.length() - 1) {
                return null;
            }

            return new RangeIterator(Integer.parseInt(input.substring(0, index)), Integer.parseInt(input.substring(index + 1))) {
                @Override
                protected String toString(int i) {
                    return Integer.toString(i);
                }
            };
        }
    },

    /**
     * Expands "a-d" to ["a", "b", "c", "d"]
     */
    CHARACTER_RANGE {
        @Override
        public Iterator<String> extract(String input) {
            if (input.length() != 3 || input.charAt(1) != RANGE_SEPARATOR) {
                return null;
            }

            return new RangeIterator(input.charAt(0), input.charAt(2)) {
                @Override
                protected String toString(int i) {
                    return Character.toString((char) i);
                }
            };
        }
    },

    /**
     * Expands "aa,bb|cc" to ["aa", "bb", "cc"]
     */
    LIST {
        private final Splitter splitter = Splitter.on(CharMatcher.anyOf(new String(new char[]{LIST_SEPARATOR, LIST_SEPARATOR_2}))).omitEmptyStrings();

        @Override
        public Iterator<String> extract(String input) {
            if (indexOfEither(input, LIST_SEPARATOR, LIST_SEPARATOR_2) == -1) {
                return Iterators.singletonIterator(input);
            }
            return this.splitter.split(input).iterator();
        }
    };

    /**
     * Expands the given input in shorthand notation to a number of strings.
     *
     * @param input the input shorthand
     * @return an iterator of the resultant strings, or optionally null if the input was invalid
     * @throws IllegalArgumentException if the input was invalid
     */
    abstract Iterator<String> extract(String input) throws IllegalArgumentException;

    /** Character used to open a group */
    private static final char OPEN_GROUP = '{';
    /** Character used to close a group */
    private static final char CLOSE_GROUP = '}';
    /** Another character used to open a group */
    private static final char OPEN_GROUP_2 = '(';
    /** Another character used to close a group */
    private static final char CLOSE_GROUP_2 = ')';
    /** Character used to separate items in a list */
    private static final char LIST_SEPARATOR = ',';
    /** Another character used to separate items in a list */
    private static final char LIST_SEPARATOR_2 = '|';
    /** Character used to indicate a range between two values */
    private static final char RANGE_SEPARATOR = '-';

    /** The parsers */
    private static final ShorthandParser[] PARSERS = values();

    /**
     * Parses and expands the shorthand format.
     *
     * @param s the string to expand
     * @return the expanded result
     */
    public static Set<String> expandShorthand(String s) {
        Set<String> results = new HashSet<>();
        results.add(s);

        Set<String> workSet = new HashSet<>();
        while (true) {

            boolean work = false;
            for (String string : results) {
                Set<String> expanded = matchGroup(string);
                if (expanded != null) {
                    work = true;
                    workSet.addAll(expanded);
                } else {
                    workSet.add(string);
                }
            }

            if (work) {
                // set results := workSet, and init an empty workSet for the next iteration
                // (we actually sneakily reuse the existing results HashSet to avoid having to create a new one)
                Set<String> temp = results;
                results = workSet;
                workSet = temp;
                workSet.clear();
                continue;
            }

            break;
        }

        // remove self
        results.remove(s);

        return results;
    }

    private static Set<String> matchGroup(String input) {
        int openingIndex = indexOfEither(input, OPEN_GROUP, OPEN_GROUP_2);
        if (openingIndex == -1) {
            return null;
        }

        int closingIndex = indexOfEither(input, CLOSE_GROUP, CLOSE_GROUP_2);
        if (closingIndex < openingIndex) {
            return null;
        }

        String before = input.substring(0, openingIndex);
        String after = input.substring(closingIndex + 1);
        String between = input.substring(openingIndex + 1, closingIndex);

        Set<String> results = new HashSet<>();

        for (ShorthandParser parser : PARSERS) {
            try {
                Iterator<String> extracted = parser.extract(between);
                if (extracted != null) {
                    while (extracted.hasNext()) {
                        results.add(before + extracted.next() + after);
                    }

                    // break after one parser has matched
                    break;
                }
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return results;
    }

    private static int indexOfEither(String s, char c1, char c2) {
        int index = s.indexOf(c1);
        if (index != -1) {
            return index;
        }
        return s.indexOf(c2);
    }

    /**
     * Implements an iterator over a given range of ints.
     */
    private abstract static class RangeIterator implements Iterator<String> {
        private static final int MAX_RANGE = 250;

        private final int max;
        private int next;

        RangeIterator(int a, int b) {
            this.max = Math.max(a, b);
            this.next = Math.min(a, b);
            if ((this.max - this.next) > MAX_RANGE) {
                throw new IllegalArgumentException("Range too large");
            }
        }

        protected abstract String toString(int i);

        @Override
        public final boolean hasNext() {
            return this.next <= this.max;
        }

        @Override
        public final String next() {
            return toString(this.next++);
        }
    }

}
