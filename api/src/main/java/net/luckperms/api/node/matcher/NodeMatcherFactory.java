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

package net.luckperms.api.node.matcher;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A factory which creates {@link NodeMatcher}s.
 *
 * @since 5.1
 */
public interface NodeMatcherFactory {

    /**
     * Gets a {@link NodeMatcher} which matches nodes with the same {@link Node#getKey() key}.
     *
     * <p>The {@link String#equalsIgnoreCase(String)} method is used to test key equality.</p>
     *
     * <p>Prefer using the {@link NodeMatcher#key(String)} accessor.</p>
     *
     * @param key the key
     * @return the matcher
     */
    @NonNull NodeMatcher<Node> key(@NonNull String key);

    /**
     * Gets a {@link NodeMatcher} which matches nodes with the same {@link Node#getKey() key}.
     *
     * <p>The {@link String#equalsIgnoreCase(String)} method is used to test key equality.</p>
     *
     * <p>Prefer using the {@link NodeMatcher#key(Node)} accessor.</p>
     *
     * @param node the node to use for the key
     * @param <T> the node type
     * @return the matcher
     */
    <T extends Node> @NonNull NodeMatcher<T> key(@NonNull T node);

    /**
     * Gets a {@link NodeMatcher} which matches nodes with a {@link Node#getKey() key} starting
     * with the given string.
     *
     * <p>Prefer using the {@link NodeMatcher#keyStartsWith(String)} accessor.</p>
     *
     * @param startingWith the string to match
     * @return the matcher
     */
    @NonNull NodeMatcher<Node> keyStartsWith(@NonNull String startingWith);

    /**
     * Gets a {@link NodeMatcher} which matches nodes which are
     * {@link Node#equals(Node, NodeEqualityPredicate) equal to} the given {@code other} node
     * according to the {@link NodeEqualityPredicate}.
     *
     * <p>Prefer using the {@link NodeMatcher#equals(Node, NodeEqualityPredicate)} accessor.</p>
     *
     * @param other the other node to test against
     * @param equalityPredicate the equality predicate
     * @param <T> the node type
     * @return the matcher
     */
    @NonNull <T extends Node> NodeMatcher<T> equals(@NonNull T other, @NonNull NodeEqualityPredicate equalityPredicate);

    /**
     * Gets a {@link NodeMatcher} which matches {@link MetaNode}s with the same
     * {@link MetaNode#getMetaKey() meta key}.
     *
     * <p>The {@link String#equalsIgnoreCase(String)} method is used to test key equality.</p>
     *
     * <p>Prefer using the {@link NodeMatcher#metaKey(String)} accessor.</p>
     *
     * @param metaKey the meta key
     * @return the matcher
     */
    @NonNull NodeMatcher<MetaNode> metaKey(@NonNull String metaKey);

    /**
     * Gets a {@link NodeMatcher} which matches {@link Node}s with the same
     * type as the given {@link NodeType}.
     *
     * <p>{@link NodeType#PERMISSION}, {@link NodeType#CHAT_META} and
     * {@link NodeType#META_OR_CHAT_META} are not supported by this method.</p>
     *
     * <p>Prefer using the {@link NodeMatcher#type(NodeType)} accessor.</p>
     *
     * @param type the node type
     * @param <T> the node type
     * @return the matcher
     */
    <T extends Node> @NonNull NodeMatcher<T> type(NodeType<? extends T> type);

}
