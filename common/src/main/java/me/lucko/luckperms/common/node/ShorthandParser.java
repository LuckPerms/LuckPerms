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

package me.lucko.luckperms.common.node;

import lombok.experimental.UtilityClass;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@UtilityClass
public class ShorthandParser {
    private static final List<Function<String, Iterable<String>>> PARSERS = ImmutableList.<Function<String, Iterable<String>>>builder()
            .add(new ListParser())
            .add(new CharacterRangeParser())
            .add(new NumericRangeParser())
            .build();

    public static Set<String> parseShorthand(String s) {
        Set<String> results = new HashSet<>(1);
        results.add(s);

        while (true) {
            Set<String> working = new HashSet<>(results.size());
            int beforeSize = results.size();

            for (String str : results) {
                Set<String> ret = captureResults(str);
                if (ret != null) {
                    working.addAll(ret);
                } else {
                    working.add(str);
                }
            }

            if (working.size() != beforeSize) {
                results = working;
                continue;
            }

            break;
        }

        // remove self
        results.remove(s);

        return results;
    }

    private static Set<String> captureResults(String s) {
        s = s.replace('(', '{').replace(')', '}');

        int openingIndex = s.indexOf('{');
        if (openingIndex == -1) {
            return null;
        }

        int closingIndex = s.indexOf('}');
        if (closingIndex < openingIndex) {
            return null;
        }

        String before = s.substring(0, openingIndex);
        String after = s.substring(closingIndex + 1);
        String between = s.substring(openingIndex + 1, closingIndex);

        Set<String> results = new HashSet<>(10);

        for (Function<String, Iterable<String>> parser : PARSERS) {
            Iterable<String> res = parser.apply(between);
            if (res != null) {
                for (String r : res) {
                    results.add(before + r + after);
                }
            }
        }

        return results;
    }

    private static class ListParser implements Function<String, Iterable<String>> {
        private static final Splitter SPLITTER = Splitter.on(',');

        @Override
        public Iterable<String> apply(String s) {
            s = s.replace('|', ',');
            if (!s.contains(",")) {
                return null;
            }
            return SPLITTER.split(s);
        }
    }

    private static class NumericRangeParser implements Function<String, Iterable<String>> {

        private static Integer parseInt(String a) {
            try {
                return Integer.parseInt(a);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public Iterable<String> apply(String s) {
            int index = s.indexOf("-");
            if (index == -1) return null;

            Integer before = parseInt(s.substring(0, index));
            if (before == null) return null;

            Integer after = parseInt(s.substring(index + 1));
            if (after == null) return null;

            return IntStream.rangeClosed(before, after).mapToObj(Integer::toString).collect(Collectors.toList());
        }
    }

    private static class CharacterRangeParser implements Function<String, Iterable<String>> {

        private static List<String> getCharRange(char a, char b) {
            List<String> s = new ArrayList<>();
            for (char c = a; c <= b; c++) {
                s.add(Character.toString(c));
            }
            return s;
        }

        @Override
        public Iterable<String> apply(String s) {
            int index = s.indexOf("-");
            if (index == -1) return null;

            String before = s.substring(0, index);
            if (before.length() != 1) return null;

            String after = s.substring(index + 1);
            if (after.length() != 1) return null;

            return getCharRange(before.charAt(0), after.charAt(0));
        }
    }

}
