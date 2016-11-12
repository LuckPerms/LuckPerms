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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Interface for the internal Storage instance
 *
 * All methods return {@link CompletableFuture}s, which will be populated with the result once the data has been loaded
 * asynchronously. Care should be taken when using the methods to ensure that the main server thread is not blocked.
 *
 * Methods such as {@link CompletableFuture#get()} and equivalent should <strong>not</strong> be called on the main server thread.
 * If you need to use the result of these operations on the main server thread, please register a callback using
 * {@link CompletableFuture#thenAcceptAsync(Consumer, Executor)} and {@link #getSyncExecutor()}.
 *
 * @since 2.14
 */
public interface Storage {

    /**
     * Get the name of the storage implementation.
     * @return the name of the implementation
     */
    String getName();

    /**
     * Return whether the storage instance is allowing logins on the platform.
     * @return true if logins are enabled
     */
    boolean isAcceptingLogins();

    /**
     * Returns an executor which will run all passed runnables on the main server thread.
     * @return an executor instance
     */
    Executor getSyncExecutor();

    /**
     * Returns an executor which will run all passed runnables asynchronously using the platforms scheduler and thread pools.
     * @return an executor instance
     */
    Executor getAsyncExecutor();

    /**
     * Saves an action to storage
     * @param entry the log entry to be saved
     * @return true if the operation completed successfully.
     * @throws NullPointerException if entry is null
     */
    CompletableFuture<Boolean> logAction(LogEntry entry);

    /**
     * Loads and returns the entire log from storage
     * @return a log instance, could be null if loading failed
     */
    CompletableFuture<Log> getLog();

    /**
     * Loads a user's data from the main storage into the plugins local storage.
     * @param uuid the uuid of the user to load
     * @param username the users username. (if you want to specify <code>null</code> here, just input "null" as a string.)
     * @return if the operation completed successfully
     * @throws NullPointerException if uuid or username is null
     */
    CompletableFuture<Boolean> loadUser(UUID uuid, String username);

    /**
     * Saves a user object back to storage. You should call this after you make any changes to a user.
     * @param user the user to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException if user is null
     * @throws IllegalStateException if the user instance was not obtained from LuckPerms.
     */
    CompletableFuture<Boolean> saveUser(User user);

    /**
     * Removes users from the main storage who are "default". This is called every time the plugin loads.
     * @return true if the operation completed successfully
     */
    CompletableFuture<Boolean> cleanupUsers();

    /**
     * Gets a set all "unique" user UUIDs.
     * "Unique" meaning the user isn't just a member of the "default" group.
     * @return a set of uuids, or null if the operation failed.
     */
    CompletableFuture<Set<UUID>> getUniqueUsers();

    /**
     * Creates and loads a group into the plugins local storage
     * @param name the name of the group
     * @return true if the operation completed successfully
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    CompletableFuture<Boolean> createAndLoadGroup(String name);

    /**
     * Loads a group into the plugins local storage.
     * @param name the name of the group
     * @return true if the operation completed successfully
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    CompletableFuture<Boolean> loadGroup(String name);

    /**
     * Loads all groups from the storage into memory
     * @return true if the operation completed successfully.
     */
    CompletableFuture<Boolean> loadAllGroups();

    /**
     * Saves a group back to storage. You should call this after you make any changes to a group.
     * @param group the group to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    CompletableFuture<Boolean> saveGroup(Group group);

    /**
     * Permanently deletes a group from storage.
     * @param group the group to delete
     * @return true if the operation completed successfully.
     * @throws NullPointerException if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    CompletableFuture<Boolean> deleteGroup(Group group);

    /**
     * Creates and loads a track into the plugins local storage
     * @param name the name of the track
     * @return true if the operation completed successfully
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    CompletableFuture<Boolean> createAndLoadTrack(String name);

    /**
     * Loads a track into the plugins local storage.
     * @param name the name of the track
     * @return true if the operation completed successfully
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    CompletableFuture<Boolean> loadTrack(String name);

    /**
     * Loads all tracks from the storage into memory
     * @return true if the operation completed successfully.
     */
    CompletableFuture<Boolean> loadAllTracks();

    /**
     * Saves a track back to storage. You should call this after you make any changes to a track.
     * @param track the track to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException if track is null
     * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
     */
    CompletableFuture<Boolean> saveTrack(Track track);

    /**
     * Permanently deletes a track from storage
     * @param track the track to delete
     * @return true if the operation completed successfully.
     * @throws NullPointerException if track is null
     * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
     */
    CompletableFuture<Boolean> deleteTrack(Track track);

    /**
     * Saves UUID caching data to the global cache
     * @param username the users username
     * @param uuid the users mojang unique id
     * @return true if the operation completed successfully.
     * @throws NullPointerException if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    CompletableFuture<Boolean> saveUUIDData(String username, UUID uuid);

    /**
     * Gets a UUID from a username
     * @param username the corresponding username
     * @return a uuid object, could be null
     * @throws NullPointerException if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    CompletableFuture<UUID> getUUID(String username);

}
