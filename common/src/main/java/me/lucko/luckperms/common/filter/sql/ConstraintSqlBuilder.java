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

import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.filter.Constraint;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.storage.implementation.sql.builder.AbstractSqlBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConstraintSqlBuilder extends AbstractSqlBuilder {

    public void visitConstraintValue(Object value) {
        if (value instanceof String) {
            this.builder.variable(((String) value));
        } else {
            throw new IllegalArgumentException("Don't know how to write value with type: " + value.getClass().getName());
        }
    }

    public void visit(Constraint<?> constraint) {
        //        '= value'
        //       '!= value'
        //     'LIKE value'
        // 'NOT LIKE value'

        visit(constraint.comparison());
        this.builder.append(' ');
        visitConstraintValue(constraint.value());
    }

    public void visit(Comparison comparison) {
        switch (comparison) {
            case EQUAL:
                this.builder.append("=");
                break;
            case NOT_EQUAL:
                this.builder.append("!=");
                break;
            case SIMILAR:
                this.builder.append("LIKE");
                break;
            case NOT_SIMILAR:
                this.builder.append("NOT LIKE");
                break;
            default:
                throw new AssertionError(comparison);
        }
    }

    public void visit(@Nullable PageParameters params) {
        if (params == null) {
            return;
        }

        int pageSize = params.pageSize();
        int pageNumber = params.pageNumber();
        this.builder.append(" LIMIT " + pageSize + " OFFSET " + (pageNumber - 1) * pageSize);
    }

}
