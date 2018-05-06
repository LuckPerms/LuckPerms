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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.api.caching.GroupData;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.api.event.log.LogNotifyEvent;
import me.lucko.luckperms.api.event.source.Source;
import me.lucko.luckperms.common.api.delegates.model.ApiPermissionHolder;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.event.impl.EventConfigReload;
import me.lucko.luckperms.common.event.impl.EventGroupCacheLoad;
import me.lucko.luckperms.common.event.impl.EventGroupCreate;
import me.lucko.luckperms.common.event.impl.EventGroupDataRecalculate;
import me.lucko.luckperms.common.event.impl.EventGroupDelete;
import me.lucko.luckperms.common.event.impl.EventGroupLoad;
import me.lucko.luckperms.common.event.impl.EventGroupLoadAll;
import me.lucko.luckperms.common.event.impl.EventLogBroadcast;
import me.lucko.luckperms.common.event.impl.EventLogNetworkPublish;
import me.lucko.luckperms.common.event.impl.EventLogNotify;
import me.lucko.luckperms.common.event.impl.EventLogPublish;
import me.lucko.luckperms.common.event.impl.EventLogReceive;
import me.lucko.luckperms.common.event.impl.EventNodeAdd;
import me.lucko.luckperms.common.event.impl.EventNodeClear;
import me.lucko.luckperms.common.event.impl.EventNodeRemove;
import me.lucko.luckperms.common.event.impl.EventPlayerDataSave;
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
import me.lucko.luckperms.common.event.impl.EventUserLoginProcess;
import me.lucko.luckperms.common.event.impl.EventUserPromote;
import me.lucko.luckperms.common.event.model.EntitySourceImpl;
import me.lucko.luckperms.common.event.model.SenderEntity;
import me.lucko.luckperms.common.event.model.UnknownSource;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.sender.Sender;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

public final class EventFactory {
    private final AbstractEventBus eventBus;

    public EventFactory(AbstractEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public AbstractEventBus getEventBus() {
        return this.eventBus;
    }

    private void fireEventAsync(LuckPermsEvent event) {
        this.eventBus.fireEventAsync(event);
    }

    private void fireEvent(LuckPermsEvent event) {
        this.eventBus.fireEvent(event);
    }

    public void handleGroupCacheLoad(Group group, GroupData data) {
        EventGroupCacheLoad event = new EventGroupCacheLoad(group.getApiDelegate(), data);
        fireEventAsync(event);
    }

    public void handleGroupCreate(Group group, CreationCause cause) {
        EventGroupCreate event = new EventGroupCreate(group.getApiDelegate(), cause);
        fireEventAsync(event);
    }

    public void handleGroupDelete(Group group, DeletionCause cause) {
        EventGroupDelete event = new EventGroupDelete(group.getName(), ImmutableSet.copyOf(group.enduringData().immutable().values()), cause);
        fireEventAsync(event);
    }

    public void handleGroupLoadAll() {
        EventGroupLoadAll event = new EventGroupLoadAll();
        fireEventAsync(event);
    }

    public void handleGroupLoad(Group group) {
        EventGroupLoad event = new EventGroupLoad(group.getApiDelegate());
        fireEventAsync(event);
    }

    public boolean handleLogBroadcast(boolean initialState, LogEntry entry, LogBroadcastEvent.Origin origin) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogBroadcast event = new EventLogBroadcast(cancel, entry, origin);
        fireEvent(event);
        return cancel.get();
    }

