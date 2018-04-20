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

package me.lucko.luckperms.common.graph;

/**
 * A minimal functional interface for graph-structured data.
 *
 * @param <N> the node parameter type
 */
@FunctionalInterface
public interface Graph<N> {

    /**
     * Returns all nodes in this graph directly adjacent to {@code node} which
     * can be reached by traversing {@code node}'s outgoing edges.
     *
     * @throws IllegalArgumentException if {@code node} is not an element of this graph
     */
    Iterable<? extends N> successors(N node);

    /**
     * Returns an iterable which will traverse this graph using the specified algorithm starting
     * at the given node.
     *
     * @param algorithm the algorithm to use when traversing
     * @param startNode the start node in the inheritance graph
     * @return an iterable
     */
    default Iterable<N> traverse(TraversalAlgorithm algorithm, N startNode) {
        return GraphTraversers.traverseUsing(algorithm, this, startNode);
    }

}