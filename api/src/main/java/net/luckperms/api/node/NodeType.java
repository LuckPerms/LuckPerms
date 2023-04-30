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
import net.luckperms.api.node.types.DisplayNameNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.RegexPermissionNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a type of {@link Node}.
 */
public interface NodeType<T extends Node> {

    /**
     * Node type for {@link PermissionNode}.
     */
    NodeType<PermissionNode> PERMISSION = new SimpleNodeType<>(
            "PERMISSION",
            n -> n instanceof PermissionNode,
            n -> (PermissionNode) n
    );

    /**
     * Node type for {@link RegexPermissionNode}.
     */
    NodeType<RegexPermissionNode> REGEX_PERMISSION = new SimpleNodeType<>(
            "REGEX_PERMISSION",
            n -> n instanceof RegexPermissionNode,
            n -> (RegexPermissionNode) n
    );

    /**
     * Node type for {@link InheritanceNode}.
     */
    NodeType<InheritanceNode> INHERITANCE = new SimpleNodeType<>(
            "INHERITANCE",
            n -> n instanceof InheritanceNode,
            n -> (InheritanceNode) n
    );

    /**
     * Node type for {@link PrefixNode}.
     */
    NodeType<PrefixNode> PREFIX = new SimpleNodeType<>(
            "PREFIX",
            n -> n instanceof PrefixNode,
            n -> (PrefixNode) n
    );

    /**
     * Node type for {@link SuffixNode}.
     */
    NodeType<SuffixNode> SUFFIX = new SimpleNodeType<>(
            "SUFFIX",
            n -> n instanceof SuffixNode,
            n -> (SuffixNode) n
    );

    /**
     * Node type for {@link MetaNode}.
     */
    NodeType<MetaNode> META = new SimpleNodeType<>(
            "META",
            n -> n instanceof MetaNode,
            n -> (MetaNode) n
    );

    /**
     * Node type for {@link WeightNode}.
     */
    NodeType<WeightNode> WEIGHT = new SimpleNodeType<>(
            "WEIGHT",
            n -> n instanceof WeightNode,
            n -> (WeightNode) n
    );

    /**
     * Node type for {@link DisplayNameNode}.
     */
    NodeType<DisplayNameNode> DISPLAY_NAME = new SimpleNodeType<>(
            "DISPLAY_NAME",
            n -> n instanceof DisplayNameNode,
            n -> (DisplayNameNode) n
    );

    /**
     * Node type for {@link ChatMetaNode}.
     *
     * <p>This is an abstract type, and therefore will never
     * be returned from {@link Node#getType()}.</p>
     */
    NodeType<ChatMetaNode<?, ?>> CHAT_META = new SimpleNodeType<>(
            "CHAT_META",
            n -> n instanceof ChatMetaNode<?, ?>,
            n -> (ChatMetaNode<?, ?>) n
    );

    /**
     * Node type for {@link ChatMetaNode} or {@link MetaNode}.
     *
     * <p>This is an abstract type, and therefore will never
     * be returned from {@link Node#getType()}.</p>
     */
    NodeType<Node> META_OR_CHAT_META = new SimpleNodeType<>(
            "META_OR_CHAT_META",
            n -> META.matches(n) || CHAT_META.matches(n),
            Function.identity()
    );

    /**
     * Gets a name for the node type.
     *
     * @return a name
     */
    @NonNull String name();

    /**
     * Returns if the passed node matches the type
     *
     * @param node the node to test
     * @return true if the node has the same type
     */
    boolean matches(@NonNull Node node);

    /**
     * Casts the given {@link Node} to the type defined by the {@link NodeType}.
     *
     * <p>An {@link IllegalArgumentException} is thrown if the node to cast does
     * not {@link #matches(Node) match} the type.</p>
     *
     * @param node the node to cast
     * @return the casted node
     * @throws IllegalArgumentException if the node to cast does not match the type
     */
    @NonNull T cast(@NonNull Node node);

    /**
     * Attempts to cast the given {@link Node} to the type defined by the
     * {@link NodeType}.
     *
     * <p>Returns an {@link Optional#empty() empty optional} if the node to cast
     * does not {@link #matches(Node) match} the type.</p>
     *
     * @param node the node to cast
     * @return an optional, possibly containing a casted node
     */
    default @NonNull Optional<T> tryCast(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        if (!matches(node)) {
            return Optional.empty();
        } else {
            return Optional.of(cast(node));
        }
    }

    /**
     * Returns a {@link Predicate}, returning whether a {@link Node}
     * {@link #matches(Node) matches} this type.
     *
     * @return a predicate for the {@link #matches(Node)} method.
     */
    default @NonNull Predicate<Node> predicate() {
        return this::matches;
    }

    /**
     * Returns a {@link Predicate}, returning whether a {@link Node}
     * {@link #matches(Node) matches} this type, and passes the given
     * {@code and} {@link Predicate}.
     *
     * @param and a predicate to AND with the result of the type match check
     * @return a matching predicate, ANDed with the given predicate parameter
     */
    default @NonNull Predicate<Node> predicate(@NonNull Predicate<? super T> and) {
        return node -> matches(node) && and.test(cast(node));
    }

}
