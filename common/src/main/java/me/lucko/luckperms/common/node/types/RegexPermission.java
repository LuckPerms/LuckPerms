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
import me.lucko.luckperms.api.node.types.RegexPermissionNode;
import me.lucko.luckperms.common.cache.Cache;
import me.lucko.luckperms.common.cache.PatternCache;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.AbstractNodeBuilder;
import me.lucko.luckperms.common.node.factory.NodeFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class RegexPermission extends AbstractNode<RegexPermissionNode, RegexPermissionNode.Builder> implements RegexPermissionNode {
    private final String pattern;

    private final Cache<PatternCache.CachedPattern> cache = new Cache<PatternCache.CachedPattern>() {
        @Override
        protected PatternCache.@NonNull CachedPattern supply() {
            return PatternCache.lookup(RegexPermission.this.pattern);
        }
    };

    public RegexPermission(String pattern, boolean value, long expireAt, ImmutableContextSet contexts, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
        super(NodeFactory.regexNode(pattern), value, expireAt, contexts, metadata);
        this.pattern = pattern;
    }

    @Override
    public @NonNull String getPatternString() {
        return this.pattern;
    }

    @Override
    public @NonNull Optional<Pattern> getPattern() {
        return Optional.ofNullable(this.cache.get().getPattern());
    }

    @Override
    public @NonNull Builder toBuilder() {
        return new Builder(this.pattern, this.value, this.expireAt, this.contexts, this.metadata);
    }

    public static final class Builder extends AbstractNodeBuilder<RegexPermissionNode, RegexPermissionNode.Builder> implements RegexPermissionNode.Builder {
        private String pattern;

        public Builder() {
            this.pattern = null;
        }

        public Builder(String pattern, boolean value, long expireAt, ImmutableContextSet context, Map<NodeMetadataKey<?>, NodeMetadata> metadata) {
            super(value, expireAt, context, metadata);
            this.pattern = pattern;
        }

        @Override
        public @NonNull Builder pattern(@NonNull String pattern) {
            this.pattern = Objects.requireNonNull(pattern, "pattern");
            return this;
        }

        @Override
        public @NonNull Builder pattern(@NonNull Pattern pattern) {
            this.pattern = Objects.requireNonNull(pattern, "pattern").pattern();
            return this;
        }

        @Override
        public @NonNull RegexPermission build() {
            Objects.requireNonNull(this.pattern, "pattern");
            return new RegexPermission(this.pattern, this.value, this.expireAt, this.context.build(), this.metadata);
        }
    }
}
