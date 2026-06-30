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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TraversalAlgorithmTest {

    private static final Node ROOT = new Node("root",
            new Node("a",
                    new Node("a1"),
                    new Node("a2")
            ),
            new Node("b",
                    new Node("b1"),
                    new Node("b2")
            )
    );

    private static final Graph<Node> GRAPH = Node::children;

    @ParameterizedTest
    @CsvSource({
            "BREADTH_FIRST, 'root, a, b, a1, a2, b1, b2'",
            "DEPTH_FIRST_PRE_ORDER, 'root, a, a1, a2, b, b1, b2'",
            "DEPTH_FIRST_POST_ORDER, 'a1, a2, a, b1, b2, b, root'"
    })
    public void testTraversal(TraversalAlgorithm alg, String expected) {
        Iterable<Node> result = GRAPH.traverse(alg, ROOT);
        String resultString = StreamSupport.stream(result.spliterator(), false)
                .map(n -> n.name)
                .collect(Collectors.joining(", "));

        assertEquals(expected, resultString);
    }

    static class Node {
        private final String name;
        private final List<Node> children;

        public Node(String name, Node... children) {
            this.name = name;
            this.children = Arrays.asList(children);
        }

        public List<Node> children() {
            return this.children;
        }

        @Override
        public String toString() {
            return this.name + "(" + this.children + ")";
        }
    }

}
