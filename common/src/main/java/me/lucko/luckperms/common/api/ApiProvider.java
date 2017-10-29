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

package me.lucko.luckperms.common.api;

import lombok.Getter;
import lombok.NonNull;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LPConfiguration;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.NodeFactory;
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.UuidCache;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.api.delegates.MetaStackFactoryDelegate;
import me.lucko.luckperms.common.api.delegates.NodeFactoryDelegate;
import me.lucko.luckperms.common.api.delegates.UserDelegate;
import me.lucko.luckperms.common.event.EventFactory;
import me.lucko.luckperms.common.event.LuckPermsEventBus;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements the LuckPerms API using the plugin instance
 */
public class ApiProvider implements LuckPermsApi {
    private final LuckPermsPlugin plugin;

    @Getter
    private final LuckPermsEventBus eventBus;

    @Getter
    private final EventFactory eventFactory;

    @Getter
    private final MetaStackFactoryDelegate metaStackFactory;

    public ApiProvider(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.eventBus = new LuckPermsEventBus(plugin);
        this.eventFactory = new EventFactory(eventBus);
        this.metaStackFactory = new MetaStackFactoryDelegate(plugin);
    }

    @Override
    public void runUpdateTask() {
        plugin.getUpdateTaskBuffer().request();
    }

    @Override
    public double getApiVersion() {
        return 3.4;
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public PlatformType getPlatformType() {
        return plugin.getServerType();
    }

    @Override
    public LPConfiguration getConfiguration() {
        return plugin.getConfiguration().getDelegate();
    }

    @Override
    public Storage getStorage() {
        return plugin.getStorage().getDelegate();
    }

    @Override
    public Optional<MessagingService> getMessagingService() {
        return plugin.getMessagingService().map(Function.identity());
    }

    @Override
    public UuidCache getUuidCache() {
        return plugin.getUuidCache().getDelegate();
    }

    @Override
    public Logger getLogger() {
        return plugin.getLog();
    }

    @Override
    public User getUser(@NonNull UUID uuid) {
        final me.lucko.luckperms.common.model.User user = plugin.getUserManager().getIfLoaded(uuid);
        return user == null ? null : user.getDelegate();
    }

    @Override
    public Optional<User> getUserSafe(@NonNull UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

    @Override
    public User getUser(@NonNull String name) {
        final me.lucko.luckperms.common.model.User user = plugin.getUserManager().getByUsername(name);
        return user == null ? null : user.getDelegate();
    }

    @Override
    public Optional<User> getUserSafe(@NonNull String name) {
        return Optional.ofNullable(getUser(name));
    }

    @Override
    public Set<User> getUsers() {
        return plugin.getUserManager().getAll().values().stream().map(me.lucko.luckperms.common.model.User::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isUserLoaded(@NonNull UUID uuid) {
        return plugin.getUserManager().isLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public void cleanupUser(@NonNull User user) {
        plugin.getUserManager().scheduleUnload(plugin.getUuidCache().getExternalUUID(UserDelegate.cast(user).getUuid()));
    }

    @Override
    public Group getGroup(@NonNull String name) {
        final me.lucko.luckperms.common.model.Group group = plugin.getGroupManager().getIfLoaded(name);
        return group == null ? null : group.getDelegate();
    }

    @Override
    public Optional<Group> getGroupSafe(@NonNull String name) {
        return Optional.ofNullable(getGroup(name));
    }

    @Override
    public Set<Group> getGroups() {
        return plugin.getGroupManager().getAll().values().stream().map(me.lucko.luckperms.common.model.Group::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isGroupLoaded(@NonNull String name) {
        return plugin.getGroupManager().isLoaded(name);
    }

    @Override
    public Track getTrack(@NonNull String name) {
        final me.lucko.luckperms.common.model.Track track = plugin.getTrackManager().getIfLoaded(name);
        return track == null ? null : track.getDelegate();
    }

    @Override
    public Optional<Track> getTrackSafe(@NonNull String name) {
        return Optional.ofNullable(getTrack(name));
    }

    @Override
    public Set<Track> getTracks() {
        return plugin.getTrackManager().getAll().values().stream().map(me.lucko.luckperms.common.model.Track::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isTrackLoaded(@NonNull String name) {
        return plugin.getTrackManager().isLoaded(name);
    }

    @Override
    public NodeFactory getNodeFactory() {
        return NodeFactoryDelegate.INSTANCE;
    }

    @Override
    public Node.Builder buildNode(@NonNull String permission) throws IllegalArgumentException {
        return me.lucko.luckperms.common.node.NodeFactory.newBuilder(permission);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerContextCalculator(@NonNull ContextCalculator<?> contextCalculator) {
        plugin.getContextManager().registerCalculator(contextCalculator);
    }

    @Override
    public Optional<Contexts> getContextForUser(@NonNull User user) {
        return Optional.ofNullable(plugin.getContextForUser(UserDelegate.cast(user)));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ContextSet getContextForPlayer(@NonNull Object player) {
        return plugin.getContextManager().getApplicableContext(player);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Contexts getContextsForPlayer(@NonNull Object player) {
        return plugin.getContextManager().getApplicableContexts(player);
    }

    @Override
    public Set<UUID> getUniqueConnections() {
        return Collections.unmodifiableSet(plugin.getUniqueConnections());
    }

    @Override
    public long getStartTime() {
        return plugin.getStartTime();
    }
}
