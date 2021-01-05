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
import me.lucko.luckperms.common.model.nodemap.MutateResult;
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
import net.luckperms.api.event.node.NodeMutateEvent;
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
import net.luckperms.api.event.type.ResultEvent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class EventDispatcher {
    private final AbstractEventBus<?> eventBus;

    public EventDispatcher(AbstractEventBus<?> eventBus) {
        this.eventBus = eventBus;
    }

    public AbstractEventBus<?> getEventBus() {
        return this.eventBus;
    }

    private <T extends LuckPermsEvent> void postAsync(Class<T> eventClass, Object... params) {
        // check against common mistakes - events with any sort of result shouldn't be posted async
        if (Cancellable.class.isAssignableFrom(eventClass)) {
            throw new RuntimeException("Cancellable event cannot be posted async (" + eventClass + ")");
        }
        if (ResultEvent.class.isAssignableFrom(eventClass)) {
            throw new RuntimeException("ResultEvent event cannot be posted async (" + eventClass + ")");
        }

        // if there aren't any handlers registered for the event, don't bother trying to post it
        if (!this.eventBus.shouldPost(eventClass)) {
            return;
        }

        // async: generate an event class and post it
        this.eventBus.getPlugin().getBootstrap().getScheduler().executeAsync(() -> {
            T event = generate(eventClass, params);
            this.eventBus.post(event);
        });
    }

    private <T extends LuckPermsEvent> void postSync(Class<T> eventClass, Object... params) {
        // if there aren't any handlers registered for our event, don't bother trying to post it
        if (!this.eventBus.shouldPost(eventClass)) {
            return;
        }

        // generate an event class and post it
        T event = generate(eventClass, params);
        this.eventBus.post(event);
    }

    private <T extends LuckPermsEvent & Cancellable> boolean postCancellable(Class<T> eventClass, Object... params) {
        // extract the initial state from the first parameter
        boolean initialState = (boolean) params[0];

        // if there aren't any handlers registered for the event, just return the initial state
        if (!this.eventBus.shouldPost(eventClass)) {
            return initialState;
        }

        // otherwise:
        // - initialise an AtomicBoolean for the result with the initial state
        // - replace the boolean with the AtomicBoolean in the params array
        // - post the event
        AtomicBoolean cancel = new AtomicBoolean(initialState);
        params[0] = cancel;
        postSync(eventClass, params);

        // return the final status
        return cancel.get();
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
        postSync(ContextUpdateEvent.class, subject);
    }

    public void dispatchExtensionLoad(Extension extension) {
        postAsync(ExtensionLoadEvent.class, extension);
    }

    public void dispatchGroupCacheLoad(Group group, GroupCachedDataManager data) {
        postAsync(GroupCacheLoadEvent.class, group.getApiProxy(), data);
    }

    public void dispatchGroupCreate(Group group, CreationCause cause) {
        postAsync(GroupCreateEvent.class, group.getApiProxy(), cause);
    }

    public void dispatchGroupDelete(Group group, DeletionCause cause) {
        postAsync(GroupDeleteEvent.class, group.getName(), ImmutableSet.copyOf(group.normalData().asSet()), cause);
    }

    public void dispatchGroupLoadAll() {
        postAsync(GroupLoadAllEvent.class);
    }

    public void dispatchGroupLoad(Group group) {
        postAsync(GroupLoadEvent.class, group.getApiProxy());
    }

    public boolean dispatchLogBroadcast(boolean initialState, Action entry, LogBroadcastEvent.Origin origin) {
        return postCancellable(LogBroadcastEvent.class, initialState, entry, origin);
    }

    public boolean dispatchLogPublish(boolean initialState, Action entry) {
        return postCancellable(LogPublishEvent.class, initialState, entry);
    }

    public boolean dispatchLogNetworkPublish(boolean initialState, UUID id, Action entry) {
        return postCancellable(LogNetworkPublishEvent.class, initialState, id, entry);
    }

    public boolean dispatchLogNotify(boolean initialState, Action entry, LogNotifyEvent.Origin origin, Sender sender) {
        return postCancellable(LogNotifyEvent.class, initialState, entry, origin, new SenderPlatformEntity(sender));
    }

    public void dispatchLogReceive(UUID id, Action entry) {
        postAsync(LogReceiveEvent.class, id, entry);
    }

    public void dispatchNodeChanges(PermissionHolder target, DataType dataType, MutateResult changes) {
        if (!this.eventBus.shouldPost(NodeAddEvent.class) && !this.eventBus.shouldPost(NodeRemoveEvent.class)) {
            return;
        }

        if (changes.isEmpty()) {
            return;
        }

        ApiPermissionHolder proxy = proxy(target);
        ImmutableSet<Node> state = target.getData(dataType).asImmutableSet();

        // call an event for each recorded change
        for (MutateResult.Change change : changes.getChanges()) {
            Class<? extends NodeMutateEvent> type = change.getType() == MutateResult.ChangeType.ADD ?
                    NodeAddEvent.class : NodeRemoveEvent.class;

            postAsync(type, proxy, dataType, state, change.getNode());
        }
    }

    public void dispatchNodeClear(PermissionHolder target, DataType dataType, MutateResult changes) {
        if (!this.eventBus.shouldPost(NodeClearEvent.class)) {
            return;
        }

        if (changes.isEmpty()) {
            return;
        }

        ApiPermissionHolder proxy = proxy(target);
        ImmutableSet<Node> state = target.getData(dataType).asImmutableSet();

        // call clear event
        ImmutableSet<Node> nodes = ImmutableSet.copyOf(changes.getRemoved());
        postAsync(NodeClearEvent.class, proxy, dataType, state, nodes);

        // call add event if needed for any nodes that were added
        for (Node added : changes.getAdded()) {
            postAsync(NodeAddEvent.class, proxy, dataType, state, added);
        }
    }

    public void dispatchConfigReload() {
        postAsync(ConfigReloadEvent.class);
    }

    public void dispatchPostSync() {
        postAsync(PostSyncEvent.class);
    }

    public boolean dispatchNetworkPreSync(boolean initialState, UUID id) {
        return postCancellable(PreNetworkSyncEvent.class, initialState, id);
    }

    public boolean dispatchPreSync(boolean initialState) {
        return postCancellable(PreSyncEvent.class, initialState);
    }

    public void dispatchTrackCreate(Track track, CreationCause cause) {
        postAsync(TrackCreateEvent.class, track.getApiProxy(), cause);
    }

    public void dispatchTrackDelete(Track track, DeletionCause cause) {
        postAsync(TrackDeleteEvent.class, track.getName(), ImmutableList.copyOf(track.getGroups()), cause);
    }

    public void dispatchTrackLoadAll() {
        postAsync(TrackLoadAllEvent.class);
    }

    public void dispatchTrackLoad(Track track) {
        postAsync(TrackLoadEvent.class, track.getApiProxy());
    }

    public void dispatchTrackAddGroup(Track track, String group, List<String> before, List<String> after) {
        postAsync(TrackAddGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group);
    }

    public void dispatchTrackClear(Track track, List<String> before) {
        postAsync(TrackClearEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.of());
    }

    public void dispatchTrackRemoveGroup(Track track, String group, List<String> before, List<String> after) {
        postAsync(TrackRemoveGroupEvent.class, track.getApiProxy(), ImmutableList.copyOf(before), ImmutableList.copyOf(after), group);
    }

    public void dispatchUserCacheLoad(User user, UserCachedDataManager data) {
        postAsync(UserCacheLoadEvent.class, user.getApiProxy(), data);
    }

    public void dispatchDataRecalculate(PermissionHolder holder) {
        if (holder.getType() == HolderType.USER) {
            User user = (User) holder;
            postAsync(UserDataRecalculateEvent.class, user.getApiProxy(), user.getCachedData());
        } else {
            Group group = (Group) holder;
            postAsync(GroupDataRecalculateEvent.class, group.getApiProxy(), group.getCachedData());
        }
    }

    public void dispatchUserFirstLogin(UUID uniqueId, String username) {
        postAsync(UserFirstLoginEvent.class, uniqueId, username);
    }

    public void dispatchPlayerLoginProcess(UUID uniqueId, String username, @Nullable User user) {
        postSync(PlayerLoginProcessEvent.class, uniqueId, username, user == null ? null : user.getApiProxy());
    }

    public void dispatchPlayerDataSave(UUID uniqueId, String username, PlayerSaveResult result) {
        postAsync(PlayerDataSaveEvent.class, uniqueId, username, result);
    }

    public String dispatchUniqueIdDetermineType(UUID uniqueId, String initialType) {
        AtomicReference<String> result = new AtomicReference<>(initialType);
        postSync(UniqueIdDetermineTypeEvent.class, result, uniqueId);
        return result.get();
    }

    public UUID dispatchUniqueIdLookup(String username, UUID initial) {
        AtomicReference<UUID> result = new AtomicReference<>(initial);
        postSync(UniqueIdLookupEvent.class, result, username);
        return result.get();
    }

    public String dispatchUsernameLookup(UUID uniqueId, String initial) {
        AtomicReference<String> result = new AtomicReference<>(initial);
        postSync(UsernameLookupEvent.class, result, uniqueId);
        return result.get();
    }

    public boolean dispatchUsernameValidityCheck(String username, boolean initialState) {
        AtomicBoolean result = new AtomicBoolean(initialState);
        postSync(UsernameValidityCheckEvent.class, username, result);
        return result.get();
    }

    public void dispatchUserLoad(User user) {
        postAsync(UserLoadEvent.class, user.getApiProxy());
    }

    public void dispatchUserDemote(User user, Track track, String from, String to, @Nullable Sender sender) {
        Source source = sender == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(sender));
        postAsync(UserDemoteEvent.class, source, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
    }

    public void dispatchUserPromote(User user, Track track, String from, String to, @Nullable Sender sender) {
        Source source = sender == null ? UnknownSource.INSTANCE : new EntitySourceImpl(new SenderPlatformEntity(sender));
        postAsync(UserPromoteEvent.class, source, track.getApiProxy(), user.getApiProxy(), Optional.ofNullable(from), Optional.ofNullable(to));
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
