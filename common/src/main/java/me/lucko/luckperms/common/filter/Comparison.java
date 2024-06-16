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

package me.lucko.luckperms.common.filter;

import java.util.regex.Pattern;

/**
 * A method of comparing two strings
 */
public enum Comparison {

    EQUAL("=="),
    NOT_EQUAL("!="),
    SIMILAR("~~"),
    NOT_SIMILAR("!~");

    public static final String WILDCARD = "%";
    public static final String WILDCARD_ONE = "_";

    private final String symbol;

    Comparison(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Gets the symbol which represents this comparison
     *
     * @return the comparison symbol
     */
    public String getSymbol() {
        return this.symbol;
    }

    @Override
    public String toString() {
        return this.symbol;
    }

    public static Comparison parse(String s) {
        for (Comparison t : values()) {
            if (t.getSymbol().equals(s)) {
                return t;
            }
        }
        return null;
    }

    public static Pattern compilePatternForLikeSyntax(String expression) {
        expression = expression.replace(".", "\\.");

        // convert from SQL LIKE syntax to regex
        expression = expression.replace(WILDCARD_ONE, ".");
        expression = expression.replace(WILDCARD, ".*");

        return Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
    }

}
