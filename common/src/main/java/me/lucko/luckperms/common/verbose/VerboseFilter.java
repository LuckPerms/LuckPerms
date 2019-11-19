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

import me.lucko.luckperms.common.verbose.event.VerboseEvent;
import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler;
import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler.AST;
import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler.LexerException;
import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler.ParserException;

/**
 * Represents a verbose filter expression.
 *
 * <p>The expression is compiled when the instance is initialised - subsequent
 * evaluations should be relatively fast.</p>
 */
public final class VerboseFilter {
    private final String expression;
    private final AST ast;

    public VerboseFilter(String expression) throws InvalidFilterException {
        this.expression = expression;
        if (expression.isEmpty()) {
            this.ast = AST.ALWAYS_TRUE;
        } else {
            try {
                this.ast = BooleanExpressionCompiler.compile(expression);
            } catch (LexerException | ParserException e) {
                throw new InvalidFilterException("Exception occurred whilst generating an expression for '" + expression + "'", e);
            }
        }
    }

    /**
     * Evaluates whether the check data passes the filter
     *
     * @param data the check data
     * @return if the check data passes the filter
     */
    public boolean evaluate(VerboseEvent data) {
        try {
            return this.ast.eval(data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isBlank() {
        return this.ast == AST.ALWAYS_TRUE;
    }

    @Override
    public String toString() {
        return isBlank() ? "any" : this.expression;
    }
}
