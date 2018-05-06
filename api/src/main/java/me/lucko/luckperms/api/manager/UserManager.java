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

package me.lucko.luckperms.api.manager;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.api.Storage;
import me.lucko.luckperms.api.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the object responsible for managing {@link User} instances.
 *
 * <p>Note that User instances are automatically loaded for online players.
 * It's likely that offline players will not have an instance pre-loaded.</p>
 *
 * <p>All blocking methods return {@link CompletableFuture}s, which will be
 * populated with the result once the data has been loaded/saved asynchronously.
 * Care should be taken when using such methods to ensure that the main server
 * thread is not blocked.</p>
 *
 * <p>Methods such as {@link CompletableFuture#get()} and equivalent should
 * <strong>not</strong> be called on the main server thread. If you need to use
 * the result of these operations on the main server thread, register a
 * callback using {@link CompletableFuture#thenAcceptAsync(Consumer, Executor)}.</p>
 *
 * @since 4.0
 */
public interface UserManager {

    /**
     * Loads a user from the plugin's storage provider into memory.
     *
     * <p>This method is effectively the same as
     * {@link Storage#loadUser(UUID, String)}, however, the Future returns the
     * resultant user instance instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a user cannot be loaded,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param uuid the uuid of the user
     * @param username the username, if known
     * @return the resultant user
     * @throws NullPointerException if the uuid is null
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<User> loadUser(@Nonnull UUID uuid, @Nullable String username);

    /**
     * Loads a user from the plugin's storage provider into memory.
     *
     * <p>This method is effectively the same as {@link Storage#loadUser(UUID)},
     * however, the Future returns the resultant user instance instead of a
     * boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a user cannot be loaded,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param uuid the uuid of the user
     * @return the resultant user
     * @throws NullPointerException if the uuid is null
     * @since 4.1
     */
    @Nonnull
    default CompletableFuture<User> loadUser(@Nonnull UUID uuid) {
        return loadUser(uuid, null);
    }

    /**
     * Uses the LuckPerms cache to find a uuid for the given username.
     *
     * <p>This lookup is case insensitive.</p>
     *
     * @param username the username
     * @return a uuid, could be null
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<UUID> lookupUuid(@Nonnull String username);

    /**
     * Uses the LuckPerms cache to find a username for the given uuid.
     *
     * @param uuid the uuid
     * @return a username, could be null
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<String> lookupUsername(@Nonnull UUID uuid);

    /**
     * Saves a user's data back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to a user.</p>
     *
     * <p>This method is effectively the same as {@link Storage#saveUser(User)},
     * however, the Future returns void instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a user cannot be saved,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param user the user to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if user is null
     * @throws IllegalStateException if the user instance was not obtained from LuckPerms.
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Void> saveUser(@Nonnull User user);

    /**
     * Saves data about a player to the uuid caching system.
     *
     * @param uuid     the users mojang unique id
     * @param username the users username
     * @return the result of the operation.
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<PlayerSaveResult> savePlayerData(@Nonnull UUID uuid, @Nonnull String username);

    /**
     * Gets a set all "unique" user UUIDs.
     *
     * <p>"Unique" meaning the user isn't just a member of the "default" group.</p>
     *
     * @return a set of uuids
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<Set<UUID>> getUniqueUsers();

    /**
     * Searches for a list of users with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions
     * @throws NullPointerException if the permission is null
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<List<HeldPermission<UUID>>> getWithPermission(@Nonnull String permission);

    /**
     * Gets a loaded user.
     *
     * @param uuid the uuid of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the uuid is null
     */
    @Nullable
    User getUser(@Nonnull UUID uuid);

    /**
     * Gets a loaded user.
     *
     * @param uuid the uuid of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the uuid is null
     */
    @Nonnull
    default Optional<User> getUserOpt(@Nonnull UUID uuid) {
        return Optional.ofNullable(getUser(uuid));
    }

    /**
     * Gets a loaded user.
     *
     * @param name the username of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    User getUser(@Nonnull String name);

    /**
     * Gets a loaded user.
     *
     * @param name the username of the user to get
     * @return an optional {@link User} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<User> getUserOpt(@Nonnull String name) {
        return Optional.ofNullable(getUser(name));
    }

    /**
     * Gets a set of all loaded users.
     *
     * @return a {@link Set} of {@link User} objects
     */
    @Nonnull
    Set<User> getLoadedUsers();

    /**
     * Check if a user is loaded in memory
     *
     * @param uuid the uuid to check for
     * @return true if the user is loaded
     * @throws NullPointerException if the uuid is null
     */
    boolean isLoaded(@Nonnull UUID uuid);

    /**
     * Unload a user from the internal storage, if they're not currently online.
     *
     * @param user the user to unload
     * @throws NullPointerException if the user is null
     */
    void cleanupUser(@Nonnull User user);

}
