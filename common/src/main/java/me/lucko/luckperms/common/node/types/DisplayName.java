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
import net.luckperms.api.node.metadata.NodeMetadataKey;
import net.luckperms.api.node.types.DisplayNameNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DisplayName extends AbstractNode<DisplayNameNode, DisplayNameNode.Builder> implements DisplayNameNode {
    private static final String NODE_KEY = "displayname";
    private static final String NODE_MARKER = NODE_KEY + ".";

    public static String key(String displayName) {
        return NODE_MARKER + displayName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String displayName) {
        return builder().displayName(displayName);
    }

    private final String displayName;

    public DisplayName(String displayName, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, Object> metadata) {
        super(key(displayName), value, expireAt, contexts, metadata);
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

    public static @Nullable Builder parse(String key) {
        if (!key.toLowerCase(Locale.ROOT).startsWith(NODE_MARKER)) {
            return null;
        }

        return builder()
                .displayName(key.substring(NODE_MARKER.length()));
    }

    public static final class Builder extends AbstractNodeBuilder<DisplayNameNode, DisplayNameNode.Builder> implements DisplayNameNode.Builder {
        private String displayName;

        private Builder() {
            this.displayName = null;
        }

        public Builder(String displayName, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, Object> metadata) {
            super(value, expireAt, context, metadata);
            this.displayName = displayName;
        }

        @Override
        public @NonNull Builder displayName(@NonNull String displayName) {
            Objects.requireNonNull(displayName, "displayName");
            if (displayName.isEmpty()) {
                throw new IllegalArgumentException("display name is empty");
            }
            this.displayName = displayName;
            return this;
        }

        @Override
        public @NonNull DisplayName build() {
            ensureDefined(this.displayName, "display name");
            return new DisplayName(this.displayName, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
