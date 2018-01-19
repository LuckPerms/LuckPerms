/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A means of loading and saving permission data to/from the backend.
 *
 * <p>All blocking methods return {@link CompletableFuture}s, which will be populated with the result once the data has been
 * loaded/saved asynchronously. Care should be taken when using such methods to ensure that the main server thread is not
 * blocked.</p>
 *
 * <p>Methods such as {@link CompletableFuture#get()} and equivalent should <strong>not</strong> be called on the main
 * server thread. If you need to use the result of these operations on the main server thread, register a
 * callback using {@link CompletableFuture#thenAcceptAsync(Consumer, Executor)} and {@link #getSyncExecutor()}.</p>
 *
 * @since 2.14
 */
public interface Storage {

    /**
     * Get the name of the storage implementation.
     *
     * @return the name of the implementation
     */
    @Nonnull
    String getName();

    /**
     * Gets whether the storage instance is allowing logins on the platform.
     *
     * @return true if logins are enabled
     */
    boolean isAcceptingLogins();

    /**
     * Returns an executor which will run all passed runnables on the main server thread.
     *
     * @return an executor instance
     */
    @Nonnull
    Executor getSyncExecutor();

    /**
     * Returns an executor which will run all passed runnables asynchronously using the platforms scheduler and thread
     * pools.
     *
     * @return an executor instance
     */
    @Nonnull
    Executor getAsyncExecutor();

    /**
     * Saves an action to storage
     *
     * @param entry the log entry to be saved
     * @return true if the operation completed successfully.
     * @throws NullPointerException if entry is null
     * @deprecated in favour of {@link ActionLogger#submit(LogEntry)}.
     */
    @Nonnull
    @Deprecated
    CompletableFuture<Boolean> logAction(@Nonnull LogEntry entry);

    /**
     * Loads and returns the entire log from storage
     *
     * @return a log instance, could be null if loading failed
     */
    @Nonnull
    CompletableFuture<Log> getLog();

    /**
     * Loads a user's data from the main storage into the plugins local storage.
     *
     * @param uuid     the uuid of the user to load
     * @param username the users username, or null if it is not known.
     * @return if the operation completed successfully
     * @throws NullPointerException if uuid is null
     */
    @Nonnull
    CompletableFuture<Boolean> loadUser(@Nonnull UUID uuid, @Nullable String username);

    /**
     * Loads a user's data from the main storage into the plugins local storage.
     *
     * @param uuid the uuid of the user to load
     * @return if the operation completed successfully
     * @throws NullPointerException if uuid is null
     */
    @Nonnull
    default CompletableFuture<Boolean> loadUser(@Nonnull UUID uuid) {
        return loadUser(uuid, null);
    }

    /**
     * Saves a user object back to storage. You should call this after you make any changes to a user.
     *
     * @param user the user to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException  if user is null
     * @throws IllegalStateException if the user instance was not obtained from LuckPerms.
     */
    @Nonnull
    CompletableFuture<Boolean> saveUser(@Nonnull User user);

    /**
     * Gets a set all "unique" user UUIDs.
     *
     * <p>"Unique" meaning the user isn't just a member of the "default" group.</p>
     *
     * @return a set of uuids, or null if the operation failed.
     */
    @Nonnull
    CompletableFuture<Set<UUID>> getUniqueUsers();

    /**
     * Searches for a list of users with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions, or null if the operation failed
     * @throws NullPointerException if the permission is null
     * @since 2.17
     */
    @Nonnull
    CompletableFuture<List<HeldPermission<UUID>>> getUsersWithPermission(@Nonnull String permission);

    /**
     * Creates and loads a group into the plugins local storage
     *
     * @param name the name of the group
     * @return true if the operation completed successfully
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    @Nonnull
    CompletableFuture<Boolean> createAndLoadGroup(@Nonnull String name);

    /**
     * Loads a group into the plugins local storage.
     *
     * @param name the name of the group
     * @return true if the operation completed successfully
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    @Nonnull
    CompletableFuture<Boolean> loadGroup(@Nonnull String name);

    /**
     * Loads all groups from the storage into memory
     *
     * @return true if the operation completed successfully.
     */
    @Nonnull
    CompletableFuture<Boolean> loadAllGroups();

    /**
     * Saves a group back to storage. You should call this after you make any changes to a group.
     *
     * @param group the group to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nonnull
    CompletableFuture<Boolean> saveGroup(@Nonnull Group group);

    /**
     * Permanently deletes a group from storage.
     *
     * @param group the group to delete
     * @return true if the operation completed successfully.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @Nonnull
    CompletableFuture<Boolean> deleteGroup(@Nonnull Group group);

    /**
     * Searches for a list of groups with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions, or null if the operation failed
     * @throws NullPointerException if the permission is null
     * @since 2.17
     */
    @Nonnull
    CompletableFuture<List<HeldPermission<String>>> getGroupsWithPermission(@Nonnull String permission);

    /**
     * Creates and loads a track into the plugins local storage
     *
     * @param name the name of the track
     * @return true if the operation completed successfully
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    @Nonnull
    CompletableFuture<Boolean> createAndLoadTrack(@Nonnull String name);

    /**
     * Loads a track into the plugins local storage.
     *
     * @param name the name of the track
     * @return true if the operation completed successfully
     * @throws NullPointerException     if name is null
     * @throws IllegalArgumentException if the name is invalid
     */
    @Nonnull
    CompletableFuture<Boolean> loadTrack(@Nonnull String name);

    /**
     * Loads all tracks from the storage into memory
     *
     * @return true if the operation completed successfully.
     */
    @Nonnull
    CompletableFuture<Boolean> loadAllTracks();

    /**
     * Saves a track back to storage. You should call this after you make any changes to a track.
     *
     * @param track the track to save
     * @return true if the operation completed successfully.
     * @throws NullPointerException  if track is null
     * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
     */
    @Nonnull
    CompletableFuture<Boolean> saveTrack(@Nonnull Track track);

    /**
     * Permanently deletes a track from storage
     *
     * @param track the track to delete
     * @return true if the operation completed successfully.
     * @throws NullPointerException  if track is null
     * @throws IllegalStateException if the track instance was not obtained from LuckPerms.
     */
    @Nonnull
    CompletableFuture<Boolean> deleteTrack(@Nonnull Track track);

    /**
     * Saves UUID caching data to the global cache
     *
     * @param username the users username
     * @param uuid     the users mojang unique id
     * @return true if the operation completed successfully.
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    @Nonnull
    CompletableFuture<Boolean> saveUUIDData(@Nonnull String username, @Nonnull UUID uuid);

    /**
     * Gets a UUID from a username
     *
     * @param username the corresponding username
     * @return a uuid object, could be null
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    @Nonnull
    CompletableFuture<UUID> getUUID(@Nonnull String username);

    /**
     * Gets a username from a UUID
     *
     * @param uuid the corresponding uuid
     * @return a name string, could be null
     * @throws NullPointerException if either parameters are null
     * @since 2.17
     */
    @Nonnull
    CompletableFuture<String> getName(@Nonnull UUID uuid);

}
