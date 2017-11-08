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

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

/**
 * An object which holds permissions.
 *
 * <p>Any changes made to permission holding objects will be lost unless the
 * instance is saved back to the {@link Storage}.</p>
 */
public interface PermissionHolder {

    /**
     * Gets the objects name.
     *
     * <p>{@link User#getUuid()}, {@link User#getName()} or {@link Group#getName()} should normally be used instead of
     * this method.</p>
     *
     * @return the identifier for this object. Either a uuid string or name.
     */
    @Nonnull
    String getObjectName();

    /**
     * Gets a friendly name for this holder, to be displayed in command output, etc.
     *
     * <p>This will <strong>always</strong> return a value, eventually falling back to {@link #getObjectName()} if no
     * other "friendlier" identifiers are present.</p>
     *
     * @return a friendly identifier for this holder
     * @since 3.2
     */
    @Nonnull
    String getFriendlyName();

    /**
     * Gets the holders {@link CachedData} cache.
     *
     * @return the holders cached data.
     * @since 3.2
     */
    @Nonnull
    CachedData getCachedData();

    /**
     * Refreshes and applies any changes to the cached holder data.
     *
     * @return the task future
     * @since 4.0
     */
    @Nonnull
    CompletableFuture<Void> refreshCachedData();

    /**
     * Gets the backing multimap containing every permission this holder has.
     *
     * <p>This method <b>does not</b> resolve inheritance rules, and returns a
     * view of what's 'in the file'.</p>
     *
     * @return the holders own permissions
     * @since 3.3
     */
    @Nonnull
    ImmutableSetMultimap<ImmutableContextSet, Node> getNodes();

    /**
     * Gets the backing multimap containing every transient permission this holder has.
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Transient permissions only exist for the duration of the session.</p>
     *
     * @return the holders own permissions
     * @since 3.3
     */
    @Nonnull
    ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes();

    /**
     * Gets a flattened/squashed view of the holders permissions.
     *
     * <p>This list is constructed using the {@link Multimap#values()} method
     * of both the transient and enduring backing multimaps.</p>
     *
     * <p>This means that it <b>may contain</b> duplicate entries.</p>
     *
     * <p>Use {@link #getPermissions()} for a view without duplicates.</p>
     *
     * @return a list of the holders own nodes.
     * @since 3.3
     */
    @Nonnull
    List<Node> getOwnNodes();

    /**
     * Gets a sorted set of all held permissions.
     *
     * @return an immutable set of permissions in priority order
     * @since 2.6
     */
    @Nonnull
    SortedSet<? extends Node> getPermissions();

    /**
     * Similar to {@link #getPermissions()}, except without transient permissions.
     *
     * <p>Unlike transient permissions, enduring permissions will be saved to storage, and exist after the session.</p>
     *
     * @return a set of nodes
     * @since 2.6
     */
    @Nonnull
    Set<? extends Node> getEnduringPermissions();

    /**
     * Similar to {@link #getPermissions()}, except without enduring permissions.
     *
     * <p>Transient permissions only exist for the duration of the session.</p>
     *
     * @return a set of nodes
     * @since 2.6
     */
    @Nonnull
    Set<? extends Node> getTransientPermissions();

    /**
     * Processes the nodes and returns the non-temporary ones.
     *
     * @return a set of permanent nodes
     * @since 2.6
     */
    @Nonnull
    Set<Node> getPermanentPermissionNodes();

    /**
     * Processes the nodes and returns the temporary ones.
     *
     * @return a set of temporary nodes
     * @since 2.6
     */
    @Nonnull
    Set<Node> getTemporaryPermissionNodes();

    /**
     * Recursively resolves this holders permissions.
     *
     * <p>The returned list will contain every inherited
     * node the holder has, in the order that they were inherited in.</p>
     *
     * <p>This means the list will contain duplicates.</p>
     *
     * @param contexts the contexts for the lookup
     * @return a list of nodes
     * @since 3.3
     */
    @Nonnull
    List<LocalizedNode> resolveInheritances(Contexts contexts);

    /**
     * Recursively resolves this holders permissions.
     *
     * <p>The returned list will contain every inherited
     * node the holder has, in the order that they were inherited in.</p>
     *
     * <p>This means the list will contain duplicates.</p>
     *
     * <p>Unlike {@link #resolveInheritances(Contexts)}, this method does not
     * filter by context, at all.</p>
     *
     * @return a list of nodes
     * @since 3.3
     */
    @Nonnull
    List<LocalizedNode> resolveInheritances();

    /**
     * Gets a mutable sorted set of the nodes that this object has and inherits, filtered by context
     *
     * <p>Unlike {@link #getAllNodesFiltered(Contexts)}, this method will not filter individual nodes. The context is only
     * used to determine which groups should apply.</p>
     *
     * <p>Nodes are sorted into priority order.</p>
     *
     * @param contexts the context for the lookup
     * @return a mutable sorted set of permissions
     * @throws NullPointerException if the context is null
     * @since 2.11
     */
    @Nonnull
    SortedSet<LocalizedNode> getAllNodes(@Nonnull Contexts contexts);

    /**
     * Gets a mutable sorted set of the nodes that this object has and inherits.
     *
     * <p>Unlike {@link #getAllNodes(Contexts)}, this method does not filter by context, at all.</p>
     *
     * <p>Nodes are sorted into priority order.</p>
     *
     * @return a mutable sorted set of permissions
     * @throws NullPointerException if the context is null
     * @since 3.3
     */
    @Nonnull
    SortedSet<LocalizedNode> getAllNodes();

