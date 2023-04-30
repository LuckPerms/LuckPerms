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

package net.luckperms.api.model.group;

import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

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
 */
public interface GroupManager {

    /**
     * Creates a new group in the plugin's storage provider and then loads it
     * into memory.
     *
     * <p>If a group by the same name already exists, it will be loaded.</p>
     *
     * @param name the name of the group
     * @return the resultant group
     * @throws NullPointerException if the name is null
     */
    @NonNull CompletableFuture<Group> createAndLoadGroup(@NonNull String name);

    /**
     * Loads a group from the plugin's storage provider into memory.
     *
     * <p>Returns an {@link Optional#empty() empty optional} if the group does
     * not exist.</p>
     *
     * @param name the name of the group
     * @return the resultant group
     * @throws NullPointerException if the name is null
     */
    @NonNull CompletableFuture<Optional<Group>> loadGroup(@NonNull String name);

    /**
     * Saves a group's data back to the plugin's storage provider.
     *
     * <p>You should call this after you make any changes to a group.</p>
     *
     * @param group the group to save
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @NonNull CompletableFuture<Void> saveGroup(@NonNull Group group);

    /**
     * Permanently deletes a group from the plugin's storage provider.
     *
     * @param group the group to delete
     * @return a future to encapsulate the operation.
     * @throws NullPointerException  if group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    @NonNull CompletableFuture<Void> deleteGroup(@NonNull Group group);

    /**
     * Loads (or creates) a group from the plugin's storage provider, applies the given
     * {@code action}, then saves the group's data back to storage.
     *
     * <p>This method effectively calls {@link #createAndLoadGroup(String)}, followed by the
     * {@code action}, then {@link #saveGroup(Group)}, and returns an encapsulation of the whole
     * process as a {@link CompletableFuture}. </p>
     *
     * @param name the name of the group
     * @param action the action to apply to the group
     * @return a future to encapsulate the operation
     * @since 5.1
     */
    default @NonNull CompletableFuture<Void> modifyGroup(@NonNull String name, @NonNull Consumer<? super Group> action) {
        /* This default method is overridden in the implementation, and is just here
           to demonstrate what this method does in the API sources. */
        return createAndLoadGroup(name)
                .thenApplyAsync(group -> { action.accept(group); return group; })
                .thenCompose(this::saveGroup);
    }

    /**
     * Loads all groups into memory.
     *
     * @return a future to encapsulate the operation.
     */
    @NonNull CompletableFuture<Void> loadAllGroups();

    /**
     * Searches the {@link Group#data() normal node maps} of all known {@link Group}s for {@link Node}
     * entries matching the given {@link NodeMatcher matcher}.
     *
     * @param matcher the matcher
     * @return the entries which matched
     * @since 5.1
     */
    <T extends Node> @NonNull CompletableFuture<@Unmodifiable Map<String, Collection<T>>> searchAll(@NonNull NodeMatcher<? extends T> matcher);

    /**
     * Searches for a list of groups with a given permission.
     *
     * @param permission the permission to search for
     * @return a list of held permissions, or null if the operation failed
     * @throws NullPointerException if the permission is null
     * @deprecated Use {@link #searchAll(NodeMatcher)} instead
     */
    @Deprecated
    @NonNull CompletableFuture<@Unmodifiable List<HeldNode<String>>> getWithPermission(@NonNull String permission);

    /**
     * Gets a loaded group.
     *
     * @param name the name of the group to get
     * @return a {@link Group} object, if one matching the name exists, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable Group getGroup(@NonNull String name);

    /**
     * Gets a set of all loaded groups.
     *
     * @return a {@link Set} of {@link Group} objects
     */
    @NonNull @Unmodifiable Set<Group> getLoadedGroups();

    /**
     * Check if a group is loaded in memory
     *
     * @param name the name to check for
     * @return true if the group is loaded
     * @throws NullPointerException if the name is null
     */
    boolean isLoaded(@NonNull String name);

}
