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

package me.lucko.luckperms.common.node;

import com.google.common.collect.Lists;
import me.lucko.luckperms.common.context.ContextSetComparatorTest;
import me.lucko.luckperms.common.node.comparator.NodeComparator;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.types.Permission;
import net.luckperms.api.node.Node;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link NodeComparator} and {@link NodeWithContextComparator}.
 *
 * <p>Both comparators are used to order nodes by "priority" - the primary use case being the
 * {@code SortedSet}/{@code SortedMap} instances backing {@code NodeMapMutable}, where the
 * <b>descending</b> comparator is used so that the highest priority node appears first when the
 * collection is iterated.</p>
 *
 * <p>{@link ContextSetComparatorTest} already extensively covers the rules used to compare nodes
 * based on their contexts (e.g. more specific contexts are higher priority) - this class instead
 * focuses on the node-specific tie-breaking rules in {@link NodeComparator}, and how
 * {@link NodeWithContextComparator} combines context specificity with those rules.</p>
 */
public class NodeComparatorTest {

    @Nested
    class NodeComparatorTests {

        @Test
        @SuppressWarnings("EqualsWithItself")
        public void testEquals() {
            Node node = Permission.builder().permission("hello.world").build();
            assertEquals(0, NodeComparator.ascending().compare(node, node));
            assertEquals(0, NodeComparator.descending().compare(node, node));
        }

        @Test
        public void testDescendingIsReverseOfAscending() {
            Instant expiry = Instant.now().plusSeconds(60);
            List<Node> nodes = new ArrayList<>(List.of(
                    Permission.builder().permission("a").build(),
                    Permission.builder().permission("z").build(),
                    Permission.builder().permission("a").expiry(expiry).build(),
                    Permission.builder().permission("foo.*").build(),
                    Permission.builder().permission("foo.bar.*").build()
            ));

            List<Node> ascendingSorted = new ArrayList<>(nodes);
            ascendingSorted.sort(NodeComparator.ascending());

            List<Node> descendingSorted = new ArrayList<>(nodes);
            descendingSorted.sort(NodeComparator.descending());

            assertEquals(Lists.reverse(ascendingSorted), descendingSorted);
        }

        @Test
        public void testTemporaryNodesTakePriorityOverPermanent() {
            Node permanentNode = Permission.builder().permission("hello.world").build();
            Instant expiry = Instant.now().plusSeconds(60);
            Node temporaryNode = Permission.builder().permission("hello.world").expiry(expiry).build();

            // the temporary node should sort first (higher priority) with the descending comparator
            List<Node> list = new ArrayList<>(List.of(permanentNode, temporaryNode));
            list.sort(NodeComparator.descending());
            assertEquals(List.of(temporaryNode, permanentNode), list);
        }

        @Test
        public void testTemporaryNodesAreOrderedByExpiryTime() {
            Instant expiry1 = Instant.now().plusSeconds(60);
            Node temporaryNode1 = Permission.builder().permission("hello.world").expiry(expiry1).build();

            Instant expiry2 = Instant.now().plusSeconds(120);
            Node temporaryNode2 = Permission.builder().permission("hello.world").expiry(expiry2).build();

            // the node with the sooner expiry time should sort first (higher priority) with the descending comparator
            List<Node> list = new ArrayList<>(List.of(temporaryNode2, temporaryNode1));
            list.sort(NodeComparator.descending());
            assertEquals(List.of(temporaryNode1, temporaryNode2), list);
        }

        @Test
        public void testMoreSpecificWildcardsTakePriorityOverLessSpecific() {
            Node rootWildcard = Permission.builder().permission("*").build();
            Node shallowWildcard = Permission.builder().permission("foo.*").build();
            Node deepWildcard = Permission.builder().permission("foo.bar.*").build();

            List<Node> list = new ArrayList<>(List.of(rootWildcard, shallowWildcard, deepWildcard));
            list.sort(NodeComparator.descending());

            // more specific (deeper) wildcards should be ordered first
            assertEquals(List.of(deepWildcard, shallowWildcard, rootWildcard), list);
        }

