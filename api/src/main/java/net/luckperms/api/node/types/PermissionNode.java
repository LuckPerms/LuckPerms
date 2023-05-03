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

package net.luckperms.api.node.types;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.ScopedNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.OptionalInt;

/**
 * A sub-type of {@link Node} representing basic permissions.
 */
public interface PermissionNode extends ScopedNode<PermissionNode, PermissionNode.Builder> {

    @Override
    default @NonNull NodeType<PermissionNode> getType() {
        return NodeType.PERMISSION;
    }

    /**
     * Gets the permission string this node encapsulates.
     *
     * <p>The exact value of this string may vary for nodes which aren't regular
     * permission settings.</p>
     *
     * @return the actual permission node
     */
    @NonNull String getPermission();

    /**
     * Gets if this node is a wildcard permission.
     *
     * @return true if this node is a wildcard permission
     */
    boolean isWildcard();

    /**
     * Gets the level of this wildcard.
     *
     * <p>The node <code>luckperms.*</code> has a wildcard level of 1.</p>
     * <p>The node <code>luckperms.user.permission.*</code> has a wildcard level of 3.</p>
     *
     * <p>Nodes with a higher wildcard level are more specific and have priority over
     * less specific nodes (nodes with a lower wildcard level).</p>
     *
     * @return the wildcard level
     */
    @NonNull OptionalInt getWildcardLevel();

    /**
     * Creates a {@link PermissionNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forPermission();
    }

    /**
     * Creates a {@link PermissionNode} builder.
     *
     * @param permission the permission to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String permission) {
        return builder().permission(permission);
    }

    /**
     * A {@link PermissionNode} builder.
     */
    interface Builder extends NodeBuilder<PermissionNode, Builder> {

        /**
         * Sets the permission.
         *
         * @param permission the permission
         * @return the builder
         * @throws IllegalArgumentException if {@code permission} is empty
         */
        @NonNull Builder permission(@NonNull String permission);

    }

}
