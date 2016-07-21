package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.api.Datastore;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.utils.Patterns;

import java.util.UUID;

/**
 * Provides a link between {@link Datastore} and {@link me.lucko.luckperms.data.Datastore}
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class DatastoreLink implements Datastore {

    private final me.lucko.luckperms.data.Datastore master;
    private final Async async;
    private final Sync sync;

    public DatastoreLink(@NonNull me.lucko.luckperms.data.Datastore master) {
        this.master = master;
        this.async = new Async(master);
        this.sync = new Sync(master);
    }

    private static String checkUsername(String s) {
        if (s.length() > 16 || Patterns.NON_USERNAME.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid username entry '" + s + "'. Usernames must be less than 16 chars" +
                    " and only contain 'a-z A-Z 1-9 _'.");
        }
        return s;
    }

    private static String checkName(String s) {
        if (s.length() > 36 || Patterns.NON_ALPHA_NUMERIC.matcher(s).find()) {
            throw new IllegalArgumentException("Invalid name entry '" + s + "'. Names must be less than 37 chars" +
                    " and only contain 'a-z A-Z 1-9'.");
        }
        return s.toLowerCase();
    }

    private static Callback checkCallback(Callback c) {
        // If no callback was given, just send an empty one
        if (c == null) {
            c = success -> {};
        }
        return c;
    }

    private static Callback.GetUUID checkCallback(Callback.GetUUID c) {
        // If no callback was given, just send an empty one
        if (c == null) {
            c = success -> {};
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
        public void loadOrCreateUser(@NonNull UUID uuid, @NonNull String username, Callback callback) {
            master.loadOrCreateUser(uuid, checkUsername(username), checkCallback(callback));
        }

        @Override
        public void loadUser(@NonNull UUID uuid, Callback callback) {
            master.loadUser(uuid, checkCallback(callback));
        }

        @Override
        public void saveUser(@NonNull User user, Callback callback) {
            Utils.checkUser(user);
            master.saveUser(((UserLink) user).getMaster(), checkCallback(callback));
        }

        @Override
        public void createAndLoadGroup(@NonNull String name, Callback callback) {
            master.createAndLoadGroup(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadGroup(@NonNull String name, Callback callback) {
            master.loadGroup(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadAllGroups(Callback callback) {
            master.loadAllGroups(checkCallback(callback));
        }

        @Override
        public void saveGroup(@NonNull Group group, Callback callback) {
            Utils.checkGroup(group);
            master.saveGroup(((GroupLink) group).getMaster(), checkCallback(callback));
        }

        @Override
        public void deleteGroup(@NonNull Group group, Callback callback) {
            Utils.checkGroup(group);
            master.deleteGroup(((GroupLink) group).getMaster(), checkCallback(callback));
        }

        @Override
        public void createAndLoadTrack(@NonNull String name, Callback callback) {
            master.createAndLoadTrack(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadTrack(@NonNull String name, Callback callback) {
            master.loadTrack(checkName(name), checkCallback(callback));
        }

        @Override
        public void loadAllTracks(Callback callback) {
            master.loadAllTracks(checkCallback(callback));
        }

        @Override
        public void saveTrack(@NonNull Track track, Callback callback) {
            Utils.checkTrack(track);
            master.saveTrack(((TrackLink) track).getMaster(), checkCallback(callback));
        }

        @Override
        public void deleteTrack(@NonNull Track track, Callback callback) {
            Utils.checkTrack(track);
            master.deleteTrack(((TrackLink) track).getMaster(), checkCallback(callback));
        }

        @Override
        public void saveUUIDData(@NonNull String username, @NonNull UUID uuid, Callback callback) {
            master.saveUUIDData(checkUsername(username), uuid, checkCallback(callback));
        }

        @Override
        public void getUUID(@NonNull String username, Callback.GetUUID callback) {
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
            Utils.checkUser(user);
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
            Utils.checkGroup(group);
            return master.saveGroup(((GroupLink) group).getMaster());
        }

        @Override
        public boolean deleteGroup(@NonNull Group group) {
            Utils.checkGroup(group);
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
            Utils.checkTrack(track);
            return master.saveTrack(((TrackLink) track).getMaster());
        }

        @Override
        public boolean deleteTrack(@NonNull Track track) {
            Utils.checkTrack(track);
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
