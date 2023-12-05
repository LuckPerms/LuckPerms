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

package me.lucko.luckperms.common.actionlog.filter;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.sql.FilterSqlBuilder;
import net.luckperms.api.actionlog.Action;

import java.util.UUID;

public class ActionFilterSqlBuilder extends FilterSqlBuilder<Action> {

    @Override
    public void visitFieldName(FilterField<Action, ?> field) {
        if (field == ActionFields.SOURCE_UNIQUE_ID) {
            this.builder.append("actor_uuid");
        } else if (field == ActionFields.SOURCE_NAME) {
            this.builder.append("actor_name");
        } else if (field == ActionFields.TARGET_TYPE) {
            this.builder.append("type");
        } else if (field == ActionFields.TARGET_UNIQUE_ID) {
            this.builder.append("acted_uuid");
        } else if (field == ActionFields.TARGET_NAME) {
            this.builder.append("acted_name");
        } else if (field == ActionFields.DESCRIPTION) {
            this.builder.append("action");
        } else {
            throw new AssertionError(field);
        }
    }

    @Override
    public void visitConstraintValue(Object value) {
        if (value instanceof String) {
            this.builder.variable(((String) value));
        } else if (value instanceof UUID) {
            this.builder.variable(value.toString());
        } else if (value instanceof Action.Target.Type) {
            this.builder.variable(LoggedAction.getTypeString((Action.Target.Type) value));
        } else {
            throw new IllegalArgumentException("Don't know how to write value with type: " + value.getClass().getName());
        }
    }

}
