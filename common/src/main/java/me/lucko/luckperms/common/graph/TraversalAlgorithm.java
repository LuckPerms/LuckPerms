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

/*
 * Copyright (C) 2017 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.lucko.luckperms.common.graph;

import com.google.common.collect.AbstractIterator;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * A set of traversal algorithm implementations for {@link Graph}s.
 *
 * @author Jens Nyman (Guava)
 */
public enum TraversalAlgorithm {

    /**
     * Traverses in breadth-first order.
     *
     * <p>That is, all the nodes of depth 0 are returned, then depth 1, then 2, and so on.</p>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Breadth-first_search">Wikipedia</a> for more info.</p>
     */
    BREADTH_FIRST {
        @Override
        public <N> Iterable<N> traverse(Graph<N> graph, N startNode) {
            Objects.requireNonNull(graph, "graph");
            Objects.requireNonNull(startNode, "startNode");
            return () -> new BreadthFirstIterator<>(graph, startNode);
        }
    },

    /**
     * Traverses in depth-first pre-order.
     *
     * <p>"Pre-order" implies that nodes appear in the order in which they are
     * first visited.</p>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.</p>
     */
    DEPTH_FIRST_PRE_ORDER {
        @Override
        public <N> Iterable<N> traverse(Graph<N> graph, N startNode) {
            Objects.requireNonNull(graph, "graph");
            Objects.requireNonNull(startNode, "startNode");
            return () -> new DepthFirstIterator<>(graph, startNode, DepthFirstIterator.Order.PRE_ORDER);
        }
    },

    /**
     * Traverses in depth-first post-order.
     *
     * <p>"Post-order" implies that nodes appear in the order in which they are
     * visited for the last time.</p>
     *
     * <p>See <a href="https://en.wikipedia.org/wiki/Depth-first_search">Wikipedia</a> for more info.</p>
     */
    DEPTH_FIRST_POST_ORDER {
        @Override
        public <N> Iterable<N> traverse(Graph<N> graph, N startNode) {
            Objects.requireNonNull(graph, "graph");
            Objects.requireNonNull(startNode, "startNode");
            return () -> new DepthFirstIterator<>(graph, startNode, DepthFirstIterator.Order.POST_ORDER);
        }
    };

    /**
     * Returns an unmodifiable {@code Iterable} over the nodes reachable from
     * {@code startNode}, in the order defined by the {@code algorithm}.
     *
     * @param graph the graph
     * @param startNode the start node
     * @param <N> the node type
     * @return the traversal
     */
    public abstract <N> Iterable<N> traverse(Graph<N> graph, N startNode);

    private static final class BreadthFirstIterator<N> implements Iterator<N> {
        private final Graph<N> graph;

        private final Queue<N> queue = new ArrayDeque<>();
        private final Set<N> visited = new HashSet<>();

        BreadthFirstIterator(Graph<N> graph, N root) {
            this.graph = graph;
            this.queue.add(root);
            this.visited.add(root);
        }

        @Override
        public boolean hasNext() {
            return !this.queue.isEmpty();
        }

        @Override
        public N next() {
            N current = this.queue.remove();
            for (N neighbor : this.graph.successors(current)) {
                if (this.visited.add(neighbor)) {
                    this.queue.add(neighbor);
                }
            }
            return current;
        }
    }

    private static final class DepthFirstIterator<N> extends AbstractIterator<N> {
        private final Graph<N> graph;

        private final Deque<NodeAndSuccessors> stack = new ArrayDeque<>();
        private final Set<N> visited = new HashSet<>();
        private final Order order;

        DepthFirstIterator(Graph<N> graph, N root, Order order) {
            this.graph = graph;

            // our invariant is that in computeNext we call next on the iterator at the top first, so we
            // need to start with one additional item on that iterator
            this.stack.push(withSuccessors(root));
            this.order = order;
        }

        @Override
        protected N computeNext() {
            while (true) {
                if (this.stack.isEmpty()) {
                    return endOfData();
                }
                NodeAndSuccessors node = this.stack.getFirst();
                boolean firstVisit = this.visited.add(node.node);
                boolean lastVisit = !node.successorIterator.hasNext();
                boolean produceNode = (firstVisit && this.order == Order.PRE_ORDER) || (lastVisit && this.order == Order.POST_ORDER);
                if (lastVisit) {
                    this.stack.pop();
                } else {
                    // we need to push a neighbor, but only if we haven't already seen it
                    N successor = node.successorIterator.next();
                    if (!this.visited.contains(successor)) {
                        this.stack.push(withSuccessors(successor));
                    }
                }
                if (produceNode) {
                    return node.node;
                }
            }
        }

        NodeAndSuccessors withSuccessors(N node) {
            return new NodeAndSuccessors(node, this.graph.successors(node));
        }

        /**
         * A simple tuple of a node and a partially iterated {@link Iterator} of
         * its successors
         */
        private final class NodeAndSuccessors {
            final N node;
            final Iterator<? extends N> successorIterator;

            NodeAndSuccessors(N node, Iterable<? extends N> successors) {
                this.node = node;
                this.successorIterator = successors.iterator();
            }
        }

        private enum Order {
            PRE_ORDER,
            POST_ORDER
        }
    }

}
