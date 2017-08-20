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

package me.lucko.luckperms.common.verbose;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.common.utils.Scripting;

import java.util.StringTokenizer;

import javax.script.ScriptEngine;

/**
 * Tests verbose filters
 */
@UtilityClass
public class VerboseFilter {

    /**
     * Evaluates whether the passed check data passes the filter
     *
     * @param data the check data
     * @param filter the filter
     * @return if the check data passes the filter
     */
    public static boolean passesFilter(CheckData data, String filter) {
        if (filter.equals("")) {
            return true;
        }

        // get the script engine
        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            return false;
        }

        // tokenize the filter
        StringTokenizer tokenizer = new StringTokenizer(filter, " |&()!", true);

        // build an expression which can be evaluated by the javascript engine
        StringBuilder expressionBuilder = new StringBuilder();

        // read the tokens
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            // if the token is a delimiter, just append it to the expression
            if (isDelim(token)) {
                expressionBuilder.append(token);

            } else {

                // if the token is not a delimiter, it must be a string.
                // we replace non-delimiters with a boolean depending on if the string matches the check data.
                boolean value = data.getCheckTarget().equalsIgnoreCase(token) ||
                        data.getPermission().toLowerCase().startsWith(token.toLowerCase()) ||
                        data.getResult().name().equalsIgnoreCase(token);

                expressionBuilder.append(value);
            }
        }

        // build the expression
        String expression = expressionBuilder.toString().replace("&", "&&").replace("|", "||");

        // evaluate the expression using the script engine
        try {
            String result = engine.eval(expression).toString();
            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException(expression + " - " + result);
            }

            return Boolean.parseBoolean(result);

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return false;
    }

    /**
     * Tests whether a filter is valid
     *
     * @param filter the filter to test
     * @return true if the filter is valid
     */
    public static boolean isValidFilter(String filter) {
        if (filter.equals("")) {
            return true;
        }

        // get the script engine
        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            return false;
        }

        // tokenize the filter
        StringTokenizer tokenizer = new StringTokenizer(filter, " |&()!", true);

        // build an expression which can be evaluated by the javascript engine
        StringBuilder expressionBuilder = new StringBuilder();

        // read the tokens
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            // if the token is a delimiter, just append it to the expression
            if (isDelim(token)) {
                expressionBuilder.append(token);
            } else {
                expressionBuilder.append("true"); // dummy result
            }
        }

        // build the expression
        String expression = expressionBuilder.toString().replace("&", "&&").replace("|", "||");

        // evaluate the expression using the script engine
        try {
            String result = engine.eval(expression).toString();
            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException(expression + " - " + result);
            }

            return true;

        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDelim(String token) {
        return token.equals(" ") ||
                token.equals("|") ||
                token.equals("&") ||
                token.equals("(") ||
                token.equals(")") ||
                token.equals("!");
    }

}