    /**
     * Gets a mutable set of the nodes that this object has and inherits, filtered by context.
     *
     * <p>Unlike {@link #getAllNodes(Contexts)}, this method WILL filter individual nodes, and only return ones that fully
     * meet the context provided.</p>
     *
     * @param contexts the context for the lookup
     * @return a mutable set of permissions
     * @throws NullPointerException if the context is null
     * @since 2.11
     */
    @Nonnull
    Set<LocalizedNode> getAllNodesFiltered(@Nonnull Contexts contexts);

    /**
     * Converts the output of {@link #getAllNodesFiltered(Contexts)}, and expands shorthand permissions.
     *
     * @param contexts the context for the lookup
     * @param lowerCase if the keys should be made lowercase whilst being exported
     * @return a mutable map of permissions
     */
    @Nonnull
    Map<String, Boolean> exportNodes(@Nonnull Contexts contexts, boolean lowerCase);

    /**
     * Removes temporary permissions that have expired
     */
    void auditTemporaryPermissions();

    /**
     * Checks to see if the object has a certain permission
     *
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @Nonnull
    Tristate hasPermission(@Nonnull Node node);

    /**
     * Checks to see if the object has a certain permission
     *
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @Nonnull
    Tristate hasTransientPermission(@Nonnull Node node);

    /**
     * Checks to see if the object inherits a certain permission
     *
     * @param node the node to check for
     * @return a Tristate for the holders inheritance status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @Nonnull
    Tristate inheritsPermission(@Nonnull Node node);

    /**
     * Check to see if this holder inherits another group directly
     *
     * @param group The group to check membership of
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.0
     */
    boolean inheritsGroup(@Nonnull Group group);

    /**
     * Check to see if this holder inherits another group directly
     *
     * @param group The group to check membership of
     * @param contextSet the context set to filter by
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.0
     */
    boolean inheritsGroup(@Nonnull Group group, @Nonnull ContextSet contextSet);

    /**
     * Sets a permission for the object
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @Nonnull
    DataMutateResult setPermission(@Nonnull Node node);

    /**
     * Sets a transient permission for the object
     *
     * <p>A transient node is a permission that does not persist.
     * Whenever a user logs out of the server, or the server restarts, this permission will disappear.
     * It is never saved to the datastore, and therefore will not apply on other servers.</p>
     *
     * <p>This is useful if you want to temporarily set a permission for a user while they're online, but don't
     * want it to persist, and have to worry about removing it when they log out.</p>
     *
     * <p>For unsetting a transient permission, see {@link #unsetTransientPermission(Node)}</p>
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @Nonnull
    DataMutateResult setTransientPermission(@Nonnull Node node);

    /**
     * Unsets a permission for the object
     *
     * @param node The node to be unset
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @Nonnull
    DataMutateResult unsetPermission(@Nonnull Node node);

    /**
     * Unsets a transient permission for the object
     *
     * @param node The node to be unset
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @Nonnull
    DataMutateResult unsetTransientPermission(@Nonnull Node node);

    /**
     * Clears any nodes from the holder which pass the predicate
     *
     * @param test the predicate to test for nodes which should be removed
     * @since 3.2
     */
    void clearMatching(@Nonnull Predicate<Node> test);

    /**
     * Clears any transient nodes from the holder which pass the predicate
     *
     * @param test the predicate to test for nodes which should be removed
     * @since 3.2
     */
    void clearMatchingTransient(@Nonnull Predicate<Node> test);

    /**
     * Clears all nodes held by the object
     *
     * @since 2.17
     */
    void clearNodes();

    /**
     * Clears all nodes held by the object in a specific context
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearNodes(@Nonnull ContextSet contextSet);

    /**
     * Clears all parent groups
     *
     * @since 2.17
     */
    void clearParents();

    /**
     * Clears all parent groups in a specific context
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearParents(@Nonnull ContextSet contextSet);

    /**
     * Clears all meta held by the object
     *
     * @since 2.17
     */
    void clearMeta();

    /**
     * Clears all meta held by the object in a specific context
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearMeta(@Nonnull ContextSet contextSet);

    /**
     * Clears all transient permissions the holder has.
     */
    void clearTransientNodes();

    /**
     * Sets a permission for the object
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 3.1
     * @deprecated now forwards to {@link #setPermission(Node)}.
     */
    @Nonnull
    @Deprecated
    default DataMutateResult setPermissionUnchecked(@Nonnull Node node) {
        return setPermission(node);
    }

    /**
     * Sets a transient permission for the object
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 3.1
     * @deprecated now forwards to {@link #setTransientPermission(Node)}
     */
    @Nonnull
    @Deprecated
    default DataMutateResult setTransientPermissionUnchecked(@Nonnull Node node) {
        return setTransientPermission(node);
    }

    /**
     * Unsets a permission for the object
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     * @deprecated now forwards to {@link #unsetPermission(Node)}
     */
    @Nonnull
    @Deprecated
    default DataMutateResult unsetPermissionUnchecked(@Nonnull Node node) {
        return unsetPermission(node);
    }

    /**
     * Unsets a transient permission for the object
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     * @deprecated now forwards to {@link #unsetTransientPermission(Node)}
     */
    @Nonnull
    @Deprecated
    default DataMutateResult unsetTransientPermissionUnchecked(@Nonnull Node node) {
        return unsetTransientPermission(node);
    }

}
