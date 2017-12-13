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

package me.lucko.luckperms.common.assignments;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.LegacyNodeFactory;
import me.lucko.luckperms.common.utils.Scripting;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

public class AssignmentExpression {

    public static AssignmentExpression compile(String expression) {
        if (expression == null) {
            return null;
        }
        return new AssignmentExpression(expression);
    }

    private final List<Token> expression;

    private AssignmentExpression(String expression) {
        this.expression = generateExpression(expression);
    }

    public boolean parse(PermissionHolder holder, Tristate tristate) throws IllegalArgumentException {
        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            throw new NullPointerException("script engine");
        }

        Predicate<Node> checker = node -> holder.hasPermission(node) == tristate;

        String exp = expression.stream().map(t -> t.forExpression(checker)).collect(Collectors.joining())
                .replace("&", "&&").replace("|", "||");

        try {
            String result = engine.eval(exp).toString();

            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException();
            }

            return Boolean.parseBoolean(result);

        } catch (Throwable t) {
            throw new IllegalArgumentException(exp, t);
        }
    }

    private static List<Token> generateExpression(String input) {
        ImmutableList.Builder<Token> exp = ImmutableList.builder();

        while (true) {
            int start = input.indexOf("<");
            int end = input.indexOf(">");
            if (start == -1 || end == -1) {
                break;
            }

            if (start != 0) {
                Token before = new StringToken(input.substring(0, start));
                exp.add(before);
            }

            String match = input.substring(start, end + 1);
            String matchContent = match.substring(1, match.length() - 1);

            Token permission = new PermissionToken(matchContent);
            exp.add(permission);

            input = input.substring(end + 1);
        }

        if (!input.isEmpty()) {
            exp.add(new StringToken(input));
        }

        return exp.build();
    }

    private interface Token {
        String forExpression(Predicate<Node> checker);
    }

    private static final class StringToken implements Token {
        private final String string;

        private StringToken(String string) {
            this.string = string;
        }

        @Override
        public String forExpression(Predicate<Node> checker) {
            return string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static final class PermissionToken implements Token {
        private final String permission;
        private final Node node;

        private PermissionToken(String permission) {
            this.permission = permission;
            this.node = LegacyNodeFactory.fromLegacyString(permission, true);
        }

        @Override
        public String forExpression(Predicate<Node> checker) {
            return Boolean.toString(checker.test(node));
        }

        @Override
        public String toString() {
            return "<" + permission + ">";
        }
    }
}
