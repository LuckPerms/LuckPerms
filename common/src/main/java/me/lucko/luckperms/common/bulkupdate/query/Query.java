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

package me.lucko.luckperms.common.bulkupdate.query;

import me.lucko.luckperms.common.bulkupdate.PreparedStatementBuilder;
import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.node.Node;

/**
 * Represents a query component
 */
public class Query {

    public static Query of(QueryField field, Constraint constraint) {
        return new Query(field, constraint);
    }

    // the field this query is comparing against
    private final QueryField field;

    // the constraint
    private final Constraint constraint;

    private Query(QueryField field, Constraint constraint) {
        this.field = field;
        this.constraint = constraint;
    }

    /**
     * Returns if the given node satisfies this query
     *
     * @param node the node
     * @return true if satisfied
     */
    public boolean isSatisfiedBy(Node node) {
        switch (this.field) {
            case PERMISSION:
                return this.constraint.eval(node.getKey());
            case SERVER:
                return this.constraint.eval(node.getContexts().getAnyValue(DefaultContextKeys.SERVER_KEY).orElse("global"));
            case WORLD:
                return this.constraint.eval(node.getContexts().getAnyValue(DefaultContextKeys.WORLD_KEY).orElse("global"));
            default:
                throw new RuntimeException();
        }
    }

    public void appendSql(PreparedStatementBuilder builder) {
        this.constraint.appendSql(builder, this.field.getSqlName());
    }

    public QueryField getField() {
        return this.field;
    }

    public Constraint getConstraint() {
        return this.constraint;
    }
}