    public boolean handleLogPublish(boolean initialState, LogEntry entry) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogPublish event = new EventLogPublish(cancel, entry);
        fireEvent(event);
        return cancel.get();
    }

    public boolean handleLogNetworkPublish(boolean initialState, UUID id, LogEntry entry) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogNetworkPublish event = new EventLogNetworkPublish(cancel, id, entry);
        fireEvent(event);
        return cancel.get();
    }

    public boolean handleLogNotify(boolean initialState, LogEntry entry, LogNotifyEvent.Origin origin, Sender sender) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventLogNotify event = new EventLogNotify(cancel, entry, origin, sender);
        fireEvent(event);
        return cancel.get();
    }

    public void handleLogReceive(UUID id, LogEntry entry) {
        EventLogReceive event = new EventLogReceive(id, entry);
        fireEventAsync(event);
    }

    public void handleNodeAdd(Node node, PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        EventNodeAdd event = new EventNodeAdd(node, getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEventAsync(event);
    }

    public void handleNodeClear(PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        EventNodeClear event = new EventNodeClear(getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEventAsync(event);
    }

    public void handleNodeRemove(Node node, PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        EventNodeRemove event = new EventNodeRemove(node, getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after));
        fireEventAsync(event);
    }

    public void handleConfigReload() {
        EventConfigReload event = new EventConfigReload();
        fireEventAsync(event);
    }

    public void handlePostSync() {
        EventPostSync event = new EventPostSync();
        fireEventAsync(event);
    }

    public boolean handleNetworkPreSync(boolean initialState, UUID id) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventPreNetworkSync event = new EventPreNetworkSync(cancel, id);
        fireEvent(event);
        return cancel.get();
    }

    public boolean handlePreSync(boolean initialState) {
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        EventPreSync event = new EventPreSync(cancel);
        fireEvent(event);
        return cancel.get();
    }

    public void handleTrackCreate(Track track, CreationCause cause) {
        EventTrackCreate event = new EventTrackCreate(track.getApiDelegate(), cause);
        fireEventAsync(event);
    }

    public void handleTrackDelete(Track track, DeletionCause cause) {
        EventTrackDelete event = new EventTrackDelete(track.getName(), ImmutableList.copyOf(track.getGroups()), cause);
        fireEventAsync(event);
    }

    public void handleTrackLoadAll() {
        EventTrackLoadAll event = new EventTrackLoadAll();
        fireEventAsync(event);
    }

    public void handleTrackLoad(Track track) {
        EventTrackLoad event = new EventTrackLoad(track.getApiDelegate());
        fireEventAsync(event);
    }

    public void handleTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        EventTrackAddGroup event = new EventTrackAddGroup(group, track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after));
        fireEventAsync(event);
    }

    public void handleTrackClear(Track track, List<String> before) {
        EventTrackClear event = new EventTrackClear(track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.of());
        fireEventAsync(event);
    }

    public void handleTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        EventTrackRemoveGroup event = new EventTrackRemoveGroup(group, track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after));
        fireEventAsync(event);
    }

    public void handleUserCacheLoad(User user, UserData data) {
        EventUserCacheLoad event = new EventUserCacheLoad(new ApiUser(user), data);
        fireEventAsync(event);
    }

    public void handleDataRecalculate(PermissionHolder holder) {
        if (holder.getType().isUser()) {
            User user = (User) holder;
            EventUserDataRecalculate event = new EventUserDataRecalculate(user.getApiDelegate(), user.getCachedData());
            fireEventAsync(event);
        } else {
            Group group = (Group) holder;
            EventGroupDataRecalculate event = new EventGroupDataRecalculate(group.getApiDelegate(), group.getCachedData());
            fireEventAsync(event);
        }
    }

    public void handleUserFirstLogin(UUID uuid, String username) {
        EventUserFirstLogin event = new EventUserFirstLogin(uuid, username);
        fireEventAsync(event);
    }

    public void handlePlayerDataSave(UUID uuid, String username, PlayerSaveResult result) {
        EventPlayerDataSave event = new EventPlayerDataSave(uuid, username, result);
        fireEventAsync(event);
    }

    public void handleUserLoad(User user) {
        EventUserLoad event = new EventUserLoad(new ApiUser(user));
        fireEventAsync(event);
    }

    public void handleUserLoginProcess(UUID uuid, String username, User user) {
        EventUserLoginProcess event = new EventUserLoginProcess(uuid, username, new ApiUser(user));
        fireEvent(event);
    }

    public void handleUserDemote(User user, Track track, String from, String to, @Nullable Sender source) {
        Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderEntity(source));
        EventUserDemote event = new EventUserDemote(track.getApiDelegate(), new ApiUser(user), from, to, s);
        fireEventAsync(event);
    }

    public void handleUserPromote(User user, Track track, String from, String to, @Nullable Sender source) {
        Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderEntity(source));
        EventUserPromote event = new EventUserPromote(track.getApiDelegate(), new ApiUser(user), from, to, s);
        fireEventAsync(event);
    }

    private static ApiPermissionHolder getDelegate(PermissionHolder holder) {
        if (holder instanceof Group) {
            return ((Group) holder).getApiDelegate();
        } else if (holder instanceof User) {
            return new ApiUser(((User) holder));
        } else {
            throw new AssertionError();
        }
    }

}
