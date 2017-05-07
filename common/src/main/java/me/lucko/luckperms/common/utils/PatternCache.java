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

import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@UtilityClass
public class PatternCache {

    private static final NullablePattern NULL_PATTERN = new NullablePattern(null);

    private static final LoadingCache<String, NullablePattern> CACHE = Caffeine.newBuilder().build(s -> {
        try {
            return new NullablePattern(Pattern.compile(s));
        } catch (PatternSyntaxException e) {
            return NULL_PATTERN;
        }
    });

    private static final LoadingCache<Map.Entry<String, String>, String> DELIMITER_CACHE = Caffeine.newBuilder()
            .build(e -> {
                // note the reversed order
                return "(?<!" + Pattern.quote(e.getValue()) + ")" + Pattern.quote(e.getKey());
            });

    public static Pattern compile(String regex) {
        return CACHE.get(regex).pattern;
    }

    public static String buildDelimitedMatcher(String delim, String esc) {
        return DELIMITER_CACHE.get(Maps.immutableEntry(delim, esc));
    }

    public static Pattern compileDelimitedMatcher(String delim, String esc) {
        return compile(buildDelimitedMatcher(delim, esc));
    }

    @AllArgsConstructor
    private static final class NullablePattern {
        private final Pattern pattern;
    }

}
