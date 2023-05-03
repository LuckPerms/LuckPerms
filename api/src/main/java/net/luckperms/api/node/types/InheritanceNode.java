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
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.ScopedNode;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A sub-type of {@link Node} used to mark that the holder of the node should inherit
 * from another group.
 */
public interface InheritanceNode extends ScopedNode<InheritanceNode, InheritanceNode.Builder> {

    @Override
    default @NonNull NodeType<InheritanceNode> getType() {
        return NodeType.INHERITANCE;
    }

    /**
     * Gets the name of the group to be inherited.
     *
     * <p>This is no guarantee that this group exists.</p>
     *
     * @return the name of the group
     */
    @NonNull String getGroupName();

    /**
     * Creates a {@link InheritanceNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forInheritance();
    }

    /**
     * Creates a {@link InheritanceNode} builder.
     *
     * @param group the group to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String group) {
        return builder().group(group);
    }

    /**
     * Sets the name of group to inherit.
     *
     * @param group the group name
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull Group group) {
        return builder().group(group);
    }

    /**
     * A {@link InheritanceNode} builder.
     */
    interface Builder extends NodeBuilder<InheritanceNode, Builder> {

        /**
         * Sets the name of the group to inherit.
         *
         * <p>Note that only one of this method and {@link #group(Group)}
         * need be called.</p>
         *
         * @param group the group name
         * @return the builder
         * @throws IllegalArgumentException if {@code group} is not a valid group name
         */
        @NonNull Builder group(@NonNull String group);

        /**
         * Sets the group to inherit.
         *
         * <p>Note that only one of this method and {@link #group(String)}
         * need be called.</p>
         *
         * @param group the group
         * @return the builder
         */
        @NonNull Builder group(@NonNull Group group);

    }

}
