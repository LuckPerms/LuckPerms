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

public class Constraint {

    public static Constraint of(Comparison comparison, String expression) {
        return new Constraint(comparison, expression);
    }

    private final Comparison comparison;
    private final String expressionValue;
    private final Comparison.CompiledExpression compiledExpression;

    private Constraint(Comparison comparison, String expression) {
        this.comparison = comparison;
        this.expressionValue = expression;
        this.compiledExpression = this.comparison.compile(this.expressionValue);
    }

    /**
     * Returns if the given value satisfies this constraint
     *
     * @param value the value
     * @return true if satisfied
     */
    public boolean eval(String value) {
        return this.compiledExpression.test(value);
    }

    public void appendSql(PreparedStatementBuilder builder, String field) {
        // e.g. field LIKE ?
        builder.append(field + " ");
        this.comparison.appendSql(builder);
        builder.append(" ?");
        builder.variable(this.expressionValue);
    }

    @Override
    public String toString() {
        return this.comparison + " " + this.expressionValue;
    }
}
