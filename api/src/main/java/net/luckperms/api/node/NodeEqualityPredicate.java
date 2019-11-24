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
 * A rule for determining if two nodes are equal.
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


    /*
     * Some 'default' implementations of NodeEqualityPredicate are provided below.
     *
     * These are implemented in the common code, by a special case in the
     * implementation of Node#equals. As noted above, this should generally be
     * avoided.
     */

    /**
     * Represents an exact match.
     *
     * <p>All attributes of the nodes must match for them to be considered
     * equal.</p>
     */
    NodeEqualityPredicate EXACT = new NodeEqualityPredicate() {
        @Override public boolean areEqual(@NonNull Node o1, @NonNull Node o2) { return o1.equals(o2, this); }
    };

    /**
     * All attributes must match, except for
     * {@link Node#getValue() value}, which is ignored.
     */
    NodeEqualityPredicate IGNORE_VALUE = new NodeEqualityPredicate() {
        @Override public boolean areEqual(@NonNull Node o1, @NonNull Node o2) { return o1.equals(o2, this); }
    };

    /**
     * All attributes must match, except for the
     * {@link Node#getExpiry() expiry time}, which is ignored.
     *
     * <p>Note that with this setting, whether a node is temporary or not is
     * still considered.</p>
     */
    NodeEqualityPredicate IGNORE_EXPIRY_TIME = new NodeEqualityPredicate() {
        @Override public boolean areEqual(@NonNull Node o1, @NonNull Node o2) { return o1.equals(o2, this); }
    };

    /**
     * All attributes must match, except for
     * {@link Node#getValue() value} and the
     * {@link Node#getExpiry() expiry time}, which are ignored.
     */
    NodeEqualityPredicate IGNORE_EXPIRY_TIME_AND_VALUE = new NodeEqualityPredicate() {
        @Override public boolean areEqual(@NonNull Node o1, @NonNull Node o2) { return o1.equals(o2, this); }
    };

    /**
     * All attributes must match, except for
     * {@link Node#getValue() value} and the if the node is
     * {@link Node#hasExpiry() temporary}, which are ignored.
     */
    NodeEqualityPredicate IGNORE_VALUE_OR_IF_TEMPORARY = new NodeEqualityPredicate() {
        @Override public boolean areEqual(@NonNull Node o1, @NonNull Node o2) { return o1.equals(o2, this); }
    };

}
