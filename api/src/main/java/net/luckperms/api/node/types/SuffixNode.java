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
 * A sub-type of {@link Node} used to store suffix assignments.
 */
public interface SuffixNode extends ChatMetaNode<SuffixNode, SuffixNode.Builder> {

    @Override
    default @NonNull NodeType<SuffixNode> getType() {
        return NodeType.SUFFIX;
    }

    /**
     * Creates a {@link SuffixNode} builder.
     *
     * @return the builder
     */
    static @NonNull Builder builder() {
        return LuckPermsProvider.get().getNodeBuilderRegistry().forSuffix();
    }

    /**
     * Creates a {@link SuffixNode} builder.
     *
     * @param suffix the suffix to set
     * @param priority the priority to set
     * @return the builder
     */
    static @NonNull Builder builder(@NonNull String suffix, int priority) {
        return builder().suffix(suffix).priority(priority);
    }

    /**
     * A {@link SuffixNode} builder.
     */
    interface Builder extends ChatMetaNode.Builder<SuffixNode, Builder> {

        /**
         * Sets the suffix.
         *
         * @param suffix the suffix
         * @return the builder
         */
        @NonNull Builder suffix(@NonNull String suffix);

    }

}
