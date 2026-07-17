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

package me.lucko.luckperms.common.model;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.nodemap.NodeMapMutable;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.query.QueryOptionsBuilderImpl;
import me.lucko.luckperms.common.util.Difference;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NodeMapTest {

    private static final PermissionHolderIdentifier ORIGIN = new PermissionHolderIdentifier(HolderType.GROUP, "test");

    @Mock private PermissionHolder mockHolder;

    @BeforeEach
    public void setupMocks() {
        when(this.mockHolder.getIdentifier()).thenReturn(ORIGIN);
    }

    private static Node makeNode(String key) {
        return NodeBuilders.determineMostApplicable(key).build();
    }

    @Test
    public void testSimpleAddAndRemove() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        assertEquals(0, map.size());

        Node node = makeNode("test");

        Difference<Node> r1 = map.add(node);
        assertEquals(ImmutableSet.of(node), r1.getAdded());
        assertEquals(ImmutableSet.of(), r1.getRemoved());
        assertEquals(1, map.size());

        Difference<Node> r2 = map.remove(node);
        assertEquals(ImmutableSet.of(), r2.getAdded());
        assertEquals(ImmutableSet.of(node), r2.getRemoved());
        assertEquals(0, map.size());
    }

    @Test
    public void testInheritanceOrigin() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node node = makeNode("test");

        map.add(node);

        List<Node> nodes = map.asList();
        assertEquals(ImmutableList.of(node), map.asList());

        InheritanceOriginMetadata origin = nodes.get(0).metadata(InheritanceOriginMetadata.KEY);
        assertEquals(ORIGIN, origin.getOrigin());
        assertEquals(DataType.NORMAL, origin.getDataType());
    }

    @ParameterizedTest
    @CsvSource({
            "test, true, false",
            "test, false, true",
            "group.test, true, false",
            "group.test, false, true"
    })
    public void testRemoveMatchingButNotSameValue(String nodeKey, boolean firstValue, boolean secondValue) {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);

        Node first = makeNode(nodeKey).toBuilder().value(firstValue).build();
        Node second = makeNode(nodeKey).toBuilder().value(secondValue).build();

        map.add(first);

        Difference<Node> diff = map.add(second);
        assertEquals(ImmutableSet.of(first), diff.getRemoved());
        assertEquals(ImmutableSet.of(second), diff.getAdded());
        assertEquals(ImmutableList.of(second), map.asList());

        if (second.getType() == NodeType.INHERITANCE && second.getValue()) {
            assertEquals(ImmutableList.of(second), map.inheritanceAsList());
        } else {
            assertEquals(ImmutableList.of(), map.inheritanceAsList());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "test, 1, 5",
            "test, 5, 1",
            "group.test, 1, 5",
            "group.test, 5, 1"
    })
    public void testRemoveMatchingButNotSameExpiry(String nodeKey, int firstDuration, int secondDuration) {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);

        Node first = makeNode(nodeKey).toBuilder()
                .expiry(firstDuration == 0 ? null : Duration.ofDays(firstDuration))
                .build();
        Node second = makeNode(nodeKey).toBuilder()
                .expiry(secondDuration == 0 ? null : Duration.ofDays(secondDuration))
                .build();

        map.add(first);

        Difference<Node> diff = map.add(second);
        assertEquals(ImmutableSet.of(first), diff.getRemoved());
        assertEquals(ImmutableSet.of(second), diff.getAdded());
        assertEquals(ImmutableList.of(second), map.asList());

        if (second.getType() == NodeType.INHERITANCE) {
            assertEquals(ImmutableList.of(second), map.inheritanceAsList());
        } else {
            assertEquals(ImmutableList.of(), map.inheritanceAsList());
        }
    }

    @Test
    public void testRemove() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        map.add(makeNode("test1"));
        map.add(makeNode("test2").toBuilder().value(false).build());
        map.add(makeNode("test3").toBuilder().expiry(1, TimeUnit.HOURS).build());
        map.add(makeNode("test4").toBuilder().withContext("hello", "world").build());

        assertEquals(4, map.size());

        for (Node node : List.of(
                makeNode("test1").toBuilder().withContext("hello", "world").build(),
                makeNode("test3"),
                makeNode("test4"),
                makeNode("test4").toBuilder().withContext("hello", "world").withContext("aaa", "bbb").build(),
                makeNode("test5")
        )) {
            Difference<Node> diff = map.remove(node);
            assertEquals(Set.of(), diff.getChanges());
        }

        assertEquals(4, map.size());

        for (Node node : List.of(
                makeNode("test1"),
                makeNode("test2").toBuilder().value(true).build(),
                makeNode("test3").toBuilder().expiry(2, TimeUnit.HOURS).build(),
                makeNode("test4").toBuilder().withContext("hello", "world").build()
        )) {
            Difference<Node> diff = map.remove(node);
            assertEquals(0, diff.getAdded().size());
            assertEquals(1, diff.getRemoved().size());
        }

        assertEquals(0, map.size());
    }

    @Test
    public void testRemoveExact() {
        Instant expiry = Instant.now().plusSeconds(60);

        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        map.add(makeNode("test1"));
        map.add(makeNode("test2").toBuilder().value(false).build());
        map.add(makeNode("test3").toBuilder().expiry(expiry).build());
        map.add(makeNode("test4").toBuilder().withContext("hello", "world").build());

        assertEquals(4, map.size());

        for (Node node : List.of(
                makeNode("test1").toBuilder().withContext("hello", "world").build(),
                makeNode("test3"),
                makeNode("test4"),
                makeNode("test4").toBuilder().withContext("hello", "world").withContext("aaa", "bbb").build(),
                makeNode("test5"),
                makeNode("test2").toBuilder().value(true).build(),
                makeNode("test3").toBuilder().expiry(2, TimeUnit.HOURS).build(),
                makeNode("test4").toBuilder().withContext("hello", "world").withContext("aaa", "bbb").build()
        )) {
            Difference<Node> diff = map.removeExact(node);
            assertEquals(Set.of(), diff.getChanges());
        }

        assertEquals(4, map.size());

        for (Node node : List.of(
                makeNode("test1"),
                makeNode("test2").toBuilder().value(false).build(),
                makeNode("test3").toBuilder().expiry(expiry).build(),
                makeNode("test4").toBuilder().withContext("hello", "world").build()
        )) {
            Difference<Node> diff = map.removeExact(node);
            assertEquals(0, diff.getAdded().size());
            assertEquals(1, diff.getRemoved().size());
        }

        assertEquals(0, map.size());
    }

    @Test
    public void testClear() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        map.add(makeNode("a"));
        map.add(makeNode("b"));
        map.add(makeNode("c"));

        assertEquals(3, map.size());

        Difference<Node> diff = map.clear();
        assertEquals(3, diff.getRemoved().size());
        assertEquals(0, map.size());
    }

    @Test
    public void testSetContent() {
        Node a = makeNode("a");
        Node b = makeNode("b");
        Node c = makeNode("c");

        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        map.add(a);
        map.add(b);

        Difference<Node> diff = map.setContent(List.of(b, c));
        assertEquals(Set.of(c), diff.getAdded());
        assertEquals(Set.of(a), diff.getRemoved());

        assertEquals(Set.of(b, c), map.asSet());
    }

    @ParameterizedTest(name = "s={0} w={1} is={2} iw={3}")
    @CsvSource({
            "true, true, true, true, 8, 4",
            "true, true, false, false, 8, 1",
            "false, true, true, true, 6, 4",
            "true, false, true, true, 6, 4",
            "false, false, true, true, 5, 4",
            "false, true, false, true, 4, 2",
            "true, false, true, false, 4, 2",
            "false, false, false, false, 2, 1"
    })
    public void testFlagsFiltering(boolean includeServer, boolean includeWorld, boolean inheritanceIncludeServer, boolean inheritanceIncludeWorld, int expected, int expectedInheritance) {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL) {
            @Override
            protected ContextSatisfyMode defaultSatisfyMode() {
                return ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY;
            }
        };

        map.add(makeNode("test1"));
        map.add(makeNode("test2").toBuilder().withContext("server", "test").build());
        map.add(makeNode("test3").toBuilder().withContext("world", "test").build());
        map.add(makeNode("test4").toBuilder().withContext("server", "test").withContext("world", "test").build());
        map.add(makeNode("group.test1"));
        map.add(makeNode("group.test2").toBuilder().withContext("server", "test").build());
        map.add(makeNode("group.test3").toBuilder().withContext("world", "test").build());
        map.add(makeNode("group.test4").toBuilder().withContext("server", "test").withContext("world", "test").build());

        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        if (includeServer) flags.add(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT);
        if (includeWorld) flags.add(Flag.INCLUDE_NODES_WITHOUT_WORLD_CONTEXT);
        if (inheritanceIncludeServer) flags.add(Flag.APPLY_INHERITANCE_NODES_WITHOUT_SERVER_CONTEXT);
        if (inheritanceIncludeWorld) flags.add(Flag.APPLY_INHERITANCE_NODES_WITHOUT_WORLD_CONTEXT);

        QueryOptions options = new QueryOptionsBuilderImpl(QueryMode.NON_CONTEXTUAL)
                .flags(flags)
                .build();

        Set<Node> output = new HashSet<>();
        map.copyTo(output, options);
        assertEquals(expected, output.size());

        output.clear();
        map.forEach(options, output::add);
        assertEquals(expected, output.size());

        Set<InheritanceNode> inheritanceOutput = new HashSet<>();
        map.copyInheritanceNodesTo(inheritanceOutput, options);
        assertEquals(expectedInheritance, inheritanceOutput.size());
    }

    @Test
    public void testRemoveIf() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node test1 = makeNode("test1");
        Node test2 = makeNode("test2").toBuilder().value(false).build();
        Node group1 = makeNode("group.test1");
        Node group2 = makeNode("group.test2").toBuilder().value(false).build();

        map.add(test1);
        map.add(test2);
        map.add(group1);
        map.add(group2);
        assertEquals(4, map.size());

        Difference<Node> diff = map.removeIf(node -> !node.getValue());
        assertEquals(Set.of(test2, group2), diff.getRemoved());
        assertEquals(Set.of(), diff.getAdded());
        assertEquals(2, map.size());
        assertEquals(Set.of(test1, group1), map.asSet());
        assertEquals(List.of(group1), map.inheritanceAsList());
    }

    @Test
    public void testRemoveIfWithContext() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node inContext = makeNode("test1").toBuilder().withContext("server", "test").build();
        Node globalNode = makeNode("test2");
        Node otherInContext = makeNode("test3").toBuilder().withContext("server", "test").build();

        map.add(inContext);
        map.add(globalNode);
        map.add(otherInContext);
        assertEquals(3, map.size());

        Difference<Node> diff = map.removeIf(inContext.getContexts(), node -> node.getKey().equals("test1"));
        assertEquals(Set.of(inContext), diff.getRemoved());
        assertEquals(2, map.size());
        assertEquals(Set.of(globalNode, otherInContext), map.asSet());

        // no-op if the context set isn't present in the map
        Difference<Node> diff2 = map.removeIf(ImmutableContextSetImpl.of("world", "test"), node -> true);
        assertTrue(diff2.isEmpty());
        assertEquals(2, map.size());
    }

    @ParameterizedTest
    @CsvSource({
            "test1, test2, false",
            "test1, test1, true"
    })
    public void testRemoveThenAdd(String removeKey, String addKey, boolean sameNode) {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node toRemove = makeNode(removeKey);
        Node toAdd = makeNode(addKey);
        map.add(toRemove);

        Difference<Node> diff = map.removeThenAdd(toRemove, toAdd);

        if (sameNode) {
            // removeThenAdd is a no-op if the two nodes are equal
            assertTrue(diff.isEmpty());
            assertEquals(1, map.size());
            assertEquals(Set.of(toRemove), map.asSet());
        } else {
            assertEquals(Set.of(toAdd), diff.getAdded());
            assertEquals(Set.of(toRemove), diff.getRemoved());
            assertEquals(Set.of(toAdd), map.asSet());
        }
    }

    @Test
    public void testClearContext() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node global = makeNode("test1");
        Node scoped = makeNode("test2").toBuilder().withContext("server", "test").build();

        map.add(global);
        map.add(scoped);
        assertEquals(2, map.size());

        Difference<Node> diff = map.clear(scoped.getContexts());
        assertEquals(Set.of(scoped), diff.getRemoved());
        assertEquals(Set.of(global), map.asSet());

        // clearing a context that's no longer present is a no-op
        Difference<Node> diff2 = map.clear(scoped.getContexts());
        assertTrue(diff2.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testApplyChanges() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node existing = makeNode("test1");
        Node incoming = makeNode("test2");
        map.add(existing);

        Difference<Node> changes = new Difference<>();
        changes.recordChange(Difference.ChangeType.REMOVE, existing);
        changes.recordChange(Difference.ChangeType.ADD, incoming);

        Difference<Node> result = map.applyChanges(changes);
        assertEquals(Set.of(incoming), result.getAdded());
        assertEquals(Set.of(existing), result.getRemoved());
        assertEquals(Set.of(incoming), map.asSet());
    }

    @Test
    public void testNodesInContext() {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL);
        Node global = makeNode("test1");
        Node scoped = makeNode("group.test2").toBuilder().withContext("server", "test").build();

        map.add(global);
        map.add(scoped);

        assertEquals(Set.of(global), Set.copyOf(map.nodesInContext(ImmutableContextSetImpl.EMPTY)));
        assertEquals(Set.of(scoped), Set.copyOf(map.nodesInContext(scoped.getContexts())));
        assertEquals(Set.of(scoped), Set.copyOf(map.inheritanceNodesInContext(scoped.getContexts())));

        // context not present in the map
        assertEquals(Set.of(), map.nodesInContext(ImmutableContextSetImpl.of("world", "test")));
    }

    @ParameterizedTest
    @CsvSource({
            "'', 2, 1",
            "server=test, 4, 2",
            "world=test, 4, 2",
            "server=test|world=test, 8, 4",
            "server=test|world=test|test=test, 8, 4",
    })
    public void testContextFiltering(String context, int expected, int expectedInheritance) {
        NodeMapMutable map = new NodeMapMutable(this.mockHolder, DataType.NORMAL) {
            @Override
            protected ContextSatisfyMode defaultSatisfyMode() {
                return ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY;
            }
        };

        map.add(makeNode("test1"));
        map.add(makeNode("test2").toBuilder().withContext("server", "test").build());
        map.add(makeNode("test3").toBuilder().withContext("world", "test").build());
        map.add(makeNode("test4").toBuilder().withContext("server", "test").withContext("world", "test").build());
        map.add(makeNode("group.test1"));
        map.add(makeNode("group.test2").toBuilder().withContext("server", "test").build());
        map.add(makeNode("group.test3").toBuilder().withContext("world", "test").build());
        map.add(makeNode("group.test4").toBuilder().withContext("server", "test").withContext("world", "test").build());

        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        if (!context.isEmpty()) {
            Splitter.on('|').withKeyValueSeparator('=').split(context).forEach(builder::add);
        }

        QueryOptions options = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL)
                .context(builder.build())
                .build();

        Set<Node> output = new HashSet<>();
        map.copyTo(output, options);
        assertEquals(expected, output.size());

        output.clear();
        map.forEach(options, output::add);
        assertEquals(expected, output.size());

        Set<InheritanceNode> inheritanceOutput = new HashSet<>();
        map.copyInheritanceNodesTo(inheritanceOutput, options);
        assertEquals(expectedInheritance, inheritanceOutput.size());
    }

}
