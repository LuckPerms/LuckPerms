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
    private static final List<Function<String, List<String>>> PARSERS = ImmutableList.<Function<String, List<String>>>builder()
            .add(new ListParser())
            .add(new CharacterRangeParser())
            .add(new NumericRangeParser())
            .build();

    public static Set<String> parseShorthand(String s) {
        return parseShorthand(s, true);
    }

    public static Set<String> parseShorthand(String s, boolean removeSelf) {
        Set<String> results = new HashSet<>();
        results.add(s);

        while (true) {
            Set<String> working = new HashSet<>();
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

        if (removeSelf) {
            results.remove(s);
        }

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

        Set<String> results = new HashSet<>();

        for (Function<String, List<String>> parser : PARSERS) {
            List<String> res = parser.apply(between);
            if (res != null) {
                for (String r : res) {
                    results.add(before + r + after);
                }
            }
        }

        return results;
    }

    private static class ListParser implements Function<String, List<String>> {

        @Override
        public List<String> apply(String s) {
            s = s.replace('|', ',');
            if (!s.contains(",")) {
                return null;
            }
            return Splitter.on(',').splitToList(s);
        }
    }

    private static class NumericRangeParser implements Function<String, List<String>> {

        private static boolean isInt(String a) {
            try {
                Integer.parseInt(a);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }

        @Override
        public List<String> apply(String s) {
            int index = s.indexOf("-");
            if (index == -1) {
                return null;
            }
            String before = s.substring(0, index);
            String after = s.substring(index + 1);

            if (isInt(before) && isInt(after)) {
                return IntStream.rangeClosed(Integer.parseInt(before), Integer.parseInt(after))
                        .boxed().map(i -> "" + i).collect(Collectors.toList());
            }
            return null;
        }
    }

    private static class CharacterRangeParser implements Function<String, List<String>> {

        private static List<String> getCharRange(char a, char b) {
            List<String> s = new ArrayList<>();
            for (char c = a; c <= b; c++) {
                s.add(Character.toString(c));
            }
            return s;
        }

        @Override
        public List<String> apply(String s) {
            int index = s.indexOf("-");
            if (index == -1) {
                return null;
            }
            String before = s.substring(0, index);
            String after = s.substring(index + 1);

            if (before.length() == 1 && after.length() == 1) {
                return getCharRange(before.charAt(0), after.charAt(0));
            }
            return null;
        }
    }

}
