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

package net.luckperms.api.event.node;

import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Set;

/**
 * Called when a node is added to/removed from a user/group
 */
public interface NodeMutateEvent extends LuckPermsEvent {

    /**
     * Gets the target of the event
     *
     * @return the event target
     */
    @Param(0)
    @NonNull PermissionHolder getTarget();

    /**
     * Gets the data type that was mutated.
     *
     * @return the data type
     */
    @Param(1)
    @NonNull DataType getDataType();

    /**
     * Gets an immutable copy of the holders data before the change
     *
     * @return the data before the change
     */
    @NonNull Set<Node> getDataBefore();

    /**
     * Gets an immutable copy of the holders data after the change
     *
     * @return the data after the change
     */
    @Param(2)
    @NonNull Set<Node> getDataAfter();

    /**
     * Gets whether the target of this event is a {@link User}
     *
     * <p>This is equivalent to checking if getTarget() instanceof User</p>
     *
     * @return if the event is targeting a user
     */
    default boolean isUser() {
        return getTarget() instanceof User;
    }

    /**
     * Gets whether the target of this event is a {@link Group}
     *
     * <p>This is equivalent to checking if getTarget() instanceof Group</p>
     *
     * @return if the event is targeting a group
     */
    default boolean isGroup() {
        return getTarget() instanceof Group;
    }

}
