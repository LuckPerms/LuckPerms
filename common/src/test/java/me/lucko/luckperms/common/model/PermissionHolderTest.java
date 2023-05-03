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
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.types.PermissionNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PermissionHolderTest {

    @Mock private LuckPermsPlugin plugin;

    @BeforeEach
    public void setupMocks() {
        when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
    }

    @Test
    public void testTemporaryMergeNone() {
        PermissionHolder holder = new Group("test", this.plugin);

        PermissionNode node1 = Permission.builder().permission("test").expiry(1, TimeUnit.HOURS).build();
        PermissionNode node2 = Permission.builder().permission("test").expiry(2, TimeUnit.HOURS).build();

        TemporaryNodeMergeStrategy strategy = TemporaryNodeMergeStrategy.NONE;

        DataMutateResult.WithMergedNode r1 = holder.setNode(DataType.NORMAL, node1, strategy);
        assertEquals(DataMutateResult.SUCCESS, r1.getResult());
        assertEquals(ImmutableList.of(node1), holder.normalData().asList());

        DataMutateResult.WithMergedNode r2 = holder.setNode(DataType.NORMAL, node2, strategy);
        assertEquals(DataMutateResult.FAIL_ALREADY_HAS, r2.getResult());
        assertEquals(ImmutableList.of(node1), holder.normalData().asList());
    }

    @Test
    public void testTemporaryMergeReplaceIfLonger() {
        PermissionHolder holder = new Group("test", this.plugin);

        PermissionNode node1 = Permission.builder().permission("test").expiry(1, TimeUnit.HOURS).build();
        PermissionNode node2 = Permission.builder().permission("test").expiry(2, TimeUnit.HOURS).build();

        TemporaryNodeMergeStrategy strategy = TemporaryNodeMergeStrategy.REPLACE_EXISTING_IF_DURATION_LONGER;

        DataMutateResult.WithMergedNode r1 = holder.setNode(DataType.NORMAL, node1, strategy);
        assertEquals(DataMutateResult.SUCCESS, r1.getResult());
        assertEquals(ImmutableList.of(node1), holder.normalData().asList());

        DataMutateResult.WithMergedNode r2 = holder.setNode(DataType.NORMAL, node2, strategy);
        assertEquals(DataMutateResult.SUCCESS, r2.getResult());
        assertEquals(ImmutableList.of(node2), holder.normalData().asList());
        assertEquals(node2, r2.getMergedNode());

        DataMutateResult.WithMergedNode r3 = holder.setNode(DataType.NORMAL, node1, strategy);
        assertEquals(DataMutateResult.FAIL_ALREADY_HAS, r3.getResult());
        assertEquals(ImmutableList.of(node2), holder.normalData().asList());
    }

    @Test
    public void testTemporaryMergeAddDurations() {
        PermissionHolder holder = new Group("test", this.plugin);

        PermissionNode node1 = Permission.builder().permission("test").expiry(2, TimeUnit.HOURS).build();
        PermissionNode node2 = Permission.builder().permission("test").expiry(1, TimeUnit.HOURS).build();

        TemporaryNodeMergeStrategy strategy = TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING;

        DataMutateResult.WithMergedNode r1 = holder.setNode(DataType.NORMAL, node1, strategy);
        assertEquals(DataMutateResult.SUCCESS, r1.getResult());
        assertEquals(ImmutableList.of(node1), holder.normalData().asList());

        DataMutateResult.WithMergedNode r2 = holder.setNode(DataType.NORMAL, node2, strategy);
        assertEquals(DataMutateResult.SUCCESS, r2.getResult());

        Instant originalExpiry = node1.getExpiry();
        assertNotNull(originalExpiry);

        Instant newExpiry = r2.getMergedNode().getExpiry();
        assertNotNull(newExpiry);

        Instant expectedExpiry = node1.getExpiry().plus(1, ChronoUnit.HOURS);

        // uses wall-clock time, so allow for the tests to run slowly
        assertTrue(Duration.between(newExpiry, expectedExpiry).abs().getSeconds() < 5);
    }

}
