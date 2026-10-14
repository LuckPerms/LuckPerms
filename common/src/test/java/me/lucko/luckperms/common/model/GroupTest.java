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
import me.lucko.luckperms.common.context.manager.ContextManager;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.node.types.DisplayName;
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

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GroupTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private LuckPermsConfiguration configuration;
    @Mock private ContextManager<?, ?> contextManager;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);

        //noinspection unchecked,rawtypes
        lenient().when(this.plugin.getContextManager()).thenReturn((ContextManager) this.contextManager);
    }

    @Test
    public void testGetWeightEmpty() {
        when(this.configuration.get(ConfigKeys.GROUP_WEIGHTS)).thenReturn(Map.of());

        OptionalInt res = new Group("test", this.plugin).getWeight();
        assertTrue(res.isEmpty());
    }

    @Test
    public void testGetWeightFromConfiguration() {
        when(this.configuration.get(ConfigKeys.GROUP_WEIGHTS)).thenReturn(Map.of("test", 10));

        OptionalInt res = new Group("test", this.plugin).getWeight();
        assertTrue(res.isPresent());
        assertEquals(10, res.getAsInt());
    }

    @Test
    public void testGetWeightFromNode() {
        when(this.configuration.get(ConfigKeys.CONTEXT_SATISFY_MODE)).thenReturn(ContextSatisfyMode.AT_LEAST_ONE_VALUE_PER_KEY);

        Group group = new Group("test", this.plugin);
        group.setNode(DataType.NORMAL, Weight.builder(3).build(), false);
        group.setNode(DataType.NORMAL, Weight.builder(5).build(), false);

        OptionalInt res = group.getWeight();
        assertTrue(res.isPresent());
        assertEquals(5, res.getAsInt());

        group.setNode(DataType.NORMAL, Weight.builder(100).build(), false);

        res = group.getWeight();
        assertTrue(res.isPresent());
        assertEquals(100, res.getAsInt());
    }

    @Test
    public void testGetDisplayNameEmpty() {
        when(this.contextManager.getStaticQueryOptions()).thenReturn(QueryOptionsImpl.DEFAULT_CONTEXTUAL);
        when(this.configuration.get(ConfigKeys.GROUP_NAME_REWRITES)).thenReturn(Map.of());

        Optional<String> displayName = new Group("test", this.plugin).getDisplayName();
        assertTrue(displayName.isEmpty());
    }

    @Test
    public void testGetDisplayNameFromConfiguration() {
        when(this.contextManager.getStaticQueryOptions()).thenReturn(QueryOptionsImpl.DEFAULT_CONTEXTUAL);
        when(this.configuration.get(ConfigKeys.GROUP_NAME_REWRITES)).thenReturn(Map.of("test", "Test"));

        Optional<String> displayName = new Group("test", this.plugin).getDisplayName();
        assertTrue(displayName.isPresent());
        assertEquals("Test", displayName.get());
    }

    @Test
    public void testGetDisplayNameFromNode() {
        when(this.contextManager.getStaticQueryOptions()).thenReturn(QueryOptionsImpl.DEFAULT_CONTEXTUAL);

        Group group = new Group("test", this.plugin);
        group.setNode(DataType.NORMAL, DisplayName.builder("TEST").build(), false);

        Optional<String> displayName = group.getDisplayName();
        assertTrue(displayName.isPresent());
        assertEquals("TEST", displayName.get());
    }
}
