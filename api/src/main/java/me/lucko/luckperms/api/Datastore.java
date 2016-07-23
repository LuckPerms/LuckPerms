package me.lucko.luckperms.api;

import me.lucko.luckperms.api.data.Callback;

import java.util.UUID;

/**
 * Wrapper interface for the internal Datastore instance
 * The implementations of this interface limit access to the datastore and add parameter checks to further prevent
 * errors and ensure all API interactions to not damage the state of the plugin.
 */
@SuppressWarnings("unused")
public interface Datastore {

    String getName();
    boolean isAcceptingLogins();

    Async async();
    Sync sync();

    interface Async {
        void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback);
        void loadUser(UUID uuid, Callback<Boolean> callback);
        void saveUser(User user, Callback<Boolean> callback);
        void createAndLoadGroup(String name, Callback<Boolean> callback);
        void loadGroup(String name, Callback<Boolean> callback);
        void loadAllGroups(Callback<Boolean> callback);
        void saveGroup(Group group, Callback<Boolean> callback);
        void deleteGroup(Group group, Callback<Boolean> callback);
        void createAndLoadTrack(String name, Callback<Boolean> callback);
        void loadTrack(String name, Callback<Boolean> callback);
        void loadAllTracks(Callback<Boolean> callback);
        void saveTrack(Track track, Callback<Boolean> callback);
        void deleteTrack(Track track, Callback<Boolean> callback);
        void saveUUIDData(String username, UUID uuid, Callback<Boolean> callback);
        void getUUID(String username, Callback<UUID> callback);
    }

    interface Sync {
        boolean loadOrCreateUser(UUID uuid, String username);
        boolean loadUser(UUID uuid);
        boolean saveUser(User user);
        boolean createAndLoadGroup(String name);
        boolean loadGroup(String name);
        boolean loadAllGroups();
        boolean saveGroup(Group group);
        boolean deleteGroup(Group group);
        boolean createAndLoadTrack(String name);
        boolean loadTrack(String name);
        boolean loadAllTracks();
        boolean saveTrack(Track track);
        boolean deleteTrack(Track track);
        boolean saveUUIDData(String username, UUID uuid);
        UUID getUUID(String username);
    }
}
