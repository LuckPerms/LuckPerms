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
