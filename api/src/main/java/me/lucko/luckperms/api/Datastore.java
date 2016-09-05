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

package me.lucko.luckperms.api;

import me.lucko.luckperms.api.data.Callback;

import java.util.Set;
import java.util.UUID;

/**
 * Interface for the internal Datastore instance
 */
@SuppressWarnings("unused")
public interface Datastore {

    String getName();
    boolean isAcceptingLogins();

    /**
     * Gets the {@link Sync} interface.
     *
     * All operations through this interface are called immediately and in the same thread as they are called.
     * Datastore operations are thread blocking, and Sync operations should not be called on the main server thread.
     * @return the sync interface
     */
    Sync sync();

    /**
     * Gets the {@link Async} interface.
     * 
     * All operations through this interface are called in a new, separate asynchronous thread.
     * When the operation is complete, the provided callback method is called, in applicable, in the main server thread.
     * @return the async interface
     */
    Async async();

    /**
     * Gets the {@link Future} interface.
     * 
     * All operations through this interface are called in a new, separate asynchronous thread, similar to {@link Async}.
     * The only difference is that instead of providing a callback, a {@link java.util.concurrent.Future} is returned.
     * See the Oracle JavaDocs for usage of the Future class.
     * @return the future interface
     */
    Future future();

    /**
     * All operations through this interface are called immediately and in the same thread as they are called.
     * Datastore operations are thread blocking, and Sync operations should not be called on the main server thread.
     */
    interface Sync {

        /**
         * Saves an action to the datastore
         * @param entry the log entry to be saved
         * @return true if the operation completed successfully.
         * @throws NullPointerException if entry is null
         */
        boolean logAction(LogEntry entry);

        /**
         * Loads and returns the log from the datastore
         * @return a log instance, could be null
         */
        Log getLog();

        /**
         * Either loads or creates a user object, and loads it into the plugins internal storage
         * @param uuid the uuid of the user
         * @param username the users username. (if you want to specify <code>null</code> here, just input "null" as a string.)
         * @return true if the operation completed successfully.
         * @throws NullPointerException if uuid or username is null
         * @throws IllegalArgumentException if either of the parameters are invalid
         * @deprecated functionality of this method is taken on by {@link #loadUser(UUID, String)}
         */
        @Deprecated
        boolean loadOrCreateUser(UUID uuid, String username);

        /**
         * Loads a user from the datastore into the plugins internal storage.
         * @param uuid the uuid of the user to load
         * @return true if the user exists, and was loaded correctly.
         * @throws NullPointerException if uuid is null
         * @deprecated replaced by {@link #loadUser(UUID, String)}
         */
        @Deprecated
        boolean loadUser(UUID uuid);

        /**
         * Loads a user's data into the plugins internal storage.
         * @param uuid the uuid of the user to load
         * @param username the users username. (if you want to specify <code>null</code> here, just input "null" as a string.)
         * @return if the operation was performed successfully
         * @throws NullPointerException if uuid or username is null
         * @since 2.6
         */
        boolean loadUser(UUID uuid, String username);

        /**
         * Saves a user object into the datastore. You should call this after you make any changes to a user.
         * @param user the user to save
         * @return true if the operation completed successfully.
         * @throws NullPointerException if user is null
         * @throws IllegalStateException if the user instance was not obtained from LuckPerms.
         */
        boolean saveUser(User user);

        /**
         * Removes users from the datastore who are "default". This is called every time the plugin loads.
         * @return true if the operation completed successfully
         * @since 2.6
         */
        boolean cleanupUsers();

        /**
         * Gets a set all user's UUIDs who are "unique", and aren't just a member of the "default" group.
         * @return a set of uuids, or null if the operation failed.
         * @since 2.6
         */
        Set<UUID> getUniqueUsers();

        /**
         * Creates and loads a group into the plugins internal storage
         * @param name the name of the group
         * @return true if the operation completed successfully
         * @throws NullPointerException if name is null
         * @throws IllegalArgumentException if the name is invalid
         */
        boolean createAndLoadGroup(String name);

        /**
         * Loads a group into the plugins internal storage.
         * @param name the name of the group
         * @return true if the operation completed successfully
         * @throws NullPointerException if name is null
         * @throws IllegalArgumentException if the name is invalid
         */
        boolean loadGroup(String name);

        /**
         * Loads all groups from the datastore into the plugins internal storage
         * @return true if the operation completed successfully.
         */
        boolean loadAllGroups();

        /**
         * Saves a group back to the datastore. You should call this after you make any changes to a group.
         * @param group the group to save
         * @return true if the operation completed successfully.
         * @throws NullPointerException if group is null
         * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
         */
        boolean saveGroup(Group group);

