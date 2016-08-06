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

package me.lucko.luckperms.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
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

    private <T> void runCallback(T t, Callback<T> callback) {
        doSync(() -> callback.onComplete(t));
    }

    /*
        These methods are called immediately and in the same thread as they are called in.
     */
    public abstract void init();
    public abstract void shutdown();
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
        Callbacks are ran on the main Bukkit server thread (if applicable)
     */
    public void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadOrCreateUser(uuid, username), callback));
    }

    public void loadUser(UUID uuid, Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadUser(uuid), callback));
    }

    public void saveUser(User user, Callback<Boolean> callback) {
        doAsync(() -> runCallback(saveUser(user), callback));
    }

    public void createAndLoadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> runCallback(createAndLoadGroup(name), callback));
    }

    public void loadGroup(String name, Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadGroup(name), callback));
    }

    public void loadAllGroups(Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadAllGroups(), callback));
    }

    public void saveGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> runCallback(saveGroup(group), callback));
    }

    public void deleteGroup(Group group, Callback<Boolean> callback) {
        doAsync(() -> runCallback(deleteGroup(group), callback));
    }

    public void createAndLoadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> runCallback(createAndLoadTrack(name), callback));
    }

    public void loadTrack(String name, Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadTrack(name), callback));
    }

    public void loadAllTracks(Callback<Boolean> callback) {
        doAsync(() -> runCallback(loadAllTracks(), callback));
    }

    public void saveTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> runCallback(saveTrack(track), callback));
    }

    public void deleteTrack(Track track, Callback<Boolean> callback) {
        doAsync(() -> runCallback(deleteTrack(track), callback));
    }

    public void saveUUIDData(String username, UUID uuid, Callback<Boolean> callback) {
        doAsync(() -> runCallback(saveUUIDData(username, uuid), callback));
    }

    public void getUUID(String username, Callback<UUID> callback) {
        doAsync(() -> runCallback(getUUID(username), callback));
    }
}
