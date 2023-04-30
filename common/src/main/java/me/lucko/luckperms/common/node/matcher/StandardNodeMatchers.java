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

import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import me.lucko.luckperms.common.bulkupdate.comparison.StandardComparison;
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

    public static ConstraintNodeMatcher<Node> of(Constraint constraint) {
        return new Generic(constraint);
    }

    public static ConstraintNodeMatcher<Node> key(String key) {
        return new Generic(Constraint.of(StandardComparison.EQUAL, key));
    }

    public static <T extends Node> ConstraintNodeMatcher<T> key(T node) {
        return new NodeEquals<>(node, NodeEqualityPredicate.ONLY_KEY);
    }

    public static ConstraintNodeMatcher<Node> keyStartsWith(String startsWith) {
        return new Generic(Constraint.of(StandardComparison.SIMILAR, startsWith + StandardComparison.WILDCARD));
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

    private static class Generic extends ConstraintNodeMatcher<Node> {
        Generic(Constraint constraint) {
            super(constraint);
        }

        @Override
        public @Nullable Node filterConstraintMatch(@NonNull Node node) {
            return node;
        }
    }

    private static final class NodeEquals<T extends Node> extends ConstraintNodeMatcher<T> {
        private final T node;
        private final NodeEqualityPredicate equalityPredicate;

        NodeEquals(T node, NodeEqualityPredicate equalityPredicate) {
            super(Constraint.of(StandardComparison.EQUAL, node.getKey()));
            this.node = node;
            this.equalityPredicate = equalityPredicate;
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

    private static final class MetaKeyEquals extends ConstraintNodeMatcher<MetaNode> {
        MetaKeyEquals(String metaKey) {
            super(Constraint.of(StandardComparison.SIMILAR, Meta.key(metaKey, StandardComparison.WILDCARD)));
        }

        @Override
        public @Nullable MetaNode filterConstraintMatch(@NonNull Node node) {
            return NodeType.META.tryCast(node).orElse(null);
        }
    }

    private static final class TypeEquals<T extends Node> extends ConstraintNodeMatcher<T> {
        private final NodeType<? extends T> type;

        protected TypeEquals(NodeType<? extends T> type) {
            super(getConstraintForType(type));
            this.type = type;
        }

        @Override
        public @Nullable T filterConstraintMatch(@NonNull Node node) {
            return this.type.tryCast(node).orElse(null);
        }

        private static Constraint getConstraintForType(NodeType<?> type) {
            if (type == NodeType.REGEX_PERMISSION) {
                return Constraint.of(StandardComparison.SIMILAR, RegexPermission.key(StandardComparison.WILDCARD));
            } else if (type == NodeType.INHERITANCE) {
                return Constraint.of(StandardComparison.SIMILAR, Inheritance.key(StandardComparison.WILDCARD));
            } else if (type == NodeType.PREFIX) {
                return Constraint.of(StandardComparison.SIMILAR, Prefix.NODE_MARKER + StandardComparison.WILDCARD + AbstractNode.NODE_SEPARATOR + StandardComparison.WILDCARD);
            } else if (type == NodeType.SUFFIX) {
                return Constraint.of(StandardComparison.SIMILAR, Suffix.NODE_MARKER + StandardComparison.WILDCARD + AbstractNode.NODE_SEPARATOR + StandardComparison.WILDCARD);
            } else if (type == NodeType.META) {
                return Constraint.of(StandardComparison.SIMILAR, Meta.key(StandardComparison.WILDCARD, StandardComparison.WILDCARD));
            } else if (type == NodeType.WEIGHT) {
                return Constraint.of(StandardComparison.SIMILAR, Weight.NODE_MARKER + StandardComparison.WILDCARD);
            } else if (type == NodeType.DISPLAY_NAME) {
                return Constraint.of(StandardComparison.SIMILAR, DisplayName.key(StandardComparison.WILDCARD));
            }

            throw new IllegalArgumentException("Unable to create a NodeMatcher for NodeType " + type.name());
        }
    }

}