        @Test
        public void testKeyOrderingIsAlphabeticalWhenOtherwiseEqual() {
            Node a = Permission.builder().permission("a").build();
            Node m = Permission.builder().permission("m").build();
            Node z = Permission.builder().permission("z").build();

            List<Node> list = new ArrayList<>(List.of(z, a, m));
            list.sort(NodeComparator.descending());

            assertEquals(List.of(a, m, z), list);
        }

        @Test
        public void testFalseValuePriorityOverTrue() {
            Node trueNode = Permission.builder().permission("hello.world").value(true).build();
            Node falseNode = Permission.builder().permission("hello.world").value(false).build();

            List<Node> list = new ArrayList<>(List.of(trueNode, falseNode));
            list.sort(NodeComparator.descending());

            assertEquals(List.of(falseNode, trueNode), list);
        }
    }

    @Nested
    class NodeWithContextComparatorTests {

        @Test
        @SuppressWarnings("EqualsWithItself")
        public void testEquals() {
            Node node = Permission.builder().permission("hello.world").withContext("server", "foo").build();
            assertEquals(0, NodeWithContextComparator.ascending().compare(node, node));
            assertEquals(0, NodeWithContextComparator.descending().compare(node, node));
        }

        @Test
        public void testDescendingIsReverseOfAscending() {
            Instant expiry = Instant.now().plusSeconds(60);
            List<Node> nodes = new ArrayList<>(List.of(
                    Permission.builder().permission("a").build(),
                    Permission.builder().permission("a").withContext("server", "foo").build(),
                    Permission.builder().permission("a").withContext("world", "foo").build(),
                    Permission.builder().permission("a").expiry(expiry).build()
            ));

            List<Node> ascendingSorted = new ArrayList<>(nodes);
            ascendingSorted.sort(NodeWithContextComparator.ascending());

            List<Node> descendingSorted = new ArrayList<>(nodes);
            descendingSorted.sort(NodeWithContextComparator.descending());

            assertEquals(Lists.reverse(ascendingSorted), descendingSorted);
        }

        @Test
        public void testContextSpecificityTakesPriorityOverNodeProperties() {
            // this node has a more specific context set, but would otherwise be considered
            // lower priority than the other node if only NodeComparator rules were applied
            // (it's permanent, whereas the other is temporary)
            Node moreSpecificContext = Permission.builder().permission("hello.world").withContext("server", "foo").build();
            Instant expiry = Instant.now().plusSeconds(60);
            Node lessSpecificContext = Permission.builder().permission("hello.world").expiry(expiry).build();

            // context specificity should win out regardless of the temporary/permanent difference
            assertTrue(NodeWithContextComparator.descending().compare(moreSpecificContext, lessSpecificContext) < 0);

            List<Node> list = new ArrayList<>(List.of(lessSpecificContext, moreSpecificContext));
            list.sort(NodeWithContextComparator.descending());
            assertEquals(List.of(moreSpecificContext, lessSpecificContext), list);
        }

        @Test
        public void testFallsBackToNodeComparatorWhenContextsAreEqual() {
            // both nodes share the same (empty) context set, so the comparator should
            // fall back to NodeComparator's rules - temporary nodes take priority
            Node permanentNode = Permission.builder().permission("hello.world").build();
            Instant expiry = Instant.now().plusSeconds(60);
            Node temporaryNode = Permission.builder().permission("hello.world").expiry(expiry).build();

            List<Node> list = new ArrayList<>(List.of(permanentNode, temporaryNode));
            list.sort(NodeWithContextComparator.descending());
            assertEquals(List.of(temporaryNode, permanentNode), list);
        }

        @Test
        public void testPriorityOrderingAcrossContextsAndNodeProperties() {
            Node empty = Permission.builder().permission("hello.world").build();
            Node withServerContext = Permission.builder().permission("hello.world").withContext("server", "foo").build();
            Instant expiry = Instant.now().plusSeconds(60);
            Node withServerContextTemporary = (Permission.builder().permission("hello.world").expiry(expiry).build()).toBuilder()
                    .withContext("server", "foo")
                    .build();

            List<Node> list = new ArrayList<>(List.of(empty, withServerContext, withServerContextTemporary));
            list.sort(NodeWithContextComparator.descending());

            // most specific context wins first; within the same context, temporary beats permanent
            assertEquals(List.of(withServerContextTemporary, withServerContext, empty), list);
        }
    }

}
