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
import net.luckperms.api.node.NodeType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A sub-type of {@link Node} used to store prefix assignments.
 */
public interface PrefixNode extends ChatMetaNode<PrefixNode, PrefixNode.Builder> {

    @Override
    default @NonNull NodeType<PrefixNode> getType() {
        return NodeType.PREFIX;
    }

    /**
     * Creates a {@link PrefixNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forPrefix();
    }

    /**
     * Creates a {@link PrefixNode} builder.
     *
     * @param prefix the prefix to set
     * @param priority the priority to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String prefix, int priority) {
        return builder().prefix(prefix).priority(priority);
    }

    /**
     * A {@link PrefixNode} builder.
     */
    interface Builder extends ChatMetaNode.Builder<PrefixNode, Builder> {

        /**
         * Sets the prefix.
         *
         * @param prefix the prefix
         * @return the builder
         */
        @NonNull Builder prefix(@NonNull String prefix);

    }

}
