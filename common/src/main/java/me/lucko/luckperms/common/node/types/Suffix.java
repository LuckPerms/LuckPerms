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
import me.lucko.luckperms.api.node.types.SuffixNode;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;

public class Suffix extends AbstractNode<SuffixNode, SuffixNode.Builder> implements SuffixNode {
    private final String suffix;
    private final int priority;

    public Suffix(String suffix, int priority, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
        super(NodeFactory.suffixNode(priority, suffix), value, expireAt, contexts, metadata);
        this.suffix = suffix;
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public @NonNull String getMetaValue() {
        return this.suffix;
    }

    @Override
    public @NonNull ChatMetaType getType() {
        return ChatMetaType.SUFFIX;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.suffix, this.priority, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static final class Builder extends AbstractNodeBuilder<SuffixNode, SuffixNode.Builder> implements SuffixNode.Builder {
        private String suffix;
        private Integer priority;

        public Builder() {
            this.suffix = null;
            this.priority = null;
        }

        public Builder(String suffix, int priority, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
            super(value, expireAt, context, metadata);
            this.suffix = suffix;
            this.priority = priority;
        }

        @Override
        public @NonNull Builder suffix(@NonNull String suffix) {
            this.suffix = suffix;
            return this;
        }

        @Override
        public @NonNull Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public @NonNull Suffix build() {
            Objects.requireNonNull(this.suffix, "suffix");
            Objects.requireNonNull(this.priority, "priority");
            return new Suffix(this.suffix, this.priority, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
