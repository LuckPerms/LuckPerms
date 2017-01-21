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

package me.lucko.luckperms.common.api.delegate;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.Datastore;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Log;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.storage.Storage;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.lucko.luckperms.common.api.ApiUtils.checkGroup;
import static me.lucko.luckperms.common.api.ApiUtils.checkName;
import static me.lucko.luckperms.common.api.ApiUtils.checkTrack;
import static me.lucko.luckperms.common.api.ApiUtils.checkUser;
import static me.lucko.luckperms.common.api.ApiUtils.checkUsername;

@SuppressWarnings("deprecation")
public class DatastoreDelegate implements Datastore {

    private final LuckPermsPlugin plugin;
    private final Storage master;
    private final Async async;
    private final Sync sync;
    private final Future future;

    public DatastoreDelegate(@NonNull LuckPermsPlugin plugin, @NonNull Storage master) {
        this.plugin = plugin;
        this.master = master;
        this.async = new Async(master);
        this.sync = new Sync(master);
        this.future = new Future(master);
    }

    private <T> void registerCallback(CompletableFuture<T> fut, Callback<T> c) {
        if (c != null) {
            fut.thenAcceptAsync(Callback.convertToConsumer(c), plugin.getSyncExecutor());
        }
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
    public Datastore.Async async() {
        return async;
    }

    @Override
    public Datastore.Sync sync() {
        return sync;
    }

    @Override
    public Datastore.Future future() {
        return future;
    }

    @AllArgsConstructor
    public class Async implements Datastore.Async {
        private final me.lucko.luckperms.common.storage.Storage master;

        @Override
        public void logAction(@NonNull LogEntry entry, Callback<Boolean> callback) {
            registerCallback(master.force().logAction(entry), callback);
        }

        @Override
        public void getLog(@NonNull Callback<Log> callback) {
            master.force().getLog().thenAcceptAsync(log -> callback.onComplete(new LogDelegate(log)), plugin.getSyncExecutor());
        }

        @Override
        public void loadOrCreateUser(@NonNull UUID uuid, @NonNull String username, Callback<Boolean> callback) {
            registerCallback(master.force().loadUser(uuid, checkUsername(username)), callback);
        }

        @Override
        public void loadUser(@NonNull UUID uuid, Callback<Boolean> callback) {
            registerCallback(master.force().loadUser(uuid, "null"), callback);
        }

        @Override
        public void loadUser(@NonNull UUID uuid, @NonNull String username, Callback<Boolean> callback) {
            registerCallback(master.force().loadUser(uuid, checkUsername(username)), callback);
        }

        @Override
        public void saveUser(@NonNull User user, Callback<Boolean> callback) {
            checkUser(user);
            registerCallback(master.force().saveUser(((UserDelegate) user).getMaster()), callback);
        }

        @Override
        public void cleanupUsers(Callback<Boolean> callback) {
            registerCallback(master.force().cleanupUsers(), callback);
        }

        @Override
        public void getUniqueUsers(Callback<Set<UUID>> callback) {
            registerCallback(master.force().getUniqueUsers(), callback);
        }

        @Override
        public void createAndLoadGroup(@NonNull String name, Callback<Boolean> callback) {
            registerCallback(master.force().createAndLoadGroup(checkName(name)), callback);
        }

        @Override
        public void loadGroup(@NonNull String name, Callback<Boolean> callback) {
            registerCallback(master.force().loadGroup(checkName(name)), callback);
        }

        @Override
        public void loadAllGroups(Callback<Boolean> callback) {
            registerCallback(master.force().loadAllGroups(), callback);
        }

        @Override
        public void saveGroup(@NonNull Group group, Callback<Boolean> callback) {
            checkGroup(group);
            registerCallback(master.force().saveGroup(((GroupDelegate) group).getMaster()), callback);
        }

        @Override
        public void deleteGroup(@NonNull Group group, Callback<Boolean> callback) {
            checkGroup(group);
            if (group.getName().equalsIgnoreCase(plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME))) {
                throw new IllegalArgumentException("Cannot delete the default group.");
            }
            registerCallback(master.force().deleteGroup(((GroupDelegate) group).getMaster()), callback);
        }

        @Override
        public void createAndLoadTrack(@NonNull String name, Callback<Boolean> callback) {
            registerCallback(master.force().createAndLoadTrack(checkName(name)), callback);
        }

        @Override
        public void loadTrack(@NonNull String name, Callback<Boolean> callback) {
            registerCallback(master.force().loadTrack(checkName(name)), callback);
        }

        @Override
        public void loadAllTracks(Callback<Boolean> callback) {
            registerCallback(master.force().loadAllTracks(), callback);
        }

        @Override
        public void saveTrack(@NonNull Track track, Callback<Boolean> callback) {
            checkTrack(track);
            registerCallback(master.force().saveTrack(((TrackDelegate) track).getMaster()), callback);
        }

        @Override
        public void deleteTrack(@NonNull Track track, Callback<Boolean> callback) {
            checkTrack(track);
            registerCallback(master.force().deleteTrack(((TrackDelegate) track).getMaster()), callback);
        }

        @Override
        public void saveUUIDData(@NonNull String username, @NonNull UUID uuid, Callback<Boolean> callback) {
            registerCallback(master.force().saveUUIDData(checkUsername(username), uuid), callback);
        }

        @Override
        public void getUUID(@NonNull String username, Callback<UUID> callback) {
            registerCallback(master.force().getUUID(checkUsername(username)), callback);
        }
    }

