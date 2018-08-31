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
import me.lucko.luckperms.api.event.Cancellable;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.api.event.group.GroupCacheLoadEvent;
import me.lucko.luckperms.api.event.group.GroupCreateEvent;
import me.lucko.luckperms.api.event.group.GroupDataRecalculateEvent;
import me.lucko.luckperms.api.event.group.GroupDeleteEvent;
import me.lucko.luckperms.api.event.group.GroupLoadAllEvent;
import me.lucko.luckperms.api.event.group.GroupLoadEvent;
import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.api.event.log.LogNetworkPublishEvent;
import me.lucko.luckperms.api.event.log.LogNotifyEvent;
import me.lucko.luckperms.api.event.log.LogPublishEvent;
import me.lucko.luckperms.api.event.log.LogReceiveEvent;
import me.lucko.luckperms.api.event.node.NodeAddEvent;
import me.lucko.luckperms.api.event.node.NodeClearEvent;
import me.lucko.luckperms.api.event.node.NodeRemoveEvent;
import me.lucko.luckperms.api.event.player.PlayerDataSaveEvent;
import me.lucko.luckperms.api.event.source.Source;
import me.lucko.luckperms.api.event.sync.ConfigReloadEvent;
import me.lucko.luckperms.api.event.sync.PostSyncEvent;
import me.lucko.luckperms.api.event.sync.PreNetworkSyncEvent;
import me.lucko.luckperms.api.event.sync.PreSyncEvent;
import me.lucko.luckperms.api.event.track.TrackCreateEvent;
import me.lucko.luckperms.api.event.track.TrackDeleteEvent;
import me.lucko.luckperms.api.event.track.TrackLoadAllEvent;
import me.lucko.luckperms.api.event.track.TrackLoadEvent;
import me.lucko.luckperms.api.event.track.mutate.TrackAddGroupEvent;
import me.lucko.luckperms.api.event.track.mutate.TrackClearEvent;
import me.lucko.luckperms.api.event.track.mutate.TrackRemoveGroupEvent;
import me.lucko.luckperms.api.event.user.UserCacheLoadEvent;
import me.lucko.luckperms.api.event.user.UserDataRecalculateEvent;
import me.lucko.luckperms.api.event.user.UserFirstLoginEvent;
import me.lucko.luckperms.api.event.user.UserLoadEvent;
import me.lucko.luckperms.api.event.user.UserLoginProcessEvent;
import me.lucko.luckperms.api.event.user.track.UserDemoteEvent;
import me.lucko.luckperms.api.event.user.track.UserPromoteEvent;
import me.lucko.luckperms.common.api.delegates.model.ApiPermissionHolder;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.event.gen.GeneratedEventSpec;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import javax.annotation.Nullable;

public final class EventFactory {
    private final AbstractEventBus<?> eventBus;

    public EventFactory(AbstractEventBus<?> eventBus) {
        this.eventBus = eventBus;
    }

    public AbstractEventBus getEventBus() {
        return this.eventBus;
    }

    private boolean shouldPost(Class<? extends LuckPermsEvent> eventClass){
        return this.eventBus.shouldPost(eventClass);
    }

    private void post(LuckPermsEvent event) {
        this.eventBus.post(event);
    }

    private <T extends LuckPermsEvent> void post(Class<T> eventClass, Supplier<T> supplier) {
        if (Cancellable.class.isAssignableFrom(eventClass)) {
            throw new RuntimeException("Cancellable event cannot be posted async (" + eventClass + ")");
        }

        this.eventBus.getPlugin().getBootstrap().getScheduler().executeAsync(() -> {
            if (!shouldPost(eventClass)) {
                return;
            }
            T event = supplier.get();
            this.eventBus.post(event);
        });
    }
    
    @SuppressWarnings("unchecked")
    private <T extends LuckPermsEvent> T generate(Class<T> eventClass, Object... params) {
        return (T) GeneratedEventSpec.lookup(eventClass).newInstance(this.eventBus.getApiProvider(), params);
    }

    public void handleGroupCacheLoad(Group group, GroupData data) {
        post(GroupCacheLoadEvent.class, () -> generate(GroupCacheLoadEvent.class, group.getApiDelegate(), data));
    }

    public void handleGroupCreate(Group group, CreationCause cause) {
        post(GroupCreateEvent.class, () -> generate(GroupCreateEvent.class, group.getApiDelegate(), cause));
    }

    public void handleGroupDelete(Group group, DeletionCause cause) {
        post(GroupDeleteEvent.class, () -> generate(GroupDeleteEvent.class, group.getName(), ImmutableSet.copyOf(group.enduringData().immutable().values()), cause));
    }

    public void handleGroupLoadAll() {
        post(GroupLoadAllEvent.class, () -> generate(GroupLoadAllEvent.class));
    }

    public void handleGroupLoad(Group group) {
        post(GroupLoadEvent.class, () -> generate(GroupLoadEvent.class, group.getApiDelegate()));
    }

    public boolean handleLogBroadcast(boolean initialState, LogEntry entry, LogBroadcastEvent.Origin origin) {
        if (!shouldPost(LogBroadcastEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogBroadcastEvent.class, cancel, entry, origin));
        return cancel.get();
    }

