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

import me.lucko.luckperms.common.bulkupdate.action.BulkUpdateAction;
import me.lucko.luckperms.common.bulkupdate.action.DeleteAction;
import me.lucko.luckperms.common.bulkupdate.action.UpdateAction;
import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.sql.FilterSqlBuilder;
import net.luckperms.api.node.Node;

public class BulkUpdateSqlBuilder extends FilterSqlBuilder<Node> {

    public void visit(BulkUpdate update) {
        visit(update.getAction());
        visit(update.getFilters());
    }

    public void visit(BulkUpdateAction action) {
        if (action instanceof UpdateAction) {
            visit(((UpdateAction) action));
        } else if (action instanceof DeleteAction) {
            visit(((DeleteAction) action));
        } else {
            throw new UnsupportedOperationException(action.getClass().getName());
        }
    }

    public void visit(UpdateAction action) {
        this.builder.append("UPDATE {table} SET ");
        visitFieldName(action.getField());
        this.builder.append("=");
        this.builder.variable(action.getNewValue());
    }

    public void visit(DeleteAction action) {
        this.builder.append("DELETE FROM {table}");
    }

    @Override
    public void visitFieldName(FilterField<Node, ?> field) {
        if (field == BulkUpdateField.PERMISSION) {
            this.builder.append("permission");
        } else if (field == BulkUpdateField.SERVER) {
            this.builder.append("server");
        } else if (field == BulkUpdateField.WORLD) {
            this.builder.append("world");
        } else {
            throw new AssertionError(field);
        }
    }

}
