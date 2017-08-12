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

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.common.bulkupdate.action.Action;
import me.lucko.luckperms.common.bulkupdate.constraint.Constraint;
import me.lucko.luckperms.common.node.NodeModel;

import java.util.List;

/**
 * Represents a query to be applied to a set of data.
 * Queries can either be applied to im-memory sets of data, or converted to SQL syntax to be executed remotely.
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class BulkUpdate {

    // the data types which this query should apply to
    private final DataType dataType;

    // the action to apply to the data which matches the constraints
    private final Action action;

    // a set of constraints which data must match to be acted upon
    private final List<Constraint> constraints;

    /**
     * Check to see if a Node instance satisfies the constrints of this query
     *
     * @param node the node to check
     * @return true if satisfied
     */
    public boolean satisfiesConstraints(NodeModel node) {
        for (Constraint constraint : constraints) {
            if (!constraint.isSatisfiedBy(node)) {
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
    public NodeModel apply(NodeModel from) {
        if (!satisfiesConstraints(from)) {
            return from; // make no change
        }

        return action.apply(from);
    }

    /**
     * Converts this {@link BulkUpdate} to SQL syntax
     *
     * @return this query in SQL form
     */
    public String buildAsSql() {
        // DELETE FROM {table} WHERE ...
        // UPDATE {table} SET ... WHERE ...

        StringBuilder sb = new StringBuilder();

        // add the action
        // (DELETE FROM or UPDATE)
        sb.append(action.getAsSql());

        // if there are no constraints, just return without a WHERE clause
        if (constraints.isEmpty()) {
            return sb.append(";").toString();
        }

        // append constraints
        sb.append(" WHERE");
        for (int i = 0; i < constraints.size(); i++) {
            Constraint constraint = constraints.get(i);

            sb.append(" ");
            if (i != 0) {
                sb.append("AND ");
            }

            sb.append(constraint.getAsSql());
        }

        return sb.append(";").toString();
    }

    /**
     * Utility to appropriately escape a string for use in a query.
     *
     * @param s the string to escape
     * @return an escaped string
     */
    public static String escapeStringForSql(String s) {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
            return s.toLowerCase();
        }

        try {
            Integer.parseInt(s);
            return s;
        } catch (NumberFormatException e) {
            // ignored
        }

        return "'" + s + "'";
    }

}