    @AllArgsConstructor
    public class Sync implements Datastore.Sync {
        private final Storage master;

        @Override
        public boolean logAction(@NonNull LogEntry entry) {
            return master.force().logAction(entry).join();
        }

        @Override
        public Log getLog() {
            me.lucko.luckperms.common.data.Log log = master.force().getLog().join();
            if (log == null) {
                return null;
            }
            return new LogDelegate(log);
        }

        @Override
        public boolean loadOrCreateUser(@NonNull UUID uuid, @NonNull String username) {
            return master.force().loadUser(uuid, checkUsername(username)).join();
        }

        @Override
        public boolean loadUser(@NonNull UUID uuid) {
            return master.force().loadUser(uuid, "null").join();
        }

        @Override
        public boolean loadUser(@NonNull UUID uuid, @NonNull String username) {
            return master.force().loadUser(uuid, checkUsername(username)).join();
        }

        @Override
        public boolean saveUser(@NonNull User user) {
            checkUser(user);
            return master.force().saveUser(((UserDelegate) user).getMaster()).join();
        }

        @Override
        public boolean cleanupUsers() {
            return master.force().cleanupUsers().join();
        }

        @Override
        public Set<UUID> getUniqueUsers() {
            return master.force().getUniqueUsers().join();
        }

        @Override
        public boolean createAndLoadGroup(@NonNull String name) {
            return master.force().createAndLoadGroup(checkName(name)).join();
        }

        @Override
        public boolean loadGroup(@NonNull String name) {
            return master.force().loadGroup(checkName(name)).join();
        }

        @Override
        public boolean loadAllGroups() {
            return master.force().loadAllGroups().join();
        }

        @Override
        public boolean saveGroup(@NonNull Group group) {
            checkGroup(group);
            return master.force().saveGroup(((GroupDelegate) group).getMaster()).join();
        }

        @Override
        public boolean deleteGroup(@NonNull Group group) {
            checkGroup(group);
            if (group.getName().equalsIgnoreCase(plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME))) {
                throw new IllegalArgumentException("Cannot delete the default group.");
            }
            return master.force().deleteGroup(((GroupDelegate) group).getMaster()).join();
        }

        @Override
        public boolean createAndLoadTrack(@NonNull String name) {
            return master.force().createAndLoadTrack(checkName(name)).join();
        }

        @Override
        public boolean loadTrack(@NonNull String name) {
            return master.force().loadTrack(checkName(name)).join();
        }

        @Override
        public boolean loadAllTracks() {
            return master.force().loadAllTracks().join();
        }

