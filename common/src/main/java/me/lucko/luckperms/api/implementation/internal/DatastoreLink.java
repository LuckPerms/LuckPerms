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

package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Datastore;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.data.Callback;

import java.util.UUID;

import static me.lucko.luckperms.api.implementation.internal.Utils.*;

/**
 * Provides a link between {@link Datastore} and {@link me.lucko.luckperms.data.Datastore}
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DatastoreLink implements Datastore {

    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.data.Datastore master;
    private final Async async;
    private final Sync sync;

    public DatastoreLink(@NonNull LuckPermsPlugin plugin, @NonNull me.lucko.luckperms.data.Datastore master) {
        this.plugin = plugin;
        this.master = master;
        this.async = new Async(master);
        this.sync = new Sync(master);
    }

    private static <T> Callback<T> checkCallback(Callback<T> c) {
        // If no callback was given, just send an empty one
        if (c == null) {
            c = t -> {};
        }
        return c;
    }

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public boolean isAcceptingLogins() {
        return master.isAcceptingLogins();
    }

    @Override
    public Async async() {
        return async;
    }

    @Override
    public Sync sync() {
        return sync;
    }

    @AllArgsConstructor
    public class Async implements Datastore.Async {
        private final me.lucko.luckperms.data.Datastore master;

        @Override
        public void loadOrCreateUser(@NonNull UUID uuid, @NonNull String username, Callback<Boolean> callback) {
            master.loadOrCreateUser(uuid, checkUsername(username), checkCallback(callback));
        }

        @Override
        public void loadUser(@NonNull UUID uuid, Callback<Boolean> callback) {
            master.loadUser(uuid, checkCallback(callback));
        }

        @Override
        public void saveUser(@NonNull User user, Callback<Boolean> callback) {
            checkUser(user);
            master.saveUser(((UserLink) user).getMaster(), checkCallback(callback));
        }

        @Override
        public void createAndLoadGroup(@NonNull String name, Callback<Boolean> callback) {
            master.createAndLoadGroup(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadGroup(@NonNull String name, Callback<Boolean> callback) {
            master.loadGroup(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadAllGroups(Callback<Boolean> callback) {
            master.loadAllGroups(checkCallback(callback));
        }

        @Override
        public void saveGroup(@NonNull Group group, Callback<Boolean> callback) {
            checkGroup(group);
            master.saveGroup(((GroupLink) group).getMaster(), checkCallback(callback));
        }

        @Override
        public void deleteGroup(@NonNull Group group, Callback<Boolean> callback) {
            checkGroup(group);
            if (group.getName().equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
                throw new IllegalArgumentException("Cannot delete the default group.");
            }
            master.deleteGroup(((GroupLink) group).getMaster(), checkCallback(callback));
        }

        @Override
        public void createAndLoadTrack(@NonNull String name, Callback<Boolean> callback) {
            master.createAndLoadTrack(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadTrack(@NonNull String name, Callback<Boolean> callback) {
            master.loadTrack(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadAllTracks(Callback<Boolean> callback) {
            master.loadAllTracks(checkCallback(callback));
        }

        @Override
        public void saveTrack(@NonNull Track track, Callback<Boolean> callback) {
            checkTrack(track);
            master.saveTrack(((TrackLink) track).getMaster(), checkCallback(callback));
        }

        @Override
        public void deleteTrack(@NonNull Track track, Callback<Boolean> callback) {
            checkTrack(track);
            master.deleteTrack(((TrackLink) track).getMaster(), checkCallback(callback));
        }

        @Override
        public void saveUUIDData(@NonNull String username, @NonNull UUID uuid, Callback<Boolean> callback) {
            master.saveUUIDData(checkUsername(username), uuid, checkCallback(callback));
        }

        @Override
        public void getUUID(@NonNull String username, Callback<UUID> callback) {
            master.getUUID(checkUsername(username), checkCallback(callback));
        }
    }

    @AllArgsConstructor
    public class Sync implements Datastore.Sync {
        private final me.lucko.luckperms.data.Datastore master;

        @Override
        public boolean loadOrCreateUser(@NonNull UUID uuid, @NonNull String username) {
            return master.loadOrCreateUser(uuid, checkUsername(username));
        }

        @Override
        public boolean loadUser(@NonNull UUID uuid) {
            return master.loadUser(uuid);
        }

        @Override
        public boolean saveUser(@NonNull User user) {
            checkUser(user);
            return master.saveUser(((UserLink) user).getMaster());
        }

        @Override
        public boolean createAndLoadGroup(@NonNull String name) {
            return master.createAndLoadGroup(checkName(name));
        }

        @Override
        public boolean loadGroup(@NonNull String name) {
            return master.loadGroup(checkName(name));
        }

        @Override
        public boolean loadAllGroups() {
            return master.loadAllGroups();
        }

        @Override
        public boolean saveGroup(@NonNull Group group) {
            checkGroup(group);
            return master.saveGroup(((GroupLink) group).getMaster());
        }

        @Override
        public boolean deleteGroup(@NonNull Group group) {
            checkGroup(group);
            if (group.getName().equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
                throw new IllegalArgumentException("Cannot delete the default group.");
            }
            return master.deleteGroup(((GroupLink) group).getMaster());
        }

        @Override
        public boolean createAndLoadTrack(@NonNull String name) {
            return master.createAndLoadTrack(checkName(name));
        }

        @Override
        public boolean loadTrack(@NonNull String name) {
            return master.loadTrack(checkName(name));
        }

        @Override
        public boolean loadAllTracks() {
            return master.loadAllTracks();
        }

        @Override
        public boolean saveTrack(@NonNull Track track) {
            checkTrack(track);
            return master.saveTrack(((TrackLink) track).getMaster());
        }

        @Override
        public boolean deleteTrack(@NonNull Track track) {
            checkTrack(track);
            return master.deleteTrack(((TrackLink) track).getMaster());
        }

        @Override
        public boolean saveUUIDData(@NonNull String username, @NonNull UUID uuid) {
            return master.saveUUIDData(checkUsername(username), uuid);
        }

        @Override
        public UUID getUUID(@NonNull String username) {
            return master.getUUID(checkUsername(username));
        }
    }

}
