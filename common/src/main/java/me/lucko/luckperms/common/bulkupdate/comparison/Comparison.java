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

package me.lucko.luckperms.common.bulkupdate.comparison;

import me.lucko.luckperms.common.bulkupdate.PreparedStatementBuilder;

/**
 * A method of comparing two strings
 */
public interface Comparison {

    /**
     * Gets the symbol which represents this comparison
     *
     * @return the comparison symbol
     */
    String getSymbol();

    /**
     * Creates a {@link CompiledExpression} for the given expression
     *
     * @param expression the expression
     * @return the compiled expression
     */
    CompiledExpression compile(String expression);

    /**
     * Returns the comparison operator in SQL form
     */
    void appendSql(PreparedStatementBuilder builder);

    /**
     * An instance of {@link Comparison} which is bound to an expression.
     */
    interface CompiledExpression {

        /**
         * Tests the expression against a given string, according to the
         * rules of the parent {@link Comparison}.
         *
         * @param string the string
         * @return if there was a match
         */
        boolean test(String string);
    }

}
