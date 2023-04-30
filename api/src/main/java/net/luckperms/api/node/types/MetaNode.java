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
 * A sub-type of {@link Node} used to store meta assignments.
 */
public interface MetaNode extends ScopedNode<MetaNode, MetaNode.Builder> {

    @Override
    default @NonNull NodeType<MetaNode> getType() {
        return NodeType.META;
    }

    /**
     * Gets the meta key.
     *
     * @return the meta key
     */
    @NonNull String getMetaKey();

    /**
     * Gets the meta value.
     *
     * @return the meta value
     */
    @NonNull String getMetaValue();

    /**
     * Creates a {@link MetaNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forMeta();
    }

    /**
     * Creates a {@link MetaNode} builder.
     *
     * @param key the meta key to set
     * @param value the meta value to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String key, @NonNull String value) {
        return builder().key(key).value(value);
    }

    /**
     * A {@link MetaNode} builder.
     */
    interface Builder extends NodeBuilder<MetaNode, Builder> {

        /**
         * Sets the meta key.
         *
         * @param key the meta key
         * @return the builder
         * @throws IllegalArgumentException if {@code key} is empty
         */
        @NonNull Builder key(@NonNull String key);

        /**
         * Sets the meta value.
         *
         * @param value the meta value
         * @return the builder
         */
        @NonNull Builder value(@NonNull String value);

    }

}
