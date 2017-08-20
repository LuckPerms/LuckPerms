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

import lombok.AllArgsConstructor;
import lombok.Getter;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.comparisons.ComparisonType;
import me.lucko.luckperms.common.node.NodeModel;

/**
 * Represents a query constraint
 */
@Getter
@AllArgsConstructor(staticName = "of")
public class Constraint {

    // the field this constraint is comparing against
    private final QueryField field;

    // the comparison type being used in this constraint
    private final ComparisonType comparisonType;

    // the expression being compared against
    private final String value;

    /**
     * Returns if the given node satisfies this constraint
     *
     * @param node the node
     * @return true if satisfied
     */
    public boolean isSatisfiedBy(NodeModel node) {
        switch (field) {
            case PERMISSION:
                return comparisonType.getComparison().matches(node.getPermission(), value);
            case SERVER:
                return comparisonType.getComparison().matches(node.getServer(), value);
            case WORLD:
                return comparisonType.getComparison().matches(node.getWorld(), value);
            default:
                throw new RuntimeException();
        }
    }

    public String getAsSql() {
        switch (comparisonType) {
            case EQUAL:
                return field.getSqlName() + " = " + BulkUpdate.escapeStringForSql(value);
            case NOT_EQUAL:
                return field.getSqlName() + " != " + BulkUpdate.escapeStringForSql(value);
            case SIMILAR:
                return field.getSqlName() + " LIKE " + BulkUpdate.escapeStringForSql(value);
            case NOT_SIMILAR:
                return field.getSqlName() + " NOT LIKE " + BulkUpdate.escapeStringForSql(value);
            default:
                throw new RuntimeException();
        }
    }

}
