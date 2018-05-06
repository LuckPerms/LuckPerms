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

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.Storage;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents the object responsible for managing {@link Group} instances.
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
public interface GroupManager {

    /**
     * Creates a new group in the plugin's storage provider and then loads it
     * into memory.
     *
     * <p>If a group by the same name already exists, it will be loaded.</p>
     *
     * <p>This method is effectively the same as
     * {@link Storage#createAndLoadGroup(String)}, however, the Future returns
     * the resultant group instance instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a group cannot be loaded,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param name the name of the group
     * @return the resultant group
     * @throws NullPointerException if the name is null
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Group> createAndLoadGroup(@Nonnull String name);

    /**
     * Loads a group from the plugin's storage provider into memory.
     *
     * <p>Returns an {@link Optional#empty() empty optional} if the group does
     * not exist.</p>
     *
     * <p>This method is effectively the same as
     * {@link Storage#loadGroup(String)}, however, the Future returns
     * the resultant group instance instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a group cannot be loaded,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param name the name of the group
     * @return the resultant group
     * @throws NullPointerException if the name is null
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Optional<Group>> loadGroup(@Nonnull String name);

    /**
     * Saves a group's data back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to a group.</p>
     *
     * <p>This method is effectively the same as {@link Storage#saveGroup(Group)},
     * however, the Future returns void instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a group cannot be saved,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @param group the group to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Void> saveGroup(@Nonnull Group group);

    /**
     * Permanently deletes a group from the plugin's storage provider.
     *
     * <p>This method is effectively the same as {@link Storage#deleteGroup(Group)},
     * however, the Future returns void instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a group cannot be deleted,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     *
     * @param group the group to delete
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Void> deleteGroup(@Nonnull Group group);

    /**
     * Loads all groups into memory.
     *
     * <p>This method is effectively the same as {@link Storage#loadAllTracks()},
     * however, the Future returns void instead of a boolean flag.</p>
     *
     * <p>Unlike the method in {@link Storage}, when a group cannot be loaded,
     * the future will be {@link CompletableFuture completed exceptionally}.</p>
     *
     * @return a future to encapsulate the operation.
     * @since 4.1
     */
    @Nonnull
    CompletableFuture<Void> loadAllGroups();

    /**
     * Searches for a list of groups with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions, or null if the operation failed
     * @throws NullPointerException if the permission is null
     * @since 4.2
     */
    @Nonnull
    CompletableFuture<List<HeldPermission<String>>> getWithPermission(@Nonnull String permission);

    /**
     * Gets a loaded group.
     *
     * @param name the name of the group to get
     * @return a {@link Group} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable
    Group getGroup(@Nonnull String name);

    /**
     * Gets a loaded group.
     *
     * <p>This method does not return null, unlike {@link #getGroup}</p>
     *
     * @param name the name of the group to get
     * @return an optional {@link Group} object
     * @throws NullPointerException if the name is null
     */
    @Nonnull
    default Optional<Group> getGroupOpt(@Nonnull String name) {
        return Optional.ofNullable(getGroup(name));
    }

    /**
     * Gets a set of all loaded groups.
     *
     * @return a {@link Set} of {@link Group} objects
     */
    @Nonnull
    Set<Group> getLoadedGroups();

    /**
     * Check if a group is loaded in memory
     *
     * @param name the name to check for
     * @return true if the group is loaded
     * @throws NullPointerException if the name is null
     */
    boolean isLoaded(@Nonnull String name);

}
