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
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.graph.TraversalAlgorithm;
import me.lucko.luckperms.common.inheritance.InheritanceGraphFactory;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Weight;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.model.data.DataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PrimaryGroupHolderTest {

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
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(u -> null);
        lenient().when(this.configuration.get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM)).thenReturn(TraversalAlgorithm.BREADTH_FIRST);
    }

    @Test
    public void testStored() {
        User user = new User(UUID.randomUUID(), this.plugin);

        // all holders inherit from stored, and should behave the same in the absence of any nodes
        List<PrimaryGroupHolder> holders = List.of(
                new PrimaryGroupHolder.Stored(user),
                new PrimaryGroupHolder.AllParentsByWeight(user),
                new PrimaryGroupHolder.ParentsByWeight(user)
        );

        for (PrimaryGroupHolder holder : holders) {
            // empty
            assertEquals(Optional.empty(), holder.getStoredValue());
            assertNull(holder.calculateValue(QueryOptionsImpl.DEFAULT_CONTEXTUAL));

            // set value
            holder.setStoredValue("test");
            assertEquals(Optional.of("test"), holder.getStoredValue());
            assertEquals("test", holder.calculateValue(QueryOptionsImpl.DEFAULT_CONTEXTUAL));
        }
    }

    @Test
    public void testParentsByWeight() {
        User user = new User(UUID.randomUUID(), this.plugin);

        Group special = createGroup("special", 100, null); // parent of mod, but not inherited directly
        Group mod = createGroup("mod", 5, special);
        Group admin = createGroup("admin", 10, mod);

        user.setNode(DataType.NORMAL, Inheritance.builder("mod").build(), false);
        user.setNode(DataType.NORMAL, Inheritance.builder("admin").build(), false);

        PrimaryGroupHolder holder = new PrimaryGroupHolder.AllParentsByWeight(user);
        assertEquals("special", holder.calculateValue(QueryOptionsImpl.DEFAULT_CONTEXTUAL));

        holder = new PrimaryGroupHolder.ParentsByWeight(user);
        assertEquals("admin", holder.calculateValue(QueryOptionsImpl.DEFAULT_CONTEXTUAL));
    }

    private Group createGroup(String name, int weight, Group parent) {
        Group group = this.groupManager.getOrMake(name);
        if (parent != null) {
            group.normalData().add(Inheritance.builder().group(parent.getName()).build());
        }
        group.normalData().add(Weight.builder().weight(weight).build());
        return group;
    }

}
