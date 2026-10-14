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
import me.lucko.luckperms.common.inheritance.InheritanceGraphFactory;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.track.DemotionResult;
import net.luckperms.api.track.PromotionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(PrimaryGroupHolder.Stored::new);
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

    @Test
    public void testPromoteThrowsOnSmallTrack() {
        Track track = new Track("test", this.plugin);
        track.setGroups(List.of("a"));

        User user = new User(UUID.randomUUID(), this.plugin);
        assertThrows(IllegalStateException.class, () ->
                track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true)
        );
    }

    @Test
    public void testPromoteAddedToFirst() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);

        // user isn't on the track at all, and addToFirst=false - no change made
        PromotionResult result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, false);
        assertEquals(PromotionResult.Status.ADDED_TO_FIRST_GROUP, result.getStatus());
        assertTrue(result.getGroupTo().isEmpty());
        assertTrue(user.normalData().inheritanceNodesInContext(ImmutableContextSetImpl.EMPTY).isEmpty());

        // user isn't on the track at all, and addToFirst=true - added to first group
        result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.ADDED_TO_FIRST_GROUP, result.getStatus());
        assertEquals("a", result.getGroupTo().orElse(null));
        assertTrue(userInheritsGroup(user, "a"));
    }

    @Test
    public void testPromoteSuccess() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        this.groupManager.getOrMake("c");
        track.setGroups(List.of("a", "b", "c"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);

        PromotionResult result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.SUCCESS, result.getStatus());
        assertEquals("a", result.getGroupFrom().orElse(null));
        assertEquals("b", result.getGroupTo().orElse(null));
        assertFalse(userInheritsGroup(user, "a"));
        assertTrue(userInheritsGroup(user, "b"));

        // promote again, to the end of the track
        result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.SUCCESS, result.getStatus());
        assertEquals("b", result.getGroupFrom().orElse(null));
        assertEquals("c", result.getGroupTo().orElse(null));
        assertFalse(userInheritsGroup(user, "b"));
        assertTrue(userInheritsGroup(user, "c"));

        // already at the end of the track
        result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.END_OF_TRACK, result.getStatus());
        assertTrue(userInheritsGroup(user, "c"));
    }

    @Test
    public void testPromoteAmbiguousCall() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);
        user.setNode(DataType.NORMAL, Inheritance.builder("b").build(), false);

        PromotionResult result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.AMBIGUOUS_CALL, result.getStatus());
    }

    @Test
    public void testPromoteMalformedTrack() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        // "b" is on the track but doesn't exist in the group manager
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);

        PromotionResult result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(PromotionResult.Status.MALFORMED_TRACK, result.getStatus());
        assertEquals("b", result.getGroupTo().orElse(null));
        assertFalse(userInheritsGroup(user, "b"));
    }

    @Test
    public void testPromoteUndefinedFailure() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);

        // permission checker denies the promotion
        PromotionResult result = track.promote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysFalse(), null, true);
        assertEquals(PromotionResult.Status.UNDEFINED_FAILURE, result.getStatus());
        assertTrue(userInheritsGroup(user, "a"));
        assertFalse(userInheritsGroup(user, "b"));
    }

    @Test
    public void testDemoteThrowsOnSmallTrack() {
        Track track = new Track("test", this.plugin);
        track.setGroups(List.of("a"));

        User user = new User(UUID.randomUUID(), this.plugin);
        assertThrows(IllegalStateException.class, () ->
                track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true)
        );
    }

    @Test
    public void testDemoteNotOnTrack() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);

        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(DemotionResult.Status.NOT_ON_TRACK, result.getStatus());
    }

    @Test
    public void testDemoteSuccess() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        this.groupManager.getOrMake("c");
        track.setGroups(List.of("a", "b", "c"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("c").build(), false);

        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(DemotionResult.Status.SUCCESS, result.getStatus());
        assertEquals("c", result.getGroupFrom().orElse(null));
        assertEquals("b", result.getGroupTo().orElse(null));
        assertFalse(userInheritsGroup(user, "c"));
        assertTrue(userInheritsGroup(user, "b"));
    }

    @Test
    public void testDemoteRemovedFromFirst() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);

        // removeFromFirst=false - no change made, but reports as if removed
        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, false);
        assertEquals(DemotionResult.Status.REMOVED_FROM_FIRST_GROUP, result.getStatus());
        assertNull(result.getGroupFrom().orElse(null));
        assertTrue(userInheritsGroup(user, "a"));

        // removeFromFirst=true - user is removed from the group entirely
        result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(DemotionResult.Status.REMOVED_FROM_FIRST_GROUP, result.getStatus());
        assertEquals("a", result.getGroupFrom().orElse(null));
        assertFalse(userInheritsGroup(user, "a"));
    }

    @Test
    public void testDemoteAmbiguousCall() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("a").build(), false);
        user.setNode(DataType.NORMAL, Inheritance.builder("b").build(), false);

        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(DemotionResult.Status.AMBIGUOUS_CALL, result.getStatus());
    }

    @Test
    public void testDemoteMalformedTrack() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("b");
        // "a" is on the track but doesn't exist in the group manager
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("b").build(), false);

        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysTrue(), null, true);
        assertEquals(DemotionResult.Status.MALFORMED_TRACK, result.getStatus());
        assertEquals("a", result.getGroupTo().orElse(null));
    }

    @Test
    public void testDemoteUndefinedFailure() {
        Track track = new Track("test", this.plugin);
        this.groupManager.getOrMake("a");
        this.groupManager.getOrMake("b");
        track.setGroups(List.of("a", "b"));

        User user = new User(UUID.randomUUID(), this.plugin);
        user.setNode(DataType.NORMAL, Inheritance.builder("b").build(), false);

        // permission checker denies the demotion
        DemotionResult result = track.demote(user, ImmutableContextSetImpl.EMPTY, Predicates.alwaysFalse(), null, true);
        assertEquals(DemotionResult.Status.UNDEFINED_FAILURE, result.getStatus());
        assertTrue(userInheritsGroup(user, "b"));
    }

    private static boolean userInheritsGroup(User user, String group) {
        for (InheritanceNode node : user.normalData().inheritanceNodesInContext(ImmutableContextSetImpl.EMPTY)) {
            if (node.getGroupName().equalsIgnoreCase(group)) {
                return true;
            }
        }
        return false;
    }

}
