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

package me.lucko.luckperms.common.constants;

import lombok.experimental.UtilityClass;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@UtilityClass
public class Patterns {
    private static final LoadingCache<String, Pattern> CACHE = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Pattern>() {
                @Override
                public Pattern load(String s) throws Exception {
                    return Pattern.compile(s);
                }

                @Override
                public ListenableFuture<Pattern> reload(String s, Pattern pattern) {
                    return Futures.immediateFuture(pattern);
                }
            });

    public static final Pattern COMMAND_SEPARATOR = Pattern.compile(" (?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
    public static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[\\/\\$\\.\\- ]");
    public static final Pattern NON_ALPHA_NUMERIC_SPACE = Pattern.compile("[\\/\\$\\.\\-]");
    public static final Pattern NON_USERNAME = Pattern.compile("[^A-Za-z0-9_ ]");
    public static final Pattern SHORTHAND_NODE = Pattern.compile("\\.\\([^.]+\\)");
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + String.valueOf('§') + "[0-9A-FK-OR]");
    public static final Pattern NODE_CONTEXTS = Pattern.compile("\\(.+\\).*");

    public static Pattern compile(String regex) {
        try {
            return CACHE.get(regex);
        } catch (UncheckedExecutionException | ExecutionException e) {
            return null;
        }
    }

}
