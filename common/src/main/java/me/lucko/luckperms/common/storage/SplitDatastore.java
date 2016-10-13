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
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SplitDatastore extends Datastore {
    private final Map<String, Datastore> backing;
    private final Map<String, String> types;

    protected SplitDatastore(LuckPermsPlugin plugin, Map<String, Datastore> backing, Map<String, String> types) {
        super(plugin, "Split Storage");
        this.backing = ImmutableMap.copyOf(backing);
        this.types = ImmutableMap.copyOf(types);
    }

    @Override
    public void init() {
        backing.values().forEach(Datastore::init);
        for (Datastore ds : backing.values()) {
            if (!ds.isAcceptingLogins()) {
                return;
            }
        }

        setAcceptingLogins(true);
    }

    @Override
    public void shutdown() {
        backing.values().forEach(Datastore::shutdown);
    }

    @Override
    public boolean logAction(LogEntry entry) {
        return backing.get(types.get("log")).logAction(entry);
    }

    @Override
    public Log getLog() {
        return backing.get(types.get("log")).getLog();
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        return backing.get(types.get("user")).loadUser(uuid, username);
    }

    @Override
    public boolean saveUser(User user) {
        return backing.get(types.get("user")).saveUser(user);
    }

    @Override
    public boolean cleanupUsers() {
        return backing.get(types.get("user")).cleanupUsers();
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        return backing.get(types.get("user")).getUniqueUsers();
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        return backing.get(types.get("group")).createAndLoadGroup(name);
    }

    @Override
    public boolean loadGroup(String name) {
        return backing.get(types.get("group")).loadGroup(name);
    }

    @Override
    public boolean loadAllGroups() {
        return backing.get(types.get("group")).loadAllGroups();
    }

    @Override
    public boolean saveGroup(Group group) {
        return backing.get(types.get("group")).saveGroup(group);
    }

    @Override
    public boolean deleteGroup(Group group) {
        return backing.get(types.get("group")).deleteGroup(group);
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        return backing.get(types.get("track")).createAndLoadTrack(name);
    }

    @Override
    public boolean loadTrack(String name) {
        return backing.get(types.get("track")).loadTrack(name);
    }

    @Override
    public boolean loadAllTracks() {
        return backing.get(types.get("track")).loadAllTracks();
    }

    @Override
    public boolean saveTrack(Track track) {
        return backing.get(types.get("track")).saveTrack(track);
    }

    @Override
    public boolean deleteTrack(Track track) {
        return backing.get(types.get("track")).deleteTrack(track);
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        return backing.get(types.get("uuid")).saveUUIDData(username, uuid);
    }

    @Override
    public UUID getUUID(String username) {
        return backing.get(types.get("uuid")).getUUID(username);
    }

    @Override
    public String getName(UUID uuid) {
        return backing.get(types.get("uuid")).getName(uuid);
    }
}
