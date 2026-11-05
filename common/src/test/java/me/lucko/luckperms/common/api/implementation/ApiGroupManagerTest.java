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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.storage.Storage;
import net.luckperms.api.event.cause.CreationCause;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ApiGroupManagerTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private Consumer<net.luckperms.api.model.group.Group> action;

    @ParameterizedTest
    @ValueSource(strings = {"", "test group", " group", "group ", "abcdefghijklmnopqrstuvwxyz01234567890"})
    public void testModifyGroupRejectsInvalidName(String name) {
        ApiGroupManager manager = new ApiGroupManager(this.plugin, null);

        assertThrows(IllegalArgumentException.class, () -> manager.modifyGroup(name, this.action));
        verifyNoInteractions(this.plugin, this.action);
    }

    @Test
    public void testModifyGroupRejectsNullArguments() {
        ApiGroupManager manager = new ApiGroupManager(this.plugin, null);

        assertThrows(NullPointerException.class, () -> manager.modifyGroup(null, this.action));
        assertThrows(NullPointerException.class, () -> manager.modifyGroup("test", null));
        verifyNoInteractions(this.plugin, this.action);
    }

    @ParameterizedTest
    @ValueSource(strings = {"test-group", "Test-Group"})
    public void testModifyGroupSavesChanges(String name) {
        Storage storage = mock(Storage.class);
        LuckPermsBootstrap bootstrap = mock(LuckPermsBootstrap.class);
        SchedulerAdapter scheduler = mock(SchedulerAdapter.class);
        Group group = mock(Group.class);
        ApiGroup proxy = new ApiGroup(group);
        when(this.plugin.getStorage()).thenReturn(storage);
        when(this.plugin.getBootstrap()).thenReturn(bootstrap);
        when(bootstrap.getScheduler()).thenReturn(scheduler);
        when(scheduler.async()).thenReturn(Runnable::run);
        when(storage.createAndLoadGroup("test-group", CreationCause.API)).thenReturn(CompletableFuture.completedFuture(group));
        when(group.getApiProxy()).thenReturn(proxy);
        when(storage.saveGroup(group)).thenReturn(CompletableFuture.completedFuture(null));

        ApiGroupManager manager = new ApiGroupManager(this.plugin, null);
        manager.modifyGroup(name, this.action).join();

        verify(this.action).accept(proxy);
        verify(storage).saveGroup(group);
    }
}
