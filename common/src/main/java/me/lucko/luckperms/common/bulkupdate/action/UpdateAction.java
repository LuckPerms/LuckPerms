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

package me.lucko.luckperms.common.bulkupdate.action;

import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.constraint.QueryField;
import me.lucko.luckperms.common.node.NodeModel;

public class UpdateAction implements Action {

    public static UpdateAction of(QueryField field, String value) {
        return new UpdateAction(field, value);
    }

    // the field we're updating
    private final QueryField field;

    // the new value of the field
    private final String value;

    private UpdateAction(QueryField field, String value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public String getName() {
        return "update";
    }

    @Override
    public NodeModel apply(NodeModel from) {
        switch (this.field) {
            case PERMISSION:
                return from.setPermission(this.value);
            case SERVER:
                return from.setServer(this.value);
            case WORLD:
                return from.setWorld(this.value);
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public String getAsSql() {
        return "UPDATE {table} SET " + this.field.getSqlName() + "=" + BulkUpdate.escapeStringForSql(this.value);
    }
}
