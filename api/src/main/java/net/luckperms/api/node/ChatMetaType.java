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

package net.luckperms.api.node;

import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;

/**
 * Represents a type of chat meta
 */
public enum ChatMetaType {

    /**
     * Represents a prefix
     */
    PREFIX(NodeType.PREFIX) {
        @Override
        public ChatMetaNode.@NonNull Builder<?, ?> builder() {
            return PrefixNode.builder();
        }

        @Override
        public ChatMetaNode.@NonNull Builder<?, ?> builder(@NonNull String prefix, int priority) {
            return PrefixNode.builder(prefix, priority);
        }
    },

    /**
     * Represents a suffix
     */
    SUFFIX(NodeType.SUFFIX) {
        @Override
        public ChatMetaNode.@NonNull Builder<?, ?> builder() {
            return SuffixNode.builder();
        }

        @Override
        public ChatMetaNode.@NonNull Builder<?, ?> builder(@NonNull String suffix, int priority) {
            return SuffixNode.builder(suffix, priority);
        }
    };

    private final String name;
    private final NodeType<? extends ChatMetaNode<?, ?>> nodeType;

    ChatMetaType(NodeType<? extends ChatMetaNode<?, ?>> nodeType) {
        this.name = nodeType.name().toLowerCase(Locale.ROOT);
        this.nodeType = nodeType;
    }

    /**
     * Gets the {@link NodeType} for the {@link ChatMetaType}.
     *
     * @return the node type
     */
    public @NonNull NodeType<? extends ChatMetaNode<?, ?>> nodeType() {
        return this.nodeType;
    }

    /**
     * Creates a {@link ChatMetaNode.Builder} for the {@link ChatMetaType}.
     *
     * @return a builder
     */
    public abstract ChatMetaNode.@NonNull Builder<?, ?> builder();

    /**
     * Creates a {@link ChatMetaNode.Builder} for the {@link ChatMetaType}.
     *
     * @param value the value to set
     * @param priority the priority to set
     * @return a builder
     */
    public abstract ChatMetaNode.@NonNull Builder<?, ?> builder(@NonNull String value, int priority);

    @Override
    public String toString() {
        return this.name;
    }

}
