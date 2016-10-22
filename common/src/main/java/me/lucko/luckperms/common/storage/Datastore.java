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

package me.lucko.luckperms.common.storage;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.LPFuture;

import java.util.Set;
import java.util.UUID;

public interface Datastore {
    
    String getName();
    
    boolean isAcceptingLogins();
    void setAcceptingLogins(boolean acceptingLogins);

    /**
     * Execute a runnable asynchronously
     * @param r the task to run
     */
    void doAsync(Runnable r);

    /**
     * Execute a runnable synchronously
     * @param r the task to run
     */
    void doSync(Runnable r);

    default Datastore force() {
        return this;
    }

    void init();
    LPFuture<Void> shutdown();
    LPFuture<Boolean> logAction(LogEntry entry);
    LPFuture<Log> getLog();
    LPFuture<Boolean> loadUser(UUID uuid, String username);
    LPFuture<Boolean> saveUser(User user);
    LPFuture<Boolean> cleanupUsers();
    LPFuture<Set<UUID>> getUniqueUsers();
    LPFuture<Boolean> createAndLoadGroup(String name);
    LPFuture<Boolean> loadGroup(String name);
    LPFuture<Boolean> loadAllGroups();
    LPFuture<Boolean> saveGroup(Group group);
    LPFuture<Boolean> deleteGroup(Group group);
    LPFuture<Boolean> createAndLoadTrack(String name);
    LPFuture<Boolean> loadTrack(String name);
    LPFuture<Boolean> loadAllTracks();
    LPFuture<Boolean> saveTrack(Track track);
    LPFuture<Boolean> deleteTrack(Track track);
    LPFuture<Boolean> saveUUIDData(String username, UUID uuid);
    LPFuture<UUID> getUUID(String username);
    LPFuture<String> getName(UUID uuid);

    default void logAction(LogEntry entry, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = logAction(entry).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void getLog(Callback<Log> callback) {
        doAsync(() -> {
            Log result = getLog().getOrDefault(null);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void loadUser(UUID uuid, String username, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadUser(uuid, username).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void saveUser(User user, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveUser(user).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void cleanupUsers(Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = cleanupUsers().getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void getUniqueUsers(Callback<Set<UUID>> callback) {
        doAsync(() -> {
            Set<UUID> result = getUniqueUsers().getOrDefault(null);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void createAndLoadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = createAndLoadGroup(name).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void loadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadGroup(name).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void loadAllGroups(Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadAllGroups().getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void saveGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveGroup(group).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void deleteGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = deleteGroup(group).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void createAndLoadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = createAndLoadTrack(name).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void loadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadTrack(name).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void loadAllTracks(Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadAllTracks().getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void saveTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveTrack(track).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void deleteTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = deleteTrack(track).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void saveUUIDData(String username, UUID uuid, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveUUIDData(username, uuid).getOrDefault(false);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void getUUID(String username, Callback<UUID> callback) {
        doAsync(() -> {
            UUID result = getUUID(username).getOrDefault(null);
            doSync(() -> callback.onComplete(result));
        });
    }

    default void getName(UUID uuid, Callback<String> callback) {
        doAsync(() -> {
            String result = getName(uuid).getOrDefault(null);
            doSync(() -> callback.onComplete(result));
        });
    }
}
