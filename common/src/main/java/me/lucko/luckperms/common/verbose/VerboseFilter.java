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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.util.Scripting;
import me.lucko.luckperms.common.verbose.event.MetaCheckEvent;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;
import me.lucko.luckperms.common.verbose.event.VerboseEvent;

import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Represents a verbose filter expression.
 *
 * <p>The filter is parsed when the instance is initialised - subsequent
 * evaluations should be relatively fast.</p>
 */
public final class VerboseFilter {

    // the characters used in an expression which are part of the expression
    // syntax - and not the filter itself.
    private static final String DELIMITERS = " |&()!";

    // the script engine to use when evaluating the expression
    private final ScriptEngine engine;
    // the parsed expression
    private final List<Token> expression;

    /**
     * Compiles a {@link VerboseFilter} instance for the given filter string
     *
     * @param filter the filter
     * @return a filter
     * @throws InvalidFilterException if the filter is invalid
     */
    public static VerboseFilter parse(String filter) throws InvalidFilterException {
        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            throw new RuntimeException("Script engine not present");
        }

        return new VerboseFilter(engine, filter);
    }

    private VerboseFilter(ScriptEngine engine, String filter) throws InvalidFilterException {
        this.engine = engine;

        if (filter.isEmpty()) {
            this.expression = ImmutableList.of();
        } else {
            try {
                this.expression = generateExpression(engine, filter);
            } catch (Exception e) {
                throw new InvalidFilterException("Exception occurred whilst generating an expression for '" + filter + "'", e);
            }
        }
    }

    /**
     * Parses a filter string into a list of 'tokens' forming the expression.
     *
     * Each token either represents part of the expressions syntax
     * (logical and, logical or, brackets, or space) or a value.
     *
     * @param engine the script engine to test the expression with
     * @param filter the filter string
     * @return a parsed list of expressions
     * @throws ScriptException if the engine throws an exception whilst evaluating the expression
     */
    private static List<Token> generateExpression(ScriptEngine engine, String filter) throws ScriptException {
        // tokenize the filter using the filter characters as delimiters.
        StringTokenizer tokenizer = new StringTokenizer(filter, DELIMITERS, true);

        // use the tokenizer to parse the string to a list of 'tokens'.
        ImmutableList.Builder<Token> expressionBuilder = ImmutableList.builder();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (isDelimiter(token)) {
                // if the token is a delimiter, just append it to the expression as a constant
                expressionBuilder.add(new ConstantToken(token));
            } else {
                // otherwise consider it to be a value
                expressionBuilder.add(new VariableToken(token));
            }
        }

        // build & test the expression
        List<Token> expression = expressionBuilder.build();
        testExpression(expression, engine);
        return expression;
    }

    private static void testExpression(List<Token> expression, ScriptEngine engine) throws ScriptException {
        // build a dummy version of the expression.
        // all values are simply replaced by "true"
        String dummyExpression = expression.stream().map(Token::forDummyExpression).collect(Collectors.joining());

        // do a test run - if the engine returns a result without throwing an exception
        // and the result is a boolean, we can consider the expression to be valid
        String result = engine.eval(dummyExpression).toString();
        if (!result.equals("true") && !result.equals("false")) {
            throw new IllegalArgumentException("Expected true/false but got '" + result + "' instead.");
        }
    }

    /**
     * Evaluates whether the check data passes the filter
     *
     * @param data the check data
     * @return if the check data passes the filter
     */
    public boolean evaluate(VerboseEvent data) {
        if (this.expression.isEmpty()) {
            return true;
        }

        // build an expression string for the passed check data.
        String expressionString = this.expression.stream().map(token -> token.forExpression(data)).collect(Collectors.joining());

        // evaluate the expression using the script engine
        try {
            String result = this.engine.eval(expressionString).toString();

            // validate return value
            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException("Expected true/false but got '" + result + "' instead.");
            }

            // return the result of the expression
            return Boolean.parseBoolean(result);

        } catch (Throwable ex) {
            // print the error & return false
            ex.printStackTrace();
            return false;
        }
    }

    public boolean isBlank() {
        return this.expression.isEmpty();
    }

    @Override
    public String toString() {
        return this.expression.stream().map(Token::toString).collect(Collectors.joining());
    }

    /**
     * Returns true if the string is equal to one of the {@link #DELIMITERS}.
     *
     * @param string the string
     * @return true if delimiter, false otherwise
     */
    private static boolean isDelimiter(String string) {
        switch (string.charAt(0)) {
            case ' ':
            case '|':
            case '&':
            case '(':
            case ')':
            case '!':
                return true;
            default:
                return false;
        }
    }

    /**
     * Represents a part of an expression
     */
    private interface Token {

        /**
         * Returns the value of this token when part of an evaluated expression
         *
         * @param event the data which an expression is being formed for
         * @return the value to be used as part of the evaluated expression
         */
        String forExpression(VerboseEvent event);

        /**
         * Returns a 'dummy' value for this token in order to build a test
         * expression.
         *
         * @return the value to be used as part of the test expression
         */
        String forDummyExpression();

    }

    /**
     * Represents a constant part of the expression - tokens will only ever
     * consist of the characters defined in the {@link #DELIMITERS} string.
     */
    private static final class ConstantToken implements Token {
        private final String string;

        private ConstantToken(String string) {
            // replace single '&' and '|' character with double values
            if (string.equals("&")) {
                string = "&&";
            } else if (string.equals("|")) {
                string = "||";
            }

            this.string = string;
        }

        @Override
        public String forExpression(VerboseEvent event) {
            return this.string;
        }

        @Override
        public String forDummyExpression() {
            return this.string;
        }

        @Override
        public String toString() {
            return this.string;
        }
    }

    /**
     * Represents a variable part of the token. When evaluated as an expression,
     * this token will be replaced with a boolean 'true' or 'false' - depending
     * on if the passed check data "matches" the value of this token.
     *
     * The check data will be deemed a "match" if:
     * - the target of the check is equal to the value of the token
     * - the permission/meta key being checked for starts with the value of the token
     * - the result of the check is equal to the value of the token
     */
    private static final class VariableToken implements Token {
        private final String value;

        private VariableToken(String value) {
            this.value = value;
        }

        @Override
        public String forExpression(VerboseEvent event) {
            if (event instanceof PermissionCheckEvent) {
                PermissionCheckEvent permissionEvent = (PermissionCheckEvent) event;
                return Boolean.toString(
                        this.value.equals("permission") ||
                                permissionEvent.getCheckTarget().equalsIgnoreCase(this.value) ||
                                permissionEvent.getPermission().toLowerCase().startsWith(this.value.toLowerCase()) ||
                                permissionEvent.getResult().result().name().equalsIgnoreCase(this.value)
                );
            }

            if (event instanceof MetaCheckEvent) {
                MetaCheckEvent metaEvent = (MetaCheckEvent) event;
                return Boolean.toString(
                        this.value.equals("meta") ||
                                metaEvent.getCheckTarget().equalsIgnoreCase(this.value) ||
                                metaEvent.getKey().toLowerCase().startsWith(this.value.toLowerCase()) ||
                                metaEvent.getResult().equalsIgnoreCase(this.value)
                );
            }

            throw new IllegalArgumentException("Unknown event type: " + event);

        }

        @Override
        public String forDummyExpression() {
            return "true";
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

}