        /**
         * Permanently deletes a group from the datastore
         * @param group the group to delete
         * @return true if the operation completed successfully.
         * @throws NullPointerException if group is null
         * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
         */
        boolean deleteGroup(Group group);

        /**
         * Creates and loads a track into the plugins internal storage
         * @param name the name of the track
         * @return true if the operation completed successfully
         * @throws NullPointerException if name is null
         * @throws IllegalArgumentException if the name is invalid
         */
        boolean createAndLoadTrack(String name);

        /**
         * Loads a track into the plugins internal storage.
         * @param name the name of the track
         * @return true if the operation completed successfully
         * @throws NullPointerException if name is null
         * @throws IllegalArgumentException if the name is invalid
         */
        boolean loadTrack(String name);

        /**
         * Loads all tracks from the datastore into the plugins internal storage
         * @return true if the operation completed successfully.
         */
        boolean loadAllTracks();

        /**
         * Saves a track back to the datastore. You should call this after you make any changes to a track.
         * @param track the track to save
         * @return true if the operation completed successfully.
         * @throws NullPointerException if track is null
         * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
         */
        boolean saveTrack(Track track);

        /**
         * Permanently deletes a track from the datastore
         * @param track the track to delete
         * @return true if the operation completed successfully.
         * @throws NullPointerException if track is null
         * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
         */
        boolean deleteTrack(Track track);

        /**
         * Saves UUID caching data to the datastore
         * @param username the users username
         * @param uuid the users mojang unique id
         * @return true if the operation completed successfully.
         * @throws NullPointerException if either parameters are null
         * @throws IllegalArgumentException if the username is invalid
         */
        boolean saveUUIDData(String username, UUID uuid);

        /**
         * Gets a UUID from a username
         * @param username the corresponding username
         * @return a uuid object, could be null
         * @throws NullPointerException if either parameters are null
         * @throws IllegalArgumentException if the username is invalid
         */
        UUID getUUID(String username);
    }

    /**
     * All operations through this interface are called in a new, separate asynchronous thread.
     * When the operation is complete, the provided callback method is called, in applicable, in the main server thread.
     * 
     * See {@link Sync} for method documentation. 
     */
    interface Async {
        void logAction(LogEntry entry, Callback<Boolean> callback);
        void getLog(Callback<Log> callback);
        @Deprecated
        void loadOrCreateUser(UUID uuid, String username, Callback<Boolean> callback);
        @Deprecated
        void loadUser(UUID uuid, Callback<Boolean> callback);
        void loadUser(UUID uuid, String username, Callback<Boolean> callback);
        void saveUser(User user, Callback<Boolean> callback);
        void cleanupUsers(Callback<Boolean> callback);
        void getUniqueUsers(Callback<Set<UUID>> callback);
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

    /**
     * All operations through this interface are called in a new, separate asynchronous thread, similar to {@link Async}.
     * The only difference is that instead of providing a callback, a {@link java.util.concurrent.Future} is returned.
     * See the Oracle JavaDocs for usage of the Future class.
     * 
     * See {@link Sync} for method documentation.
     */
    interface Future {
        java.util.concurrent.Future<Boolean> logAction(LogEntry entry);
        java.util.concurrent.Future<Log> getLog();
        @Deprecated
        java.util.concurrent.Future<Boolean> loadOrCreateUser(UUID uuid, String username);
        @Deprecated
        java.util.concurrent.Future<Boolean> loadUser(UUID uuid);
        java.util.concurrent.Future<Boolean> loadUser(UUID uuid, String username);
        java.util.concurrent.Future<Boolean> saveUser(User user);
        java.util.concurrent.Future<Boolean> cleanupUsers();
        java.util.concurrent.Future<Set<UUID>> getUniqueUsers();
        java.util.concurrent.Future<Boolean> createAndLoadGroup(String name);
        java.util.concurrent.Future<Boolean> loadGroup(String name);
        java.util.concurrent.Future<Boolean> loadAllGroups();
        java.util.concurrent.Future<Boolean> saveGroup(Group group);
        java.util.concurrent.Future<Boolean> deleteGroup(Group group);
        java.util.concurrent.Future<Boolean> createAndLoadTrack(String name);
        java.util.concurrent.Future<Boolean> loadTrack(String name);
        java.util.concurrent.Future<Boolean> loadAllTracks();
        java.util.concurrent.Future<Boolean> saveTrack(Track track);
        java.util.concurrent.Future<Boolean> deleteTrack(Track track);
        java.util.concurrent.Future<Boolean> saveUUIDData(String username, UUID uuid);
        java.util.concurrent.Future<UUID> getUUID(String username);
    }
}
