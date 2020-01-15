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

package me.lucko.luckperms.common.command.utils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tokenizes command input into distinct "argument" tokens.
 *
 * <p>Splits on whitespace, except when surrounded by quotes.</p>
 */
public enum ArgumentTokenizer {

    EXECUTE {
        @Override
        public List<String> tokenizeInput(String[] args) {
            return stripQuotes(EXECUTE_ARGUMENT_SPLITTER.split(ARGUMENT_JOINER.join(args)));
        }

        @Override
        public List<String> tokenizeInput(String args) {
            return stripQuotes(EXECUTE_ARGUMENT_SPLITTER.split(args));
        }
    },
    TAB_COMPLETE {
        @Override
        public List<String> tokenizeInput(String[] args) {
            return stripQuotes(TAB_COMPLETE_ARGUMENT_SPLITTER.split(ARGUMENT_JOINER.join(args)));
        }

        @Override
        public List<String> tokenizeInput(String args) {
            return stripQuotes(TAB_COMPLETE_ARGUMENT_SPLITTER.split(args));
        }
    };

    private static final Pattern ARGUMENT_SEPARATOR_PATTERN = Pattern.compile(" (?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
    private static final Splitter TAB_COMPLETE_ARGUMENT_SPLITTER = Splitter.on(ARGUMENT_SEPARATOR_PATTERN);
    private static final Splitter EXECUTE_ARGUMENT_SPLITTER = TAB_COMPLETE_ARGUMENT_SPLITTER.omitEmptyStrings();
    private static final Joiner ARGUMENT_JOINER = Joiner.on(' ');

    public abstract List<String> tokenizeInput(String[] args);

    public abstract List<String> tokenizeInput(String args);

    private static List<String> stripQuotes(Iterable<String> input) {
        List<String> list = new ArrayList<>();
        for (String argument : input) {
            if (argument.length() >= 3 && argument.charAt(0) == '"' && argument.charAt(argument.length() - 1) == '"') {
                list.add(argument.substring(1, argument.length() - 1));
            } else {
                list.add(argument);
            }
        }
        return list;
    }
}
