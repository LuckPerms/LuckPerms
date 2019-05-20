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

package me.lucko.luckperms.common.node.types;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.node.ChatMetaType;
import me.lucko.luckperms.api.node.metadata.NodeMetadata;
import me.lucko.luckperms.api.node.metadata.NodeMetadataKey;
import me.lucko.luckperms.api.node.types.PrefixNode;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;

public class Prefix extends AbstractNode<PrefixNode, PrefixNode.Builder> implements PrefixNode {
    private final String prefix;
    private final int priority;

    public Prefix(String prefix, int priority, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
        super(NodeFactory.prefixNode(priority, prefix), value, expireAt, contexts, metadata);
        this.prefix = prefix;
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public @NonNull String getMetaValue() {
        return this.prefix;
    }

    @Override
    public @NonNull ChatMetaType getType() {
        return ChatMetaType.PREFIX;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.prefix, this.priority, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static final class Builder extends AbstractNodeBuilder<PrefixNode, PrefixNode.Builder> implements PrefixNode.Builder {
        private String prefix;
        private Integer priority;

        public Builder() {
            this.prefix = null;
            this.priority = null;
        }

        public Builder(String prefix, int priority, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
            super(value, expireAt, context, metadata);
            this.prefix = prefix;
            this.priority = priority;
        }

        @Override
        public @NonNull Builder prefix(@NonNull String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public @NonNull Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public @NonNull Prefix build() {
            Objects.requireNonNull(this.prefix, "prefix");
            Objects.requireNonNull(this.priority, "priority");
            return new Prefix(this.prefix, this.priority, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