        @Override
        public boolean saveTrack(@NonNull Track track) {
            checkTrack(track);
            return master.force().saveTrack(((TrackDelegate) track).getMaster()).join();
        }

        @Override
        public boolean deleteTrack(@NonNull Track track) {
            checkTrack(track);
            return master.force().deleteTrack(((TrackDelegate) track).getMaster()).join();
        }

        @Override
        public boolean saveUUIDData(@NonNull String username, @NonNull UUID uuid) {
            return master.force().saveUUIDData(checkUsername(username), uuid).join();
        }

        @Override
        public UUID getUUID(@NonNull String username) {
            return master.force().getUUID(checkUsername(username)).join();
        }
    }

    @AllArgsConstructor
    public class Future implements Datastore.Future {
        private final Storage master;

        @Override
        public java.util.concurrent.Future<Boolean> logAction(@NonNull LogEntry entry) {
            return master.force().logAction(entry);
        }

        @Override
        public java.util.concurrent.Future<Log> getLog() {
            return master.force().getLog().thenApply(log -> log == null ? null : new LogDelegate(log));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadOrCreateUser(@NonNull UUID uuid, @NonNull String username) {
            return master.force().loadUser(uuid, checkUsername(username));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadUser(@NonNull UUID uuid) {
            return master.force().loadUser(uuid, "null");
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadUser(@NonNull UUID uuid, @NonNull String username) {
            return master.force().loadUser(uuid, checkUsername(username));
        }

        @Override
        public java.util.concurrent.Future<Boolean> saveUser(@NonNull User user) {
            checkUser(user);
            return master.force().saveUser(((UserDelegate) user).getMaster());
        }

        @Override
        public java.util.concurrent.Future<Boolean> cleanupUsers() {
            return master.force().cleanupUsers();
        }

        @Override
        public java.util.concurrent.Future<Set<UUID>> getUniqueUsers() {
            return master.force().getUniqueUsers();
        }

        @Override
        public java.util.concurrent.Future<Boolean> createAndLoadGroup(@NonNull String name) {
            return master.force().createAndLoadGroup(checkName(name));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadGroup(@NonNull String name) {
            return master.force().loadGroup(checkName(name));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadAllGroups() {
            return master.force().loadAllGroups();
        }

        @Override
        public java.util.concurrent.Future<Boolean> saveGroup(@NonNull Group group) {
            checkGroup(group);
            return master.force().saveGroup(((GroupDelegate) group).getMaster());
        }

        @Override
        public java.util.concurrent.Future<Boolean> deleteGroup(@NonNull Group group) {
            checkGroup(group);
            if (group.getName().equalsIgnoreCase(plugin.getConfiguration().get(ConfigKeys.DEFAULT_GROUP_NAME))) {
                throw new IllegalArgumentException("Cannot delete the default group.");
            }
            return master.force().deleteGroup(((GroupDelegate) group).getMaster());
        }

        @Override
        public java.util.concurrent.Future<Boolean> createAndLoadTrack(@NonNull String name) {
            return master.force().createAndLoadTrack(checkName(name));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadTrack(@NonNull String name) {
            return master.force().loadTrack(checkName(name));
        }

        @Override
        public java.util.concurrent.Future<Boolean> loadAllTracks() {
            return master.force().loadAllTracks();
        }

        @Override
        public java.util.concurrent.Future<Boolean> saveTrack(@NonNull Track track) {
            checkTrack(track);
            return master.force().saveTrack(((TrackDelegate) track).getMaster());
        }

        @Override
        public java.util.concurrent.Future<Boolean> deleteTrack(@NonNull Track track) {
            checkTrack(track);
            return master.force().deleteTrack(((TrackDelegate) track).getMaster());
        }

        @Override
        public java.util.concurrent.Future<Boolean> saveUUIDData(@NonNull String username, @NonNull UUID uuid) {
            return master.force().saveUUIDData(checkUsername(username), uuid);
        }

        @Override
        public java.util.concurrent.Future<UUID> getUUID(@NonNull String username) {
            return master.force().getUUID(checkUsername(username));
        }
    }

}