    public boolean handleLogPublish(boolean initialState, LogEntry entry) {
        if (!shouldPost(LogPublishEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogPublishEvent.class, cancel, entry));
        return cancel.get();
    }

    public boolean handleLogNetworkPublish(boolean initialState, UUID id, LogEntry entry) {
        if (!shouldPost(LogNetworkPublishEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogNetworkPublishEvent.class, cancel, id, entry));
        return cancel.get();
    }

    public boolean handleLogNotify(boolean initialState, LogEntry entry, LogNotifyEvent.Origin origin, Sender sender) {
        if (!shouldPost(LogNotifyEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogNotifyEvent.class, cancel, entry, origin, new SenderEntity(sender)));
        return cancel.get();
    }

    public void handleLogReceive(UUID id, LogEntry entry) {
        post(LogReceiveEvent.class, () -> generate(LogReceiveEvent.class, id, entry));
    }

    public void handleNodeAdd(Node node, PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeAddEvent.class, () -> generate(NodeAddEvent.class, getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after), node));
    }

    public void handleNodeClear(PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeClearEvent.class, () -> generate(NodeClearEvent.class, getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after)));
    }

    public void handleNodeRemove(Node node, PermissionHolder target, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeRemoveEvent.class, () -> generate(NodeRemoveEvent.class, getDelegate(target), ImmutableSet.copyOf(before), ImmutableSet.copyOf(after), node));
    }

    public void handleConfigReload() {
        post(ConfigReloadEvent.class, () -> generate(ConfigReloadEvent.class));
    }

    public void handlePostSync() {
        post(PostSyncEvent.class, () -> generate(PostSyncEvent.class));
    }

    public boolean handleNetworkPreSync(boolean initialState, UUID id) {
        if (!shouldPost(PreNetworkSyncEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(PreNetworkSyncEvent.class, cancel, id));
        return cancel.get();
    }

    public boolean handlePreSync(boolean initialState) {
        if (!shouldPost(PreSyncEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(PreSyncEvent.class, cancel));
        return cancel.get();
    }

    public void handleTrackCreate(Track track, CreationCause cause) {
        post(TrackCreateEvent.class, () -> generate(TrackCreateEvent.class, track.getApiDelegate(), cause));
    }

    public void handleTrackDelete(Track track, DeletionCause cause) {
        post(TrackDeleteEvent.class, () -> generate(TrackDeleteEvent.class, track.getName(), ImmutableList.copyOf(track.getGroups()), cause));
    }

    public void handleTrackLoadAll() {
        post(TrackLoadAllEvent.class, () -> generate(TrackLoadAllEvent.class));
    }

    public void handleTrackLoad(Track track) {
        post(TrackLoadEvent.class, () -> generate(TrackLoadEvent.class, track.getApiDelegate()));
    }

    public void handleTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        post(TrackAddGroupEvent.class, () -> generate(TrackAddGroupEvent.class, track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group));
    }

    public void handleTrackClear(Track track, List<String> before) {
        post(TrackClearEvent.class, () -> generate(TrackClearEvent.class, track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.of()));
    }

    public void handleTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        post(TrackRemoveGroupEvent.class, () -> generate(TrackRemoveGroupEvent.class, track.getApiDelegate(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group));
    }

    public void handleUserCacheLoad(User user, UserData data) {
        post(UserCacheLoadEvent.class, () -> generate(UserCacheLoadEvent.class, new ApiUser(user), data));
    }

    public void handleDataRecalculate(PermissionHolder holder) {
        if (holder.getType().isUser()) {
            User user = (User) holder;
            post(UserDataRecalculateEvent.class, () -> generate(UserDataRecalculateEvent.class, user.getApiDelegate(), user.getCachedData()));
        } else {
            Group group = (Group) holder;
            post(GroupDataRecalculateEvent.class, () -> generate(GroupDataRecalculateEvent.class, group.getApiDelegate(), group.getCachedData()));
        }
    }

    public void handleUserFirstLogin(UUID uuid, String username) {
        post(UserFirstLoginEvent.class, () -> generate(UserFirstLoginEvent.class, uuid, username));
    }

    public void handlePlayerDataSave(UUID uuid, String username, PlayerSaveResult result) {
        post(PlayerDataSaveEvent.class, () -> generate(PlayerDataSaveEvent.class, uuid, username, result));
    }

    public void handleUserLoad(User user) {
        post(UserLoadEvent.class, () -> generate(UserLoadEvent.class, new ApiUser(user)));
    }

    public void handleUserLoginProcess(UUID uuid, String username, User user) {
        post(UserLoginProcessEvent.class, () -> generate(UserLoginProcessEvent.class, uuid, username, new ApiUser(user)));
    }

    public void handleUserDemote(User user, Track track, String from, String to, @Nullable Sender source) {
        post(UserDemoteEvent.class, () -> {
            Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderEntity(source));
            return generate(UserDemoteEvent.class, s, track.getApiDelegate(), new ApiUser(user), Optional.ofNullable(from), Optional.ofNullable(to));
        });
    }

    public void handleUserPromote(User user, Track track, String from, String to, @Nullable Sender source) {
        post(UserPromoteEvent.class, () -> {
            Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderEntity(source));
            return generate(UserPromoteEvent.class, s, track.getApiDelegate(), new ApiUser(user), Optional.ofNullable(from), Optional.ofNullable(to));
        });
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
