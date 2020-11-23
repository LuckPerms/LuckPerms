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

import me.lucko.luckperms.common.api.implementation.ApiPermissionHolder;
import me.lucko.luckperms.common.cacheddata.GroupCachedDataManager;
import me.lucko.luckperms.common.cacheddata.UserCachedDataManager;
import me.lucko.luckperms.common.event.gen.GeneratedEventClass;
import me.lucko.luckperms.common.event.model.EntitySourceImpl;
import me.lucko.luckperms.common.event.model.SenderPlatformEntity;
import me.lucko.luckperms.common.event.model.UnknownSource;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.sender.Sender;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.event.context.ContextUpdateEvent;
import net.luckperms.api.event.extension.ExtensionLoadEvent;
import net.luckperms.api.event.group.GroupCacheLoadEvent;
import net.luckperms.api.event.group.GroupCreateEvent;
import net.luckperms.api.event.group.GroupDataRecalculateEvent;
import net.luckperms.api.event.group.GroupDeleteEvent;
import net.luckperms.api.event.group.GroupLoadAllEvent;
import net.luckperms.api.event.group.GroupLoadEvent;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.log.LogNetworkPublishEvent;
import net.luckperms.api.event.log.LogNotifyEvent;
import net.luckperms.api.event.log.LogPublishEvent;
import net.luckperms.api.event.log.LogReceiveEvent;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeClearEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.player.PlayerDataSaveEvent;
import net.luckperms.api.event.player.PlayerLoginProcessEvent;
import net.luckperms.api.event.player.lookup.UniqueIdDetermineTypeEvent;
import net.luckperms.api.event.player.lookup.UniqueIdLookupEvent;
import net.luckperms.api.event.player.lookup.UsernameLookupEvent;
import net.luckperms.api.event.player.lookup.UsernameValidityCheckEvent;
import net.luckperms.api.event.source.Source;
import net.luckperms.api.event.sync.ConfigReloadEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import net.luckperms.api.event.sync.PreSyncEvent;
import net.luckperms.api.event.track.TrackCreateEvent;
import net.luckperms.api.event.track.TrackDeleteEvent;
import net.luckperms.api.event.track.TrackLoadAllEvent;
import net.luckperms.api.event.track.TrackLoadEvent;
import net.luckperms.api.event.track.mutate.TrackAddGroupEvent;
import net.luckperms.api.event.track.mutate.TrackClearEvent;
import net.luckperms.api.event.track.mutate.TrackRemoveGroupEvent;
import net.luckperms.api.event.type.Cancellable;
import net.luckperms.api.event.user.UserCacheLoadEvent;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.event.user.UserFirstLoginEvent;
import net.luckperms.api.event.user.UserLoadEvent;
import net.luckperms.api.event.user.track.UserDemoteEvent;
import net.luckperms.api.event.user.track.UserPromoteEvent;
import net.luckperms.api.extension.Extension;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class EventDispatcher {
    private final AbstractEventBus<?> eventBus;

    public EventDispatcher(AbstractEventBus<?> eventBus) {
        this.eventBus = eventBus;
    }

    public AbstractEventBus<?> getEventBus() {
        return this.eventBus;
    }

    private boolean shouldPost(Class<? extends LuckPermsEvent> eventClass) {
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
            post(event);
        });
    }
    
    @SuppressWarnings("unchecked")
    private <T extends LuckPermsEvent> T generate(Class<T> eventClass, Object... params) {
        try {
            return (T) GeneratedEventClass.generate(eventClass).newInstance(this.eventBus.getApiProvider(), params);
        } catch (Throwable e) {
            throw new RuntimeException("Exception occurred whilst generating event instance", e);
        }
    }

    public void dispatchContextUpdate(Object subject) {
        if (!shouldPost(ContextUpdateEvent.class)) {
            return;
        }

        post(generate(ContextUpdateEvent.class, subject));
    }

    public void dispatchExtensionLoad(Extension extension) {
        post(ExtensionLoadEvent.class, () -> generate(ExtensionLoadEvent.class, extension));
    }

    public void dispatchGroupCacheLoad(Group group, GroupCachedDataManager data) {
        post(GroupCacheLoadEvent.class, () -> generate(GroupCacheLoadEvent.class, group.getApiProxy(), data));
    }

    public void dispatchGroupCreate(Group group, CreationCause cause) {
        post(GroupCreateEvent.class, () -> generate(GroupCreateEvent.class, group.getApiProxy(), cause));
    }

    public void dispatchGroupDelete(Group group, DeletionCause cause) {
        post(GroupDeleteEvent.class, () -> generate(GroupDeleteEvent.class, group.getName(), ImmutableSet.copyOf(group.normalData().asSet()), cause));
    }

    public void dispatchGroupLoadAll() {
        post(GroupLoadAllEvent.class, () -> generate(GroupLoadAllEvent.class));
    }

    public void dispatchGroupLoad(Group group) {
        post(GroupLoadEvent.class, () -> generate(GroupLoadEvent.class, group.getApiProxy()));
    }

    public boolean dispatchLogBroadcast(boolean initialState, Action entry, LogBroadcastEvent.Origin origin) {
        if (!shouldPost(LogBroadcastEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogBroadcastEvent.class, cancel, entry, origin));
        return cancel.get();
    }

    public boolean dispatchLogPublish(boolean initialState, Action entry) {
        if (!shouldPost(LogPublishEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogPublishEvent.class, cancel, entry));
        return cancel.get();
    }

    public boolean dispatchLogNetworkPublish(boolean initialState, UUID id, Action entry) {
        if (!shouldPost(LogNetworkPublishEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogNetworkPublishEvent.class, cancel, id, entry));
        return cancel.get();
    }

    public boolean dispatchLogNotify(boolean initialState, Action entry, LogNotifyEvent.Origin origin, Sender sender) {
        if (!shouldPost(LogNotifyEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(LogNotifyEvent.class, cancel, entry, origin, new SenderPlatformEntity(sender)));
        return cancel.get();
    }

    public void dispatchLogReceive(UUID id, Action entry) {
        post(LogReceiveEvent.class, () -> generate(LogReceiveEvent.class, id, entry));
    }

    public void dispatchNodeAdd(Node node, PermissionHolder target, DataType dataType, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeAddEvent.class, () -> generate(NodeAddEvent.class, proxy(target), dataType, ImmutableSet.copyOf(before), ImmutableSet.copyOf(after), node));
    }

    public void dispatchNodeClear(PermissionHolder target, DataType dataType, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeClearEvent.class, () -> generate(NodeClearEvent.class, proxy(target), dataType, ImmutableSet.copyOf(before), ImmutableSet.copyOf(after)));
    }

    public void dispatchNodeRemove(Node node, PermissionHolder target, DataType dataType, Collection<? extends Node> before, Collection<? extends Node> after) {
        post(NodeRemoveEvent.class, () -> generate(NodeRemoveEvent.class, proxy(target), dataType, ImmutableSet.copyOf(before), ImmutableSet.copyOf(after), node));
    }

    public void dispatchConfigReload() {
        post(ConfigReloadEvent.class, () -> generate(ConfigReloadEvent.class));
    }

    public void dispatchPostSync() {
        post(PostSyncEvent.class, () -> generate(PostSyncEvent.class));
    }

    public boolean dispatchNetworkPreSync(boolean initialState, UUID id) {
        if (!shouldPost(PreNetworkSyncEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(PreNetworkSyncEvent.class, cancel, id));
        return cancel.get();
    }

    public boolean dispatchPreSync(boolean initialState) {
        if (!shouldPost(PreSyncEvent.class)) {
            return initialState;
        }

        AtomicBoolean cancel = new AtomicBoolean(initialState);
        post(generate(PreSyncEvent.class, cancel));
        return cancel.get();
    }

    public void dispatchTrackCreate(Track track, CreationCause cause) {
        post(TrackCreateEvent.class, () -> generate(TrackCreateEvent.class, track.getApiProxy(), cause));
    }

    public void dispatchTrackDelete(Track track, DeletionCause cause) {
        post(TrackDeleteEvent.class, () -> generate(TrackDeleteEvent.class, track.getName(), ImmutableList.copyOf(track.getGroups()), cause));
    }

    public void dispatchTrackLoadAll() {
        post(TrackLoadAllEvent.class, () -> generate(TrackLoadAllEvent.class));
    }

    public void dispatchTrackLoad(Track track) {
        post(TrackLoadEvent.class, () -> generate(TrackLoadEvent.class, track.getApiProxy()));
    }

    public void dispatchTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        post(TrackAddGroupEvent.class, () -> generate(TrackAddGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group));
    }

    public void dispatchTrackClear(Track track, List<String> before) {
        post(TrackClearEvent.class, () -> generate(TrackClearEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.of()));
    }

    public void dispatchTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        post(TrackRemoveGroupEvent.class, () -> generate(TrackRemoveGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group));
    }

    public void dispatchUserCacheLoad(User user, UserCachedDataManager data) {
        post(UserCacheLoadEvent.class, () -> generate(UserCacheLoadEvent.class, user.getApiProxy(), data));
    }

    public void dispatchDataRecalculate(PermissionHolder holder) {
        if (holder.getType() == HolderType.USER) {
            User user = (User) holder;
            post(UserDataRecalculateEvent.class, () -> generate(UserDataRecalculateEvent.class, user.getApiProxy(), user.getCachedData()));
        } else {
            Group group = (Group) holder;
            post(GroupDataRecalculateEvent.class, () -> generate(GroupDataRecalculateEvent.class, group.getApiProxy(), group.getCachedData()));
        }
    }

    public void dispatchUserFirstLogin(UUID uniqueId, String username) {
        post(UserFirstLoginEvent.class, () -> generate(UserFirstLoginEvent.class, uniqueId, username));
    }

    public void dispatchPlayerLoginProcess(UUID uniqueId, String username, User user) {
        if (!shouldPost(PlayerLoginProcessEvent.class)) {
            return;
        }

        post(generate(PlayerLoginProcessEvent.class, uniqueId, username, user.getApiProxy()));
    }

    public void dispatchPlayerDataSave(UUID uniqueId, String username, PlayerSaveResult result) {
        post(PlayerDataSaveEvent.class, () -> generate(PlayerDataSaveEvent.class, uniqueId, username, result));
    }

    public String dispatchUniqueIdDetermineType(UUID uniqueId, String initialType) {
        if (!shouldPost(UniqueIdDetermineTypeEvent.class)) {
            return initialType;
        }

        AtomicReference<String> result = new AtomicReference<>(initialType);
        post(generate(UniqueIdDetermineTypeEvent.class, result, uniqueId));
        return result.get();
    }

    public UUID dispatchUniqueIdLookup(String username, UUID initial) {
        if (!shouldPost(UniqueIdLookupEvent.class)) {
            return initial;
        }

        AtomicReference<UUID> result = new AtomicReference<>(initial);
        post(generate(UniqueIdLookupEvent.class, result, username));
        return result.get();
    }

    public String dispatchUsernameLookup(UUID uniqueId, String initial) {
        if (!shouldPost(UsernameLookupEvent.class)) {
            return initial;
        }

        AtomicReference<String> result = new AtomicReference<>(initial);
        post(generate(UsernameLookupEvent.class, result, uniqueId));
        return result.get();
    }

    public boolean dispatchUsernameValidityCheck(String username, boolean initialState) {
        if (!shouldPost(UsernameValidityCheckEvent.class)) {
            return initialState;
        }

        AtomicBoolean result = new AtomicBoolean(initialState);
        post(generate(UsernameValidityCheckEvent.class, username, result));
        return result.get();
    }

    public void dispatchUserLoad(User user) {
        post(UserLoadEvent.class, () -> generate(UserLoadEvent.class, user.getApiProxy()));
    }

    public void dispatchUserDemote(User user, Track track, String from, String to, @Nullable Sender source) {
        post(UserDemoteEvent.class, () -> {
            Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(source));
            return generate(UserDemoteEvent.class, s, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
        });
    }

    public void dispatchUserPromote(User user, Track track, String from, String to, @Nullable Sender source) {
        post(UserPromoteEvent.class, () -> {
            Source s = source == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(source));
            return generate(UserPromoteEvent.class, s, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
        });
    }

    private static ApiPermissionHolder proxy(PermissionHolder holder) {
        if (holder instanceof Group) {
            return ((Group) holder).getApiProxy();
        } else if (holder instanceof User) {
            return ((User) holder).getApiProxy();
        } else {
            throw new AssertionError();
        }
    }

    public static List<Class<? extends LuckPermsEvent>> getKnownEventTypes() {
        return ImmutableList.of(
                ContextUpdateEvent.class,
                ExtensionLoadEvent.class,
                GroupCacheLoadEvent.class,
                GroupCreateEvent.class,
                GroupDataRecalculateEvent.class,
                GroupDeleteEvent.class,
                GroupLoadAllEvent.class,
                GroupLoadEvent.class,
                LogBroadcastEvent.class,
                LogNetworkPublishEvent.class,
                LogNotifyEvent.class,
                LogPublishEvent.class,
                LogReceiveEvent.class,
                NodeAddEvent.class,
                NodeClearEvent.class,
                NodeRemoveEvent.class,
                PlayerDataSaveEvent.class,
                PlayerLoginProcessEvent.class,
                UniqueIdDetermineTypeEvent.class,
                UniqueIdLookupEvent.class,
                UsernameLookupEvent.class,
                UsernameValidityCheckEvent.class,
                ConfigReloadEvent.class,
                PostSyncEvent.class,
                PreNetworkSyncEvent.class,
                PreSyncEvent.class,
                TrackCreateEvent.class,
                TrackDeleteEvent.class,
                TrackLoadAllEvent.class,
                TrackLoadEvent.class,
                TrackAddGroupEvent.class,
                TrackClearEvent.class,
                TrackRemoveGroupEvent.class,
                UserCacheLoadEvent.class,
                UserDataRecalculateEvent.class,
                UserFirstLoginEvent.class,
                UserLoadEvent.class,
                UserDemoteEvent.class,
                UserPromoteEvent.class
        );
    }

}
