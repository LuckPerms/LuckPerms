/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.api.implementation;

import com.google.common.eventbus.EventBus;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.context.ContextListener;
import me.lucko.luckperms.api.context.IContextCalculator;
import me.lucko.luckperms.api.event.LPEvent;
import me.lucko.luckperms.api.event.LPListener;
import me.lucko.luckperms.api.implementation.internal.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.lucko.luckperms.api.implementation.internal.Utils.checkNode;

/**
 * Implements the LuckPerms API using the plugin instance
 */
@AllArgsConstructor
public class ApiProvider implements LuckPermsApi {
    private final LuckPermsPlugin plugin;
    private final EventBus eventBus = new EventBus("LuckPerms");

    public void fireEventAsync(LPEvent event) {
        plugin.doAsync(() -> fireEvent(event));
    }

    public void fireEvent(LPEvent event) {
        try {
            event.setApi(this);
            eventBus.post(event);
        } catch (Exception e) {
            getLogger().severe("Couldn't fire LuckPerms Event: " + event.getEventName());
            e.printStackTrace();
        }
    }

    @Override
    public void runUpdateTask() {
        plugin.runUpdateTask();
    }

    @Override
    public double getApiVersion() {
        return 2.10;
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public PlatformType getPlatformType() {
        return plugin.getType();
    }

    @Override
    public void registerListener(@NonNull LPListener listener) {
        eventBus.register(listener);
    }

    @Override
    public void unregisterListener(@NonNull LPListener listener) {
        eventBus.unregister(listener);
    }

    @Override
    public LPConfiguration getConfiguration() {
        return new LPConfigurationLink(plugin.getConfiguration());
    }

    @Override
    public Datastore getDatastore() {
        return new DatastoreLink(plugin, plugin.getDatastore());
    }

    @Override
    public UuidCache getUuidCache() {
        return new UuidCacheLink(plugin.getUuidCache());
    }

    @Override
    public Logger getLogger() {
        return plugin.getLog();
    }

    @Override
    public User getUser(@NonNull UUID uuid) {
        final me.lucko.luckperms.users.User user = plugin.getUserManager().get(uuid);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(@NonNull UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

    @Override
    public User getUser(@NonNull String name) {
        final me.lucko.luckperms.users.User user = plugin.getUserManager().get(name);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(@NonNull String name) {
        return Optional.ofNullable(getUser(name));
    }

    @Override
    public Set<User> getUsers() {
        return plugin.getUserManager().getAll().values().stream().map(UserLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isUserLoaded(@NonNull UUID uuid) {
        return plugin.getUserManager().isLoaded(uuid);
    }

    @Override
    public void cleanupUser(@NonNull User user) {
        Utils.checkUser(user);
        plugin.getUserManager().cleanup(((UserLink) user).getMaster());
    }

    @Override
    public Group getGroup(@NonNull String name) {
        final me.lucko.luckperms.groups.Group group = plugin.getGroupManager().get(name);
        if (group == null) {
            return null;
        }

        return new GroupLink(group);
    }

    @Override
    public Optional<Group> getGroupSafe(@NonNull String name) {
        return Optional.ofNullable(getGroup(name));
    }

    @Override
    public Set<Group> getGroups() {
        return plugin.getGroupManager().getAll().values().stream().map(GroupLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isGroupLoaded(@NonNull String name) {
        return plugin.getGroupManager().isLoaded(name);
    }

    @Override
    public Track getTrack(@NonNull String name) {
        final me.lucko.luckperms.tracks.Track track = plugin.getTrackManager().get(name);
        if (track == null) {
            return null;
        }

        return new TrackLink(track);
    }

    @Override
    public Optional<Track> getTrackSafe(@NonNull String name) {
        return Optional.ofNullable(getTrack(name));
    }

    @Override
    public Set<Track> getTracks() {
        return plugin.getTrackManager().getAll().values().stream().map(TrackLink::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isTrackLoaded(@NonNull String name) {
        return plugin.getTrackManager().isLoaded(name);
    }

    @Override
    public Node.Builder buildNode(@NonNull String permission) throws IllegalArgumentException {
        return new me.lucko.luckperms.core.Node.Builder(checkNode(permission));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerContextCalculator(IContextCalculator<?> contextCalculator) {
        plugin.getContextManager().registerCalculator(contextCalculator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerContextListener(ContextListener<?> contextListener) {
        plugin.getContextManager().registerListener(contextListener);
    }
}
