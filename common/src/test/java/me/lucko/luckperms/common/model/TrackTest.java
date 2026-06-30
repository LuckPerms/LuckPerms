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

import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.inheritance.InheritanceGraphFactory;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.model.data.DataMutateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class TrackTest {

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
    }

    @Test
    public void testGetRelative() {
        Track track = new Track("test", this.plugin);

        Group a = this.groupManager.getOrMake("a");
        Group b = this.groupManager.getOrMake("b");
        Group c = this.groupManager.getOrMake("c");

        track.setGroups(List.of("a", "b", "c"));
        assertEquals(List.of("a", "b", "c"), track.getGroups());

        // next
        assertEquals("b", track.getNext(a));
        assertEquals("c", track.getNext(b));
        assertNull(track.getNext(c));
        assertThrows(IllegalArgumentException.class, () -> track.getNext("missing"));

        // previous
        assertEquals("b", track.getPrevious(c));
        assertEquals("a", track.getPrevious(b));
        assertNull(track.getPrevious(a));
        assertThrows(IllegalArgumentException.class, () -> track.getPrevious("missing"));
    }

    @Test
    public void testAppendInsertRemove() {
        Track track = new Track("test", this.plugin);

        Group a = this.groupManager.getOrMake("a");
        Group b = this.groupManager.getOrMake("b");
        Group c = this.groupManager.getOrMake("c");
        Group d = this.groupManager.getOrMake("d");

        track.setGroups(List.of("a", "b"));
        assertEquals(List.of("a", "b"), track.getGroups());

        // append
        DataMutateResult res = track.appendGroup(a);
        assertEquals(DataMutateResult.FAIL_ALREADY_HAS, res);

        res = track.appendGroup(c);
        assertEquals(DataMutateResult.SUCCESS, res);
        assertEquals(List.of("a", "b", "c"), track.getGroups());

        // insert
        res = track.insertGroup(d, 1);
        assertEquals(DataMutateResult.SUCCESS, res);
        assertEquals(List.of("a", "d", "b", "c"), track.getGroups());

        // remove
        res = track.removeGroup(b);
        assertEquals(DataMutateResult.SUCCESS, res);
        assertEquals(List.of("a", "d", "c"), track.getGroups());
    }

}
