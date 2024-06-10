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

import me.lucko.luckperms.common.filter.FilterField;
import me.lucko.luckperms.common.filter.mongo.FilterMongoBuilder;
import net.luckperms.api.actionlog.Action;

import java.util.UUID;

public final class ActionFilterMongoBuilder extends FilterMongoBuilder<Action> {
    public static final ActionFilterMongoBuilder INSTANCE = new ActionFilterMongoBuilder();

    private ActionFilterMongoBuilder() {

    }

    @Override
    public String mapFieldName(FilterField<Action, ?> field) {
        if (field == ActionFields.SOURCE_UNIQUE_ID) {
            return "source.uniqueId";
        } else if (field == ActionFields.SOURCE_NAME) {
            return "source.name";
        } else if (field == ActionFields.TARGET_TYPE) {
            return "target.type";
        } else if (field == ActionFields.TARGET_UNIQUE_ID) {
            return "target.uniqueId";
        } else if (field == ActionFields.TARGET_NAME) {
            return "target.name";
        } else if (field == ActionFields.DESCRIPTION) {
            return "description";
        }
        throw new AssertionError(field);
    }

    @Override
    public Object mapConstraintValue(Object value) {
        if (value instanceof String | value instanceof UUID) {
            return value;
        } else if (value instanceof Action.Target.Type) {
            return ((Action.Target.Type) value).name();
        } else {
            throw new IllegalArgumentException("Don't know how to map value with type: " + value.getClass().getName());
        }
    }

}
