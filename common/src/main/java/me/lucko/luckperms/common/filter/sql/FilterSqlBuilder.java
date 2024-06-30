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

package me.lucko.luckperms.common.filter.sql;

import me.lucko.luckperms.common.filter.Filter;
import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.FilterList;

import java.util.List;

public abstract class FilterSqlBuilder<T> extends ConstraintSqlBuilder {

    public abstract void visitFieldName(FilterField<T, ?> field);

    public void visit(Filter<T, ?> filter) {
        //        'field = value'
        //       'field != value'
        //     'field LIKE value'
        // 'field NOT LIKE value'

        visitFieldName(filter.field());
        this.builder.append(' ');
        visit(filter.constraint());
    }

    public void visit(FilterList.LogicalOperator combineOperator, List<? extends Filter<T, ?>> filters) {
        if (filters.isEmpty()) {
            return;
        }

        String combineString;
        switch (combineOperator) {
            case AND:
                combineString = "AND ";
                break;
            case OR:
                combineString = "OR ";
                break;
            default:
                throw new AssertionError(combineOperator);
        }

        this.builder.append(" WHERE");
        for (int i = 0; i < filters.size(); i++) {
            Filter<T, ?> filter = filters.get(i);
            this.builder.append(" ");
            if (i != 0) {
                this.builder.append(combineString);
            }
            visit(filter);
        }
    }

    public void visit(FilterList<T> filters) {
        visit(filters.operator(), filters);
    }

}
