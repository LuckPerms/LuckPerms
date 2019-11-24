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

package me.lucko.luckperms.common.bulkupdate;

import me.lucko.luckperms.common.bulkupdate.action.Action;
import me.lucko.luckperms.common.bulkupdate.query.Query;

import net.luckperms.api.node.Node;

import java.util.List;
import java.util.Objects;

/**
 * Represents a query to be applied to a set of data.
 * Queries can either be applied to im-memory sets of data, or converted to SQL syntax to be executed remotely.
 */
public final class BulkUpdate {

    // the data types which this query should apply to
    private final DataType dataType;

    // the action to apply to the data which matches the constraints
    private final Action action;

    // a set of constraints which data must match to be acted upon
    private final List<Query> queries;

    public BulkUpdate(DataType dataType, Action action, List<Query> queries) {
        this.dataType = dataType;
        this.action = action;
        this.queries = queries;
    }

    /**
     * Check to see if a Node instance satisfies the constrints of this query
     *
     * @param node the node to check
     * @return true if satisfied
     */
    public boolean satisfiesConstraints(Node node) {
        for (Query query : this.queries) {
            if (!query.isSatisfiedBy(node)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Applies this query to the given NodeModel, and returns the result.
     *
     * @param from the node to base changes from
     * @return the new nodemodel instance, or null if the node should be deleted.
     */
    public Node apply(Node from) {
        if (!satisfiesConstraints(from)) {
            return from; // make no change
        }

        return this.action.apply(from);
    }

    /**
     * Converts this {@link BulkUpdate} to SQL syntax
     *
     * @return this query in SQL form
     */
    public PreparedStatementBuilder buildAsSql() {
        // DELETE FROM {table} WHERE ...
        // UPDATE {table} SET ... WHERE ...

        PreparedStatementBuilder builder = new PreparedStatementBuilder();

        // add the action
        // (DELETE FROM or UPDATE)
        this.action.appendSql(builder);

        // if there are no constraints, just return without a WHERE clause
        if (this.queries.isEmpty()) {
            return builder;
        }

        // append constraints
        builder.append(" WHERE");
        for (int i = 0; i < this.queries.size(); i++) {
            Query query = this.queries.get(i);

            builder.append(" ");
            if (i != 0) {
                builder.append("AND ");
            }

            query.appendSql(builder);
        }

        return builder;
    }

    public DataType getDataType() {
        return this.dataType;
    }

    public Action getAction() {
        return this.action;
    }

    public List<Query> getQueries() {
        return this.queries;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof BulkUpdate)) return false;
        final BulkUpdate that = (BulkUpdate) o;

        return this.getDataType() == that.getDataType() &&
                Objects.equals(this.getAction(), that.getAction()) &&
                Objects.equals(this.getQueries(), that.getQueries());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), getAction(), getQueries());
    }

    @Override
    public String toString() {
        return "BulkUpdate(" +
                "dataType=" + this.getDataType() + ", " +
                "action=" + this.getAction() + ", " +
                "constraints=" + this.getQueries() + ")";
    }
}
