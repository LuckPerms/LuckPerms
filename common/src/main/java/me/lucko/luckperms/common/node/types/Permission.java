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

import me.lucko.luckperms.common.calculator.processor.WildcardProcessor;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.PermissionNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public class Permission extends AbstractNode<PermissionNode, PermissionNode.Builder> implements PermissionNode {
    public static Builder builder() {
        return new Builder();
    }

    private final int wildcardLevel;

    public Permission(String permission, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        super(permission, value, expireAt, contexts, metadata);
        this.wildcardLevel = WildcardProcessor.isWildcardPermission(permission) ? permission.chars().filter(num -> num == NODE_SEPARATOR).sum() : -1;
    }

    @Override
    public @NonNull String getPermission() {
        return getKey();
    }

    @Override
    public boolean isWildcard() {
        return this.wildcardLevel != -1;
    }

    @Override
    public @NonNull OptionalInt getWildcardLevel() {
        return isWildcard() ? OptionalInt.of(this.wildcardLevel) : OptionalInt.empty();
    }

    @Override
    public PermissionNode.@NonNull Builder toBuilder() {
        return new Builder(this.key, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static final class Builder extends AbstractNodeBuilder<PermissionNode, PermissionNode.Builder> implements PermissionNode.Builder {
        private String permission;

        private Builder() {
            this.permission = null;
        }

        public Builder(String permission, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
            super(value, expireAt, context, metadata);
            this.permission = permission;
        }

        @Override
        public @NonNull Builder permission(@NonNull String permission) {
            Objects.requireNonNull(permission, "permission");
            if (permission.isEmpty()) {
                throw new IllegalArgumentException("permission string is empty");
            }
            this.permission = permission;
            return this;
        }

        @Override
        public @NonNull Permission build() {
            ensureDefined(this.permission, "permission");

            NodeBuilder<?, ?> testBuilder = NodeBuilders.determineMostApplicable(this.permission);
            if (!(testBuilder instanceof Builder)) {
                throw new IllegalArgumentException("Attempting to build non-permission node with PermissionNode.Builder. permission = '" + this.permission + "', correct builder type = " + testBuilder.getClass().getName());
            }

            return new Permission(this.permission, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }

}
