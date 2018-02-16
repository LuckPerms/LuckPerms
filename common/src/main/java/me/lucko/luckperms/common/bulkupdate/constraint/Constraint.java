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

package me.lucko.luckperms.common.bulkupdate.constraint;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparisons.Comparison;
import me.lucko.luckperms.common.node.NodeModel;

/**
 * Represents a query constraint
 */
public class Constraint {

    public static Constraint of(QueryField field, Comparison comparison, String value) {
        return new Constraint(field, comparison, value);
    }

    // the field this constraint is comparing against
    private final QueryField field;

    // the comparison type being used in this constraint
    private final Comparison comparison;

    // the expression being compared against
    private final String value;

    private Constraint(QueryField field, Comparison comparison, String value) {
        this.field = field;
        this.comparison = comparison;
        this.value = value;
    }

    /**
     * Returns if the given node satisfies this constraint
     *
     * @param node the node
     * @return true if satisfied
     */
    public boolean isSatisfiedBy(NodeModel node) {
        switch (this.field) {
            case PERMISSION:
                return this.comparison.matches(node.getPermission(), this.value);
            case SERVER:
                return this.comparison.matches(node.getServer(), this.value);
            case WORLD:
                return this.comparison.matches(node.getWorld(), this.value);
            default:
                throw new RuntimeException();
        }
    }

    public String getAsSql() {
        return this.field.getSqlName() + " " + this.comparison.getAsSql() + " " + BulkUpdate.escapeStringForSql(this.value);
    }

    public QueryField getField() {
        return this.field;
    }

    public Comparison getComparison() {
        return this.comparison;
    }

    public String getValue() {
        return this.value;
    }
}
