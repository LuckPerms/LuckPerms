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

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.inheritance.InheritanceGraphFactory;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Weight;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsBuilderImpl;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InheritanceTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private LuckPermsConfiguration configuration;

    private StandardGroupManager groupManager;

    @BeforeEach
    public void setupMocks() {
        this.groupManager = new StandardGroupManager(this.plugin);

        //noinspection unchecked,rawtypes
        lenient().when(this.plugin.getGroupManager()).thenReturn((GroupManager) this.groupManager);
        lenient().when(this.plugin.getInheritanceGraphFactory()).thenReturn(new InheritanceGraphFactory(this.plugin));
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.configuration.get(ConfigKeys.CONTEXT_SATISFY_MODE)).thenReturn(ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY);
        lenient().when(this.configuration.get(ConfigKeys.GROUP_WEIGHTS)).thenReturn(Collections.emptyMap());
    }

    /*
     * Given the following inheritance setup:
     * (value in brackets is the group weight)
     *
     *   test user
     *   │
     *   ├── owner (13)
     *   │   └── admin (12)
     *   │       └── mod (11)
     *   │           └── helper (10)
     *   │               └── member (0)
     *   └── vip+ (6)
     *       └── vip (5)
     *           └── member (0)
     *
     * This test checks if the resolved inheritance order is correct :)
     */
    @ParameterizedTest(name = "[{index}] {0}, {1}")
    @CsvSource({
            "DEPTH_FIRST_PRE_ORDER,  false, 'owner -> admin -> mod -> helper -> member -> vip+ -> vip'",
            "BREADTH_FIRST,          false, 'owner -> vip+ -> admin -> vip -> mod -> member -> helper'",
            "DEPTH_FIRST_POST_ORDER, false, 'member -> helper -> mod -> admin -> owner -> vip -> vip+'",
            "DEPTH_FIRST_PRE_ORDER,  true,  'owner -> admin -> mod -> helper -> vip+ -> vip -> member'",
            "BREADTH_FIRST,          true,  'owner -> admin -> mod -> helper -> vip+ -> vip -> member'",
            "DEPTH_FIRST_POST_ORDER, true,  'owner -> admin -> mod -> helper -> vip+ -> vip -> member'"
    })
    public void testInheritanceTree(TraversalAlgorithm traversalAlgorithm, boolean postTraversalSort, String expected) {
        when(this.configuration.get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM)).thenReturn(traversalAlgorithm);
        when(this.configuration.get(ConfigKeys.POST_TRAVERSAL_INHERITANCE_SORT)).thenReturn(postTraversalSort);

        Group member = this.groupManager.getOrMake("member");

        Group helper = createGroup("helper", 10, member);
        Group mod = createGroup("mod", 11, helper);
        Group admin = createGroup("admin", 12, mod);
        Group owner = createGroup("owner", 13, admin);

        Group vip = createGroup("vip", 5, member);
        Group vipPlus = createGroup("vip+", 6, vip);

        PermissionHolder testHolder = this.groupManager.getOrMake("test");
        testHolder.normalData().add(Inheritance.builder().group(owner.getName()).build());
        testHolder.normalData().add(Inheritance.builder().group(vipPlus.getName()).build());

        List<String> groups = testHolder.resolveInheritanceTree(QueryOptionsImpl.DEFAULT_CONTEXTUAL)
                        .stream().map(Group::getName).collect(Collectors.toList());

        List<String> expectedList = Arrays.stream(expected.split(" -> ")).collect(Collectors.toList());
        assertEquals(expectedList, groups);
    }

    @Test
    public void testResolveInheritedNodes() {
        when(this.configuration.get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM)).thenReturn(TraversalAlgorithm.DEPTH_FIRST_PRE_ORDER);
        when(this.configuration.get(ConfigKeys.POST_TRAVERSAL_INHERITANCE_SORT)).thenReturn(false);

        Group member = this.groupManager.getOrMake("member");
        Group vip = createGroup("vip", 5, member);
        Group vipPlus = createGroup("vip+", 10, vip);

        Node testTrue = Permission.builder().permission("test").build();
        Node testFalse = Permission.builder().permission("test").value(false).build();
        PermissionNode testTrueWithContext = Permission.builder().permission("test").withContext("server", "test").build();

        member.setNode(DataType.NORMAL, testFalse, false);
        vip.setNode(DataType.NORMAL, testTrueWithContext, false);
        vipPlus.setNode(DataType.NORMAL, testTrue, false);

        PermissionHolder testHolder = this.groupManager.getOrMake("test");
        testHolder.setNode(DataType.NORMAL, Inheritance.builder().group(vipPlus.getName()).build(), false);

        assertEquals(
                List.of(testTrue, testFalse),
                testHolder.resolveInheritedNodes(QueryOptionsImpl.DEFAULT_CONTEXTUAL).stream()
                        .filter(node -> node.getKey().equals("test"))
                        .collect(Collectors.toList())
        );

        QueryOptions queryOptions = new QueryOptionsBuilderImpl(QueryMode.CONTEXTUAL).context(ImmutableContextSetImpl.of("server", "test")).build();
        assertEquals(
                List.of(testTrue, testTrueWithContext, testFalse),
                testHolder.resolveInheritedNodes(queryOptions).stream()
                        .filter(node -> node.getKey().equals("test"))
                        .collect(Collectors.toList())
        );
    }

    private Group createGroup(String name, int weight, Group parent) {
        Group group = this.groupManager.getOrMake(name);
        group.setNode(DataType.NORMAL, Inheritance.builder().group(parent.getName()).build(), false);
        group.setNode(DataType.NORMAL, Weight.builder().weight(weight).build(), false);
        return group;
    }

}
