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

/**
 * A sub-type of {@link Node} used to mark the display name of the node's holder.
 */
public interface DisplayNameNode extends ScopedNode<DisplayNameNode, DisplayNameNode.Builder> {

    @Override
    default @NonNull NodeType<DisplayNameNode> getType() {
        return NodeType.DISPLAY_NAME;
    }

    /**
     * Gets the display name.
     *
     * @return the display name
     */
    @NonNull String getDisplayName();

    /**
     * Creates a {@link DisplayNameNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forDisplayName();
    }

    /**
     * Creates a {@link DisplayNameNode} builder.
     *
     * @param displayName the display name to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String displayName) {
        return builder().displayName(displayName);
    }

    /**
     * A {@link DisplayNameNode} builder.
     */
    interface Builder extends NodeBuilder<DisplayNameNode, Builder> {

        /**
         * Sets the display name.
         *
         * @param displayName the display name
         * @return the builder
         * @throws IllegalArgumentException if {@code displayName} is empty
         */
        @NonNull Builder displayName(@NonNull String displayName);

    }

}
