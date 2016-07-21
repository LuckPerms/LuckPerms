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
        void loadOrCreateUser(UUID uuid, String username, Callback callback);
        void loadUser(UUID uuid, Callback callback);
        void saveUser(User user, Callback callback);
        void createAndLoadGroup(String name, Callback callback);
        void loadGroup(String name, Callback callback);
        void loadAllGroups(Callback callback);
        void saveGroup(Group group, Callback callback);
        void deleteGroup(Group group, Callback callback);
        void createAndLoadTrack(String name, Callback callback);
        void loadTrack(String name, Callback callback);
        void loadAllTracks(Callback callback);
        void saveTrack(Track track, Callback callback);
        void deleteTrack(Track track, Callback callback);
        void saveUUIDData(String username, UUID uuid, Callback callback);
        void getUUID(String username, Callback.GetUUID callback);
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
