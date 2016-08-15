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

package me.lucko.luckperms.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Datastore {
    protected final LuckPermsPlugin plugin;

    @Getter
    public final String name;

    @Getter
    @Setter
    private boolean acceptingLogins = false;

    /**
     * Execute a runnable asynchronously
     * @param r the task to run
     */
    private void doAsync(Runnable r) {
        plugin.doAsync(r);
    }

    /**
     * Execute a runnable synchronously
     * @param r the task to run
     */
    private void doSync(Runnable r) {
        plugin.doSync(r);
    }

    /*
        These methods are called immediately and in the same thread as they are called in.
     */
    public abstract void init();
    public abstract void shutdown();
    public abstract boolean logAction(LogEntry entry);
    public abstract Log getLog();
    public abstract boolean loadOrCreateUser(UUID uuid, String username);
    public abstract boolean loadUser(UUID uuid);
    public abstract boolean saveUser(User user);
    public abstract boolean createAndLoadGroup(String name);
    public abstract boolean loadGroup(String name);
    public abstract boolean loadAllGroups();
    public abstract boolean saveGroup(Group group);
    public abstract boolean deleteGroup(Group group);
    public abstract boolean createAndLoadTrack(String name);
    public abstract boolean loadTrack(String name);
    public abstract boolean loadAllTracks();
    public abstract boolean saveTrack(Track track);
    public abstract boolean deleteTrack(Track track);
    public abstract boolean saveUUIDData(String username, UUID uuid);
    public abstract UUID getUUID(String username);



    /*
        These methods will schedule the operation to run async. The callback will be ran when the task is complete.
        Callbacks are ran on the main server thread (except on BungeeCord)
     */
    public void logAction(LogEntry entry, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = logAction(entry);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void getLog(Callback<Log> callback) {
        doAsync(() -> {
            Log result = getLog();
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadOrCreateUser(uuid, username);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadUser(UUID uuid, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadUser(uuid);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void saveUser(User user, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveUser(user);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void createAndLoadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = createAndLoadGroup(name);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadGroup(name);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadAllGroups(Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadAllGroups();
            doSync(() -> callback.onComplete(result));
        });
    }

    public void saveGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveGroup(group);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void deleteGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = deleteGroup(group);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void createAndLoadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = createAndLoadTrack(name);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadTrack(name);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void loadAllTracks(Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = loadAllTracks();
            doSync(() -> callback.onComplete(result));
        });
    }

    public void saveTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveTrack(track);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void deleteTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = deleteTrack(track);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void saveUUIDData(String username, UUID uuid, Callback<Boolean> callback) {
        doAsync(() -> {
            boolean result = saveUUIDData(username, uuid);
            doSync(() -> callback.onComplete(result));
        });
    }

    public void getUUID(String username, Callback<UUID> callback) {
        doAsync(() -> {
            UUID result = getUUID(username);
            doSync(() -> callback.onComplete(result));
        });
    }
}
