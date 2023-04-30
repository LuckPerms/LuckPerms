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

import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GroupManagerTest {

    @Mock private LuckPermsPlugin plugin;

    @Test
    public void testSanitizeIdentifier() {
        StandardGroupManager manager = new StandardGroupManager(this.plugin) {
            @Override
            public Group apply(String name) {
                Group group = mock(Group.class);
                when(group.getName()).thenReturn(name);
                return group;
            }
        };

        Group group = manager.getOrMake("DEFAULT");
        assertEquals("default", group.getName());
        assertEquals(ImmutableSet.of("default"), manager.getAll().keySet());
    }

    @Test
    public void testGetByDisplayName() {
        StandardGroupManager manager = new StandardGroupManager(this.plugin) {
            @Override
            public Group apply(String name) {
                return mock(Group.class);
            }
        };

        Group defaultGroup = manager.getOrMake("default");
        when(defaultGroup.getDisplayName()).thenReturn(Optional.of("member"));

        assertSame(defaultGroup, manager.getByDisplayName("default"));
        assertSame(defaultGroup, manager.getByDisplayName("Default"));
        assertSame(defaultGroup, manager.getByDisplayName("member"));
        assertSame(defaultGroup, manager.getByDisplayName("Member"));
        assertNull(manager.getByDisplayName("test"));

        Group memberGroup = manager.getOrMake("member");

        assertSame(defaultGroup, manager.getByDisplayName("default"));
        assertSame(defaultGroup, manager.getByDisplayName("Default"));
        assertSame(memberGroup, manager.getByDisplayName("member"));
        assertSame(memberGroup, manager.getByDisplayName("Member"));
    }

}
