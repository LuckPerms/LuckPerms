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

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizes strings on whitespace, but ignoring whitespace enclosed within quotes.
 */
public class QuotedStringTokenizer {
    private final String string;
    private int cursor;

    public QuotedStringTokenizer(String string) {
        this.string = string;
    }

    public List<String> tokenize(boolean omitEmptyStringAtEnd) {
        List<String> output = new ArrayList<>();
        while (hasNext()) {
            output.add(readString());
        }
        if (!omitEmptyStringAtEnd && this.cursor > 0 && isWhitespace(peek(-1))) {
            output.add("");
        }
        return output;
    }

    private static boolean isQuoteCharacter(char c) {
        // return c == '"' || c == '“' || c == '”';
        return c == '\u0022' || c == '\u201C' || c == '\u201D';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ';
    }

    private String readString() {
        if (isQuoteCharacter(peek())) {
            return readQuotedString();
        } else {
            return readUnquotedString();
        }
    }

    private String readUnquotedString() {
        final int start = this.cursor;
        while (hasNext() && !isWhitespace(peek())) {
            skip();
        }
        final int end = this.cursor;

        if (hasNext()) {
            skip(); // skip whitespace
        }

        return this.string.substring(start, end);
    }

    private String readQuotedString() {
        skip(); // skip start quote

        final int start = this.cursor;
        while (hasNext() && !isQuoteCharacter(peek())) {
            skip();
        }
        final int end = this.cursor;

        if (hasNext()) {
            skip(); // skip end quote
        }
        if (hasNext() && isWhitespace(peek())) {
            skip(); // skip whitespace
        }

        return this.string.substring(start, end);
    }

    private boolean hasNext() {
        return this.cursor + 1 <= this.string.length();
    }

    private char peek() {
        return this.string.charAt(this.cursor);
    }

    private char peek(int offset) {
        return this.string.charAt(this.cursor + offset);
    }

    private void skip() {
        this.cursor++;
    }

}
