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

package net.luckperms.api.model.user;

import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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
 */
public interface UserManager {

    /**
     * Loads a user from the plugin's storage provider into memory.
     *
     * @param uniqueId the uuid of the user
     * @param username the username, if known
     * @return the resultant user
     * @throws NullPointerException if the uuid is null
     */
    @NonNull CompletableFuture<User> loadUser(@NonNull UUID uniqueId, @Nullable String username);

    /**
     * Loads a user from the plugin's storage provider into memory.
     *
     * @param uniqueId the uuid of the user
     * @return the resultant user
     * @throws NullPointerException if the uuid is null
     */
    default @NonNull CompletableFuture<User> loadUser(@NonNull UUID uniqueId) {
        return loadUser(uniqueId, null);
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
     */
    @NonNull CompletableFuture<UUID> lookupUniqueId(@NonNull String username);

    /**
     * Uses the LuckPerms cache to find a username for the given uuid.
     *
     * @param uniqueId the uuid
     * @return a username, could be null
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    @NonNull CompletableFuture<String> lookupUsername(@NonNull UUID uniqueId);

    /**
     * Saves a user's data back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to a user.</p>
     *
     * @param user the user to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if user is null
     * @throws IllegalStateException if the user instance was not obtained from LuckPerms.
     */
    @NonNull CompletableFuture<Void> saveUser(@NonNull User user);

    /**
     * Loads a user from the plugin's storage provider, applies the given {@code action},
     * then saves the user's data back to storage.
     *
     * <p>This method effectively calls {@link #loadUser(UUID)}, followed by the {@code action},
     * then {@link #saveUser(User)}, and returns an encapsulation of the whole process as a
     * {@link CompletableFuture}. </p>
     *
     * @param uniqueId the uuid of the user
     * @param action the action to apply to the user
     * @return a future to encapsulate the operation
     * @since 5.1
     */
    default @NonNull CompletableFuture<Void> modifyUser(@NonNull UUID uniqueId, @NonNull Consumer<? super User> action) {
        /* This default method is overridden in the implementation, and is just here
           to demonstrate what this method does in the API sources. */
        return loadUser(uniqueId)
                .thenApplyAsync(user -> { action.accept(user); return user; })
                .thenCompose(this::saveUser);
    }

    /**
     * Saves data about a player to the uuid caching system.
     *
     * @param uniqueId     the users mojang unique id
     * @param username the users username
     * @return the result of the operation.
     * @throws NullPointerException     if either parameters are null
     * @throws IllegalArgumentException if the username is invalid
     */
    @NonNull CompletableFuture<PlayerSaveResult> savePlayerData(@NonNull UUID uniqueId, @NonNull String username);

    /**
     * Deletes any data about a given player from the uuid caching system.
     *
     * <p>Note that this method does not affect any saved user/permissions data.</p>
     *
     * @param uniqueId the users mojang unique id
     * @return a future encapsulating the result of the operation
     * @since 5.2
     */
    @NonNull CompletableFuture<Void> deletePlayerData(@NonNull UUID uniqueId);

    /**
     * Gets a set all "unique" user UUIDs.
     *
     * <p>"Unique" meaning the user isn't just a member of the "default" group.</p>
     *
     * @return a set of uuids
     */
    @NonNull CompletableFuture<@Unmodifiable Set<UUID>> getUniqueUsers();

    /**
     * Searches the {@link User#data() normal node maps} of all known {@link User}s for {@link Node}
     * entries matching the given {@link NodeMatcher matcher}.
     *
     * @param matcher the matcher
     * @return the entries which matched
     * @since 5.1
     */
    <T extends Node> @NonNull CompletableFuture<@Unmodifiable Map<UUID, Collection<T>>> searchAll(@NonNull NodeMatcher<? extends T> matcher);

    /**
     * Searches for a list of users with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions
     * @throws NullPointerException if the permission is null
     * @deprecated Use {@link #searchAll(NodeMatcher)} instead
     */
    @Deprecated
    @NonNull CompletableFuture<@Unmodifiable List<HeldNode<UUID>>> getWithPermission(@NonNull String permission);

    /**
     * Gets a loaded user.
     *
     * @param uniqueId the uuid of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the uuid is null
     */
    @Nullable User getUser(@NonNull UUID uniqueId);

    /**
     * Gets a loaded user.
     *
     * @param username the username of the user to get
     * @return a {@link User} object, if one matching the uuid is loaded, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable User getUser(@NonNull String username);

    /**
     * Gets a set of all loaded users.
     *
     * @return a {@link Set} of {@link User} objects
     */
    @NonNull @Unmodifiable Set<User> getLoadedUsers();

    /**
     * Check if a user is loaded in memory
     *
     * @param uniqueId the uuid to check for
     * @return true if the user is loaded
     * @throws NullPointerException if the uuid is null
     */
    boolean isLoaded(@NonNull UUID uniqueId);

    /**
     * Unload a user from the internal storage, if they're not currently online.
     *
     * @param user the user to unload
     * @throws NullPointerException if the user is null
     */
    void cleanupUser(@NonNull User user);

}
