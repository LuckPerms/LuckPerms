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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.implementation.internal.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides static access to LuckPerms
 */
@AllArgsConstructor
public class ApiProvider implements LuckPermsApi {
    private final LuckPermsPlugin plugin;

    @Override
    public void runUpdateTask() {
        plugin.runUpdateTask();
    }

    @Override
    public String getVersion() {
        return plugin.getVersion();
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
        final me.lucko.luckperms.users.User user = plugin.getUserManager().getUser(uuid);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

    @Override
    public User getUser(@NonNull String name) {
        final me.lucko.luckperms.users.User user = plugin.getUserManager().getUser(name);
        if (user == null) {
            return null;
        }

        return new UserLink(user);
    }

    @Override
    public Optional<User> getUserSafe(String name) {
        return Optional.ofNullable(getUser(name));
    }

    @Override
    public boolean isUserLoaded(@NonNull UUID uuid) {
        return plugin.getUserManager().isLoaded(uuid);
    }

    @Override
    public Group getGroup(@NonNull String name) {
        final me.lucko.luckperms.groups.Group group = plugin.getGroupManager().getGroup(name);
        if (group == null) {
            return null;
        }

        return new GroupLink(group);
    }

    @Override
    public Optional<Group> getGroupSafe(String name) {
        return Optional.ofNullable(getGroup(name));
    }

    @Override
    public boolean isGroupLoaded(@NonNull String name) {
        return plugin.getGroupManager().isLoaded(name);
    }

    @Override
    public Track getTrack(@NonNull String name) {
        final me.lucko.luckperms.tracks.Track track = plugin.getTrackManager().getTrack(name);
        if (track == null) {
            return null;
        }

        return new TrackLink(track);
    }

    @Override
    public Optional<Track> getTrackSafe(String name) {
        return Optional.ofNullable(getTrack(name));
    }

    @Override
    public boolean isTrackLoaded(@NonNull String name) {
        return plugin.getTrackManager().isLoaded(name);
    }
}
