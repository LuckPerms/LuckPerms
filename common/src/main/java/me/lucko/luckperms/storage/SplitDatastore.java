package me.lucko.luckperms.storage;

import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

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
