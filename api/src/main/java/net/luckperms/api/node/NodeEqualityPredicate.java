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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.function.Predicate;

/**
 * An equality test for determining if two nodes are to be considered equal.
 *
 * <p>Recall that {@link Node}s have 4 key attributes: key, value, context, expiry.</p>
 *
 * <p>In the default {@link Node#equals(Object)} implementation (equivalent to {@link #EXACT}),
 * all 4 of these key attributes are considered. However, there are occasions where such strict
 * equality checking is not desired, hence the use of this class.</p>
 *
 * <p>{@link NodeEqualityPredicate}s can either be used inline, by directly calling the
 * {@link #areEqual(Node, Node)} method, or can be passed as a parameter to the
 * {@link Node#equals(Node, NodeEqualityPredicate)} method. Either approach is valid, and both will
 * result in the same result.</p>
 *
 * <p>Generally, implementations of this interface should fulfil the same
 * requirements as the {@link Object#equals(Object)} contract.</p>
 */
@FunctionalInterface
public interface NodeEqualityPredicate {

    /**
     * Returns if the two nodes are equal.
     *
     * <p>This method should avoid making calls to {@link Node#equals(Node, NodeEqualityPredicate)}
     * with {@code this} as the second argument, directly or otherwise.</p>
     *
     * @param o1 the first node
     * @param o2 the second node
     * @return true if equal
     */
    boolean areEqual(@NonNull Node o1, @NonNull Node o2);

    /**
     * Returns a {@link Predicate}, returning true if the tested node is equal
     * to the one given, according to the {@link NodeEqualityPredicate}.
     *
     * @param node the given node
     * @return a predicate
     */
    default Predicate<Node> equalTo(Node node) {
        return other -> areEqual(node, other);
    }

    /**
     * Represents an exact match.
     *
     * <p>Returns true if: (and)</p>
     * <p></p>
     * <ul>
     * <li>{@link Node#getKey() key} = key</li>
     * <li>{@link Node#getValue() value} = value</li>
     * <li>{@link Node#getContexts() context} = context</li>
     * <li>{@link Node#getExpiry() expiry} = expiry</li>
     * </ul>
     *
     * <p>All 4 attributes of the nodes must match to be considered equal.</p>
     *
     * <p>This is the default form of equality, used by {@link Node#equals(Object)}.</p>
     */
    NodeEqualityPredicate EXACT = new DummyNodeEqualityPredicate("EXACT");

    /**
     * Only the {@link Node#getKey() key}s need match, all other attributes are ignored.
     */
    NodeEqualityPredicate ONLY_KEY = new DummyNodeEqualityPredicate("ONLY_KEY");

    /**
     * All attributes must match, except for {@link Node#getValue() value}, which is ignored.
     *
     * <p>Returns true if: (and)</p>
     * <p></p>
     * <ul>
     * <li>{@link Node#getKey() key} = key</li>
     * <li>{@link Node#getContexts() context} = context</li>
     * <li>{@link Node#getExpiry() expiry} = expiry</li>
     * </ul>
     */
    NodeEqualityPredicate IGNORE_VALUE = new DummyNodeEqualityPredicate("IGNORE_VALUE");

    /**
     * All attributes must match, except for the {@link Node#getExpiry() expiry time}, which is
     * ignored.
     *
     * <p>Note that with this setting, whether a node has an expiry or not is still considered.</p>
     *
     * <p>Returns true if: (and)</p>
     * <p></p>
     * <ul>
     * <li>{@link Node#getKey() key} = key</li>
     * <li>{@link Node#getValue() value} = value</li>
     * <li>{@link Node#getContexts() context} = context</li>
     * <li>{@link Node#hasExpiry() has expiry} = has expiry</li>
     * </ul>
     */
    NodeEqualityPredicate IGNORE_EXPIRY_TIME = new DummyNodeEqualityPredicate("IGNORE_EXPIRY_TIME");

    /**
     * All attributes must match, except for {@link Node#getValue() value} and the
     * {@link Node#getExpiry() expiry time}, which are ignored.
     *
     * <p>Note that with this setting, whether a node has an expiry or not is still considered.</p>
     *
     * <p>Returns true if: (and)</p>
     * <p></p>
     * <ul>
     * <li>{@link Node#getKey() key} = key</li>
     * <li>{@link Node#getContexts() context} = context</li>
     * <li>{@link Node#hasExpiry() has expiry} = has expiry</li>
     * </ul>
     */
    NodeEqualityPredicate IGNORE_EXPIRY_TIME_AND_VALUE = new DummyNodeEqualityPredicate("IGNORE_EXPIRY_TIME_AND_VALUE");

    /**
     * All attributes must match, except for {@link Node#getValue() value} and the if the node
     * {@link Node#hasExpiry() has an expiry}, which are ignored.
     *
     * <p>Effectively only considers the key and the context.</p>
     *
     * <p>Returns true if: (and)</p>
     * <p></p>
     * <ul>
     * <li>{@link Node#getKey() key} = key</li>
     * <li>{@link Node#getContexts() context} = context</li>
     * </ul>
     */
    NodeEqualityPredicate IGNORE_VALUE_OR_IF_TEMPORARY = new DummyNodeEqualityPredicate("IGNORE_VALUE_OR_IF_TEMPORARY");

}
