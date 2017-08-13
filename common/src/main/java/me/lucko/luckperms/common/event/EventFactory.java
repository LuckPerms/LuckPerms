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

package me.lucko.luckperms.common.event;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.event.impl.EventConfigReload;
import me.lucko.luckperms.common.event.impl.EventGroupCreate;
import me.lucko.luckperms.common.event.impl.EventGroupDelete;
import me.lucko.luckperms.common.event.impl.EventGroupLoad;
import me.lucko.luckperms.common.event.impl.EventGroupLoadAll;
import me.lucko.luckperms.common.event.impl.EventLogBroadcast;
import me.lucko.luckperms.common.event.impl.EventLogPublish;
import me.lucko.luckperms.common.event.impl.EventNodeAdd;
import me.lucko.luckperms.common.event.impl.EventNodeClear;
import me.lucko.luckperms.common.event.impl.EventNodeRemove;
import me.lucko.luckperms.common.event.impl.EventPostSync;
import me.lucko.luckperms.common.event.impl.EventPreNetworkSync;
import me.lucko.luckperms.common.event.impl.EventPreSync;
import me.lucko.luckperms.common.event.impl.EventTrackAddGroup;
import me.lucko.luckperms.common.event.impl.EventTrackClear;
import me.lucko.luckperms.common.event.impl.EventTrackCreate;
import me.lucko.luckperms.common.event.impl.EventTrackDelete;
import me.lucko.luckperms.common.event.impl.EventTrackLoad;
import me.lucko.luckperms.common.event.impl.EventTrackLoadAll;
import me.lucko.luckperms.common.event.impl.EventTrackRemoveGroup;
import me.lucko.luckperms.common.event.impl.EventUserCacheLoad;
import me.lucko.luckperms.common.event.impl.EventUserDataRecalculate;
import me.lucko.luckperms.common.event.impl.EventUserDemote;
import me.lucko.luckperms.common.event.impl.EventUserFirstLogin;
import me.lucko.luckperms.common.event.impl.EventUserLoad;
import me.lucko.luckperms.common.event.impl.EventUserPromote;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public final class EventFactory {
    private final LuckPermsEventBus eventBus;

    private void fireEvent(LuckPermsEvent event) {
        eventBus.fireEventAsync(event);
    }

    public void handleGroupCreate(Group group, CreationCause cause) {
        EventGroupCreate event = new EventGroupCreate(group.getDelegate(), cause);
        fireEvent(event);
    }

    public void handleGroupDelete(Group group, DeletionCause cause) {
        EventGroupDelete event = new EventGroupDelete(group.getName(), ImmutableSet.copyOf(group.getEnduringNodes().values()), cause);
        fireEvent(event);
    }

    public void handleGroupLoadAll() {
        EventGroupLoadAll event = new EventGroupLoadAll();
        fireEvent(event);
    }

    public void handleGroupLoad(Group group) {
        EventGroupLoad event = new EventGroupLoad(group.getDelegate());
        fireEvent(event);
    }

    public boolean handleLogBroadcast(boolean initialState, LogEntry entry) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogBroadcast event = new EventLogBroadcast(cancel, entry);
        eventBus.fireEvent(event);
        return cancel.get();
    }

    public boolean handleLogPublish(boolean initialState, LogEntry entry) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogPublish event = new EventLogPublish(cancel, entry);
        eventBus.fireEvent(event);
        return cancel.get();
    }

    public void handleNodeAdd(Node node, PermissionHolder target, Collection<Node> before, Collection<Node> after) {
        EventNodeAdd event = new EventNodeAdd(node, target.getDelegate(), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEvent(event);
    }

    public void handleNodeClear(PermissionHolder target, Collection<Node> before, Collection<Node> after) {
        EventNodeClear event = new EventNodeClear(target.getDelegate(), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEvent(event);
    }

    public void handleNodeRemove(Node node, PermissionHolder target, Collection<Node> before, Collection<Node> after) {
        EventNodeRemove event = new EventNodeRemove(node, target.getDelegate(), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEvent(event);
    }

    public void handleConfigReload() {
        EventConfigReload event = new EventConfigReload();
        fireEvent(event);
    }

    public void handlePostSync() {
        EventPostSync event = new EventPostSync();
        fireEvent(event);
    }

    public boolean handleNetworkPreSync(boolean initialState, UUID id) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventPreNetworkSync event = new EventPreNetworkSync(cancel, id);
        eventBus.fireEvent(event);
        return cancel.get();
    }

    public boolean handlePreSync(boolean initialState) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventPreSync event = new EventPreSync(cancel);
        eventBus.fireEvent(event);
        return cancel.get();
    }

    public void handleTrackCreate(Track track, CreationCause cause) {
        EventTrackCreate event = new EventTrackCreate(track.getDelegate(), cause);
        fireEvent(event);
    }

    public void handleTrackDelete(Track track, DeletionCause cause) {
        EventTrackDelete event = new EventTrackDelete(track.getName(), ImmutableList.copyOf(track.getGroups()), cause);
        fireEvent(event);
    }

    public void handleTrackLoadAll() {
        EventTrackLoadAll event = new EventTrackLoadAll();
        fireEvent(event);
    }

    public void handleTrackLoad(Track track) {
        EventTrackLoad event = new EventTrackLoad(track.getDelegate());
        fireEvent(event);
    }

    public void handleTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        EventTrackAddGroup event = new EventTrackAddGroup(group, track.getDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after));
        fireEvent(event);
    }

    public void handleTrackClear(Track track, List<String> before) {
        EventTrackClear event = new EventTrackClear(track.getDelegate(), ImmutableList.copyOf(before), ImmutableList.of());
        fireEvent(event);
    }

    public void handleTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        EventTrackRemoveGroup event = new EventTrackRemoveGroup(group, track.getDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after));
        fireEvent(event);
    }

    public void handleUserCacheLoad(User user, UserData data) {
        EventUserCacheLoad event = new EventUserCacheLoad(user.getDelegate(), data);
        fireEvent(event);
    }

    public void handleUserDataRecalculate(User user, UserData data) {
        EventUserDataRecalculate event = new EventUserDataRecalculate(user.getDelegate(), data);
        fireEvent(event);
    }

    public void handleUserFirstLogin(UUID uuid, String username) {
        EventUserFirstLogin event = new EventUserFirstLogin(uuid, username);
        fireEvent(event);
    }

    public void handleUserLoad(User user) {
        EventUserLoad event = new EventUserLoad(user.getDelegate());
        fireEvent(event);
    }

    public void handleUserDemote(User user, Track track, String from, String to) {
        EventUserDemote event = new EventUserDemote(track.getDelegate(), user.getDelegate(), from, to);
        fireEvent(event);
    }

    public void handleUserPromote(User user, Track track, String from, String to) {
        EventUserPromote event = new EventUserPromote(track.getDelegate(), user.getDelegate(), from, to);
        fireEvent(event);
    }

}
