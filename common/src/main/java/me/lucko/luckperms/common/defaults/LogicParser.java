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

package me.lucko.luckperms.common.defaults;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.utils.Scripting;

import java.util.function.Function;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;

public class LogicParser {
    public static boolean parse(String s, PermissionHolder holder, Tristate tristate) throws IllegalArgumentException {
        try {
            ScriptEngine engine = Scripting.getScriptEngine();
            if (engine == null) {
                throw new NullPointerException("script engine");
            }

            String expression = generateExpression(s, s1 -> holder.hasPermission(NodeFactory.fromSerializedNode(s1, true)) == tristate);
            String result = engine.eval(expression).toString();

            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException();
            }

            return Boolean.parseBoolean(result);

        } catch (Throwable t) {
            throw new IllegalArgumentException(s, t);
        }
    }

    private static String generateExpression(String input, Function<String, Boolean> checker) {
        while (true) {
            int i = input.indexOf("<");
            int i2 = input.indexOf(">");
            if (i == -1 || i2 == -1) {
                break;
            }

            String match = input.substring(i, i2 + 1);
            String matchContent = match.substring(1, match.length() - 1);

            String matchReplacement = ("" + checker.apply(matchContent)).toLowerCase();

            input = input.replaceFirst(Pattern.quote(match), matchReplacement);
        }

        return input.replace("&", "&&").replace("|", "||");
    }
}
