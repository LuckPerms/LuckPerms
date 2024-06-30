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
import net.luckperms.api.actionlog.Action;

import java.util.UUID;

public final class ActionFields {

    public static final FilterField<Action, UUID> SOURCE_UNIQUE_ID = FilterField.named(
            "SOURCE_UNIQUE_ID",
            action -> action.getSource().getUniqueId()
    );
    public static final FilterField<Action, String> SOURCE_NAME = FilterField.named(
            "SOURCE_NAME",
            action -> action.getSource().getName()
    );
    public static final FilterField<Action, Action.Target.Type> TARGET_TYPE = FilterField.named(
            "TARGET_TYPE",
            action -> action.getTarget().getType()
    );
    public static final FilterField<Action, UUID> TARGET_UNIQUE_ID = FilterField.named(
            "TARGET_UNIQUE_ID",
            action -> action.getTarget().getUniqueId().orElse(null)
    );
    public static final FilterField<Action, String> TARGET_NAME = FilterField.named(
            "TARGET_NAME",
            action -> action.getTarget().getName()
    );
    public static final FilterField<Action, String> DESCRIPTION = FilterField.named(
            "DESCRIPTION",
            action -> action.getDescription()
    );

}