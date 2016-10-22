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

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.LPFuture;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SplitBacking implements Datastore {

    @Getter
    @Setter
    private boolean acceptingLogins = false;

    private final LuckPermsPlugin plugin;
    private final Map<String, Datastore> backing;
    private final Map<String, String> types;

    protected SplitBacking(LuckPermsPlugin plugin, Map<String, Datastore> backing, Map<String, String> types) {
        this.plugin = plugin;
        this.backing = ImmutableMap.copyOf(backing);
        this.types = ImmutableMap.copyOf(types);
    }

    @Override
    public String getName() {
        return "Split Storage";
    }

    @Override
    public void doAsync(Runnable r) {
        plugin.doAsync(r);
    }

    @Override
    public void doSync(Runnable r) {
        plugin.doSync(r);
    }

    @Override
    public void init() {
        boolean success = true;
        backing.values().forEach(Datastore::init);
        for (Datastore ds : backing.values()) {
            if (!ds.isAcceptingLogins()) {
                success = false;
            }
        }

        setAcceptingLogins(success);
    }

    @Override
    public void shutdown() {
        backing.values().forEach(Datastore::shutdown);
    }

    @Override
    public LPFuture<Boolean> logAction(LogEntry entry) {
        return backing.get(types.get("log")).logAction(entry);
    }

    @Override
    public LPFuture<Log> getLog() {
        return backing.get(types.get("log")).getLog();
    }

    @Override
    public LPFuture<Boolean> loadUser(UUID uuid, String username) {
        return backing.get(types.get("user")).loadUser(uuid, username);
    }

    @Override
    public LPFuture<Boolean> saveUser(User user) {
        return backing.get(types.get("user")).saveUser(user);
    }

    @Override
    public LPFuture<Boolean> cleanupUsers() {
        return backing.get(types.get("user")).cleanupUsers();
    }

    @Override
    public LPFuture<Set<UUID>> getUniqueUsers() {
        return backing.get(types.get("user")).getUniqueUsers();
    }

    @Override
    public LPFuture<Boolean> createAndLoadGroup(String name) {
        return backing.get(types.get("group")).createAndLoadGroup(name);
    }

    @Override
    public LPFuture<Boolean> loadGroup(String name) {
        return backing.get(types.get("group")).loadGroup(name);
    }

    @Override
    public LPFuture<Boolean> loadAllGroups() {
        return backing.get(types.get("group")).loadAllGroups();
    }

    @Override
    public LPFuture<Boolean> saveGroup(Group group) {
        return backing.get(types.get("group")).saveGroup(group);
    }

    @Override
    public LPFuture<Boolean> deleteGroup(Group group) {
        return backing.get(types.get("group")).deleteGroup(group);
    }

    @Override
    public LPFuture<Boolean> createAndLoadTrack(String name) {
        return backing.get(types.get("track")).createAndLoadTrack(name);
    }

    @Override
    public LPFuture<Boolean> loadTrack(String name) {
        return backing.get(types.get("track")).loadTrack(name);
    }

    @Override
    public LPFuture<Boolean> loadAllTracks() {
        return backing.get(types.get("track")).loadAllTracks();
    }

    @Override
    public LPFuture<Boolean> saveTrack(Track track) {
        return backing.get(types.get("track")).saveTrack(track);
    }

    @Override
    public LPFuture<Boolean> deleteTrack(Track track) {
        return backing.get(types.get("track")).deleteTrack(track);
    }

    @Override
    public LPFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return backing.get(types.get("uuid")).saveUUIDData(username, uuid);
    }

    @Override
    public LPFuture<UUID> getUUID(String username) {
        return backing.get(types.get("uuid")).getUUID(username);
    }

    @Override
    public LPFuture<String> getName(UUID uuid) {
        return backing.get(types.get("uuid")).getName(uuid);
    }
}
