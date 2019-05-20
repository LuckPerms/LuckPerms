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
import me.lucko.luckperms.api.node.metadata.NodeMetadata;
import me.lucko.luckperms.api.node.metadata.NodeMetadataKey;
import me.lucko.luckperms.api.node.types.DisplayNameNode;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;

public class DisplayName extends AbstractNode<DisplayNameNode, DisplayNameNode.Builder> implements DisplayNameNode {
    private final String displayName;

    public DisplayName(String displayName, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
        super(NodeFactory.displayName(displayName), value, expireAt, contexts, metadata);
        this.displayName = displayName;
    }

    @Override
    public @NonNull String getDisplayName() {
        return this.displayName;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.displayName, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static final class Builder extends AbstractNodeBuilder<DisplayNameNode, DisplayNameNode.Builder> implements DisplayNameNode.Builder {
        private String displayName;

        public Builder() {
            this.displayName = null;
        }

        public Builder(String displayName, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
            super(value, expireAt, context, metadata);
            this.displayName = displayName;
        }

        @Override
        public @NonNull Builder displayName(@NonNull String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public @NonNull DisplayName build() {
            Objects.requireNonNull(this.displayName, "displayName");
            return new DisplayName(this.displayName, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
