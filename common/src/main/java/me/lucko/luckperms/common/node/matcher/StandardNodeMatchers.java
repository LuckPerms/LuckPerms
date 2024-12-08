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

package me.lucko.luckperms.common.node.matcher;

import me.lucko.luckperms.common.filter.Comparison;
import me.lucko.luckperms.common.node.AbstractNode;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.RegexPermission;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.node.types.Weight;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StandardNodeMatchers {
    private StandardNodeMatchers() {}

    public static ConstraintNodeMatcher<Node> key(String value, Comparison comparison) {
        return new Generic(comparison, value);
    }

    public static ConstraintNodeMatcher<Node> key(String key) {
        return new Generic(Comparison.EQUAL, key);
    }

    public static <T extends Node> ConstraintNodeMatcher<T> key(T node) {
        return new NodeEquals<>(node, NodeEqualityPredicate.ONLY_KEY);
    }

    public static ConstraintNodeMatcher<Node> keyStartsWith(String startsWith) {
        return new Generic(Comparison.SIMILAR, startsWith + Comparison.WILDCARD);
    }

    public static <T extends Node> ConstraintNodeMatcher<T> equals(T other, NodeEqualityPredicate equalityPredicate) {
        return new NodeEquals<>(other, equalityPredicate);
    }

    public static ConstraintNodeMatcher<MetaNode> metaKey(String metaKey) {
        return new MetaKeyEquals(metaKey);
    }

    public static <T extends Node> ConstraintNodeMatcher<T> type(NodeType<? extends T> type) {
        return new TypeEquals<>(type);
    }

    public static class Generic extends ConstraintNodeMatcher<Node> {
        Generic(Comparison comparison, String value) {
            super(comparison, value);
        }

        @Override
        public @Nullable Node filterConstraintMatch(@NonNull Node node) {
            return node;
        }
    }

    public static final class NodeEquals<T extends Node> extends ConstraintNodeMatcher<T> {
        private final T node;
        private final NodeEqualityPredicate equalityPredicate;

        NodeEquals(T node, NodeEqualityPredicate equalityPredicate) {
            super(Comparison.EQUAL, node.getKey());
            this.node = node;
            this.equalityPredicate = equalityPredicate;
        }

        public T getNode() {
            return this.node;
        }

        public NodeEqualityPredicate getEqualityPredicate() {
            return this.equalityPredicate;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable T filterConstraintMatch(@NonNull Node node) {
            if (this.equalityPredicate == NodeEqualityPredicate.ONLY_KEY || this.node.equals(node, this.equalityPredicate)) {
                return (T) node;
            }
            return null;
        }
    }

    public static final class MetaKeyEquals extends ConstraintNodeMatcher<MetaNode> {
        private final String metaKey;

        MetaKeyEquals(String metaKey) {
            super(Comparison.SIMILAR, Meta.key(metaKey, Comparison.WILDCARD));
            this.metaKey = metaKey;
        }

        public String getMetaKey() {
            return this.metaKey;
        }

        @Override
        public @Nullable MetaNode filterConstraintMatch(@NonNull Node node) {
            return NodeType.META.tryCast(node).orElse(null);
        }
    }

    public static final class TypeEquals<T extends Node> extends ConstraintNodeMatcher<T> {
        private final NodeType<? extends T> type;

        TypeEquals(NodeType<? extends T> type) {
            super(Comparison.SIMILAR, getSimilarToComparisonValue(type));
            this.type = type;
        }

        public NodeType<? extends T> getType() {
            return this.type;
        }

        @Override
        public @Nullable T filterConstraintMatch(@NonNull Node node) {
            return this.type.tryCast(node).orElse(null);
        }

        private static String getSimilarToComparisonValue(NodeType<?> type) {
            if (type == NodeType.REGEX_PERMISSION) {
                return RegexPermission.key(Comparison.WILDCARD);
            } else if (type == NodeType.INHERITANCE) {
                return Inheritance.key(Comparison.WILDCARD);
            } else if (type == NodeType.PREFIX) {
                return Prefix.NODE_MARKER + Comparison.WILDCARD + AbstractNode.NODE_SEPARATOR + Comparison.WILDCARD;
            } else if (type == NodeType.SUFFIX) {
                return Suffix.NODE_MARKER + Comparison.WILDCARD + AbstractNode.NODE_SEPARATOR + Comparison.WILDCARD;
            } else if (type == NodeType.META) {
                return Meta.key(Comparison.WILDCARD, Comparison.WILDCARD);
            } else if (type == NodeType.WEIGHT) {
                return Weight.NODE_MARKER + Comparison.WILDCARD;
            } else if (type == NodeType.DISPLAY_NAME) {
                return DisplayName.key(Comparison.WILDCARD);
            }

            throw new IllegalArgumentException("Unable to create a NodeMatcher for NodeType " + type.name());
        }
    }

}
