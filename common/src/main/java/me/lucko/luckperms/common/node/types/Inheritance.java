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

import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.InheritanceNode;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Objects;

public class Inheritance extends AbstractNode<InheritanceNode, InheritanceNode.Builder> implements InheritanceNode {
    private static final String NODE_KEY = "group";
    private static final String NODE_MARKER = NODE_KEY + ".";

    public static String key(String groupName) {
        return NODE_MARKER + groupName.toLowerCase();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String groupName) {
        return builder().group(groupName);
    }

    private final String groupName;

    public Inheritance(String groupName, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        super(key(groupName), value, expireAt, contexts, metadata);
        this.groupName = groupName.toLowerCase();
    }

    @Override
    public @NonNull String getGroupName() {
        return this.groupName;
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.groupName, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static @Nullable Builder parse(String key) {
        key = key.toLowerCase();
        if (!key.startsWith(NODE_MARKER)) {
            return null;
        }

        return builder()
                .group(key.substring(NODE_MARKER.length()));
    }

    public static final class Builder extends AbstractNodeBuilder<InheritanceNode, InheritanceNode.Builder> implements InheritanceNode.Builder {
        private String groupName;

        private Builder() {
            this.groupName = null;
        }

        public Builder(String groupName, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
            super(value, expireAt, context, metadata);
            this.groupName = groupName;
        }

        @Override
        public @NonNull Builder group(@NonNull String group) {
            this.groupName = Objects.requireNonNull(group, "group");
            return this;
        }

        @Override
        public @NonNull Builder group(@NonNull Group group) {
            this.groupName = Objects.requireNonNull(group, "group").getName();
            return this;
        }

        @Override
        public @NonNull Inheritance build() {
            Objects.requireNonNull(this.groupName, "groupName");
            return new Inheritance(this.groupName, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
