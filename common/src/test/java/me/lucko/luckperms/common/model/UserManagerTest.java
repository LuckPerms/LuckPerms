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

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UserManagerTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private LuckPermsBootstrap bootstrap;
    @Mock private LuckPermsConfiguration configuration;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.plugin.getBootstrap()).thenReturn(this.bootstrap);
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.bootstrap.getScheduler()).thenReturn(mock(SchedulerAdapter.class));
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(PrimaryGroupHolder.AllParentsByWeight::new);
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD)).thenReturn("parents-by-weight");
    }

    @Test
    public void testGiveDefaultIfNeeded() {
        StandardUserManager manager = new StandardUserManager(this.plugin);

        User user = manager.getOrMake(UUID.randomUUID());
        assertEquals(ImmutableList.of(), user.normalData().asList());

        boolean changed = manager.giveDefaultIfNeeded(user);
        assertTrue(changed);

        Inheritance defaultNode = Inheritance.builder("default").build();
        assertEquals(ImmutableList.of(defaultNode), user.normalData().asList());

        changed = manager.giveDefaultIfNeeded(user);
        assertFalse(changed);
    }

    @Test
    public void testIsNonDefaultUser() {
        StandardUserManager manager = new StandardUserManager(this.plugin);
        User user = manager.getOrMake(UUID.randomUUID());
        manager.giveDefaultIfNeeded(user);

        assertFalse(manager.isNonDefaultUser(user));

        user.normalData().add(Permission.builder().permission("test").build());
        assertTrue(manager.isNonDefaultUser(user));
    }

    @Test
    public void testIsDefaultNode() {
        StandardUserManager manager = new StandardUserManager(this.plugin);

        assertTrue(manager.isDefaultNode(Inheritance.builder().group("default").build()));
        assertTrue(manager.isDefaultNode(Inheritance.builder().group("Default").build()));

        assertFalse(manager.isDefaultNode(Inheritance.builder().group("default").value(false).build()));
        assertFalse(manager.isDefaultNode(Inheritance.builder().group("default").withContext("server", "test").build()));
        assertFalse(manager.isDefaultNode(Inheritance.builder().group("default").expiry(1, TimeUnit.DAYS).build()));
        assertFalse(manager.isDefaultNode(Inheritance.builder().group("test").build()));
        assertFalse(manager.isDefaultNode(Permission.builder().permission("hello").build()));
    }

}
