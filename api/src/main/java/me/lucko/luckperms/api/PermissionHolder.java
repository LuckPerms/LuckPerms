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
import me.lucko.luckperms.api.manager.GroupManager;
import me.lucko.luckperms.api.manager.UserManager;
import me.lucko.luckperms.api.nodetype.types.MetaType;
import me.lucko.luckperms.api.nodetype.types.PrefixType;
import me.lucko.luckperms.api.nodetype.types.SuffixType;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Generic superinterface for an object which holds permissions.
 */
public interface PermissionHolder {

    /**
     * Gets the objects generic name.
     *
     * <p>The result of this method is guaranteed to be a unique identifier for distinct instances
     * of the same type of object.</p>
     *
     * <p>For {@link User}s, this method returns a {@link UUID#toString() string} representation of
     * the users {@link User#getUuid() unique id}.</p>
     *
     * <p>For {@link Group}s, this method returns the {@link Group#getName() group name}.</p>
     *
     * <p>The {@link User#getUuid()}, {@link User#getName()} and {@link Group#getName()} methods
     * define a "tighter" specification for obtaining object identifiers.</p>
     *
     * @return the identifier for this object. Either a uuid string or name.
     */
    @NonNull
    String getObjectName();

    /**
     * Gets a friendly name for this holder, to be displayed in command output, etc.
     *
     * <p>This will <strong>always</strong> return a value, eventually falling back to
     * {@link #getObjectName()} if no other "friendlier" identifiers are present.</p>
     *
     * <p>For {@link User}s, this method will attempt to return the {@link User#getName() username},
     * before falling back to {@link #getObjectName()}.</p>
     *
     * <p>For {@link Group}s, this method will attempt to return the groups display name, before
     * falling back to {@link #getObjectName()}.</p>
     *
     * @return a friendly identifier for this holder
     * @since 3.2
     */
    @NonNull
    String getFriendlyName();

    /**
     * Gets the holders {@link CachedData} cache.
     *
     * @return the holders cached data.
     * @since 3.2
     */
    @NonNull
    CachedData getCachedData();

    /**
     * Refreshes and applies any changes to the cached holder data.
     *
     * <p>Calling this method is unnecessary in most cases. Cache updates are handled
     * behind the scenes by the implementation.</p>
     *
     * @return the task future
     * @since 4.0
     */
    @NonNull
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
    @NonNull
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
    @NonNull
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
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * @return a list of the holders own nodes.
     * @since 3.3
     */
    @NonNull
    List<Node> getOwnNodes();

    /**
     * Gets a sorted set of all held permissions.
     *
     * <p>Effectively a sorted version of {@link #getOwnNodes()}, without duplicates. Use the
     * aforementioned method if you don't require either of these attributes.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Although this method is named getPermissions, it will actually return all types of node.</p>
     *
     * @return an immutable set of permissions in priority order
     * @since 2.6
     */
    @NonNull
    SortedSet<? extends Node> getPermissions();

    /**
     * Similar to {@link #getPermissions()}, except only including permissions from the enduring
     * node map. (See {@link #getNodes()})
     *
     * <p>Unlike transient permissions, enduring permissions will be saved to storage, and exist
     * after the session.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Although this method is named getEnduringPermissions, it will actually return all types
     * of node.</p>
     *
     * @return a set of nodes
     * @since 2.6
     */
    @NonNull
    Set<? extends Node> getEnduringPermissions();

    /**
     * Similar to {@link #getPermissions()}, except only including permissions from the enduring
     * node map. (See {@link #getTransientNodes()})
     *
     * <p>Transient permissions only exist for the duration of the session.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Although this method is named getTransientPermissions, it will actually return all types
     * of node.</p>
     *
     * @return a set of nodes
     * @since 2.6
     */
    @NonNull
    Set<? extends Node> getTransientPermissions();

    /**
     * A filtered view of this holders nodes, only including permanent entries.
     *
     * <p>Data is sourced from {@link #getOwnNodes()}, filtered, and then collected to a set.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Although this method is named getPermanentPermissionNodes, it will actually return all types
     * of node.</p>
     *
     * @return a set of permanent nodes
     * @since 2.6
     */
    @NonNull
    Set<Node> getPermanentPermissionNodes();

    /**
     * A filtered view of this holders nodes, only including temporary entries.
     *
     * <p>Data is sourced from {@link #getOwnNodes()}, filtered, and then collected to a set.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * <p>Although this method is named getTemporaryPermissionNodes, it will actually return all types
     * of node.</p>
     *
     * @return a set of temporary nodes
     * @since 2.6
     */
    @NonNull
    Set<Node> getTemporaryPermissionNodes();

    /**
     * Recursively resolves this holders permissions.
     *
     * <p>The returned list will contain every inherited
     * node the holder has, in the order that they were inherited in.</p>
     *
     * <p>This means the list will contain duplicates.</p>
     *
     * <p>Inheritance is performed according to the platforms rules, and the order will vary
     * depending on the accumulation order. By default, the holders own nodes are first in the list,
     * with the entries from the end of the inheritance tree appearing last.</p>
     *
     * @param contexts the contexts for the lookup
     * @return a list of nodes
     * @since 3.3
     */
    @NonNull
    List<LocalizedNode> resolveInheritances(@NonNull Contexts contexts);

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
     * <p>Inheritance is performed according to the platforms rules, and the order will vary
     * depending on the accumulation order. By default, the holders own nodes are first in the list,
     * with the entries from the end of the inheritance tree appearing last.</p>
     *
     * @return a list of nodes
     * @since 3.3
     */
    @NonNull
    List<LocalizedNode> resolveInheritances();

    /**
     * Gets a mutable sorted set of the nodes that this object has and inherits, filtered by context
     *
     * <p>Unlike {@link #getAllNodesFiltered(Contexts)}, this method will not filter individual
     * nodes by context. The context is only used to determine which groups should apply.</p>
     *
     * <p>Nodes are sorted into priority order. The order of inheritance is only important during
     * the process of flattening inherited entries.</p>
     *
     * @param contexts the context for the lookup
     * @return an immutable sorted set of permissions
     * @throws NullPointerException if the context is null
     * @since 2.11
     */
    @NonNull
    SortedSet<LocalizedNode> getAllNodes(@NonNull Contexts contexts);

    /**
     * Gets a mutable sorted set of the nodes that this object has and inherits.
     *
     * <p>Unlike {@link #getAllNodes(Contexts)}, this method does not filter by context, at all.</p>
     *
     * <p>Nodes are sorted into priority order. The order of inheritance is only important during
     * the process of flattening inherited entries.</p>
     *
     * @return an immutable sorted set of permissions
     * @throws NullPointerException if the context is null
     * @since 3.3
     */
    @NonNull
    SortedSet<LocalizedNode> getAllNodes();

    /**
     * Gets a mutable set of the nodes that this object has and inherits, filtered by context.
     *
     * <p>Unlike {@link #getAllNodes(Contexts)}, this method WILL filter individual nodes,
     * and only return ones that fully meet the context provided.</p>
     *
     * @param contexts the context for the lookup
     * @return a mutable set of permissions
     * @throws NullPointerException if the context is null
     * @since 2.11
     */
    @NonNull
    Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts);

    /**
     * Converts the output of {@link #getAllNodesFiltered(Contexts)} into string and boolean form,
     * and expands shorthand permissions.
     *
     * @param contexts the context for the lookup
     * @param convertToLowercase if the keys should be made lowercase whilst being exported
     * @return a mutable map of permissions
     */
    @NonNull
    Map<String, Boolean> exportNodes(@NonNull Contexts contexts, boolean convertToLowercase);

    /**
     * Removes any temporary permissions that have expired.
     *
     * <p>This method is called periodically by the platform, so it is only necessary to run
     * if you want to guarentee that the current data is totally up-to-date.</p>
     */
    void auditTemporaryPermissions();

    /**
     * Checks to see if the object has a certain permission.
     *
     * <p>Although this method is named hasPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @param equalityPredicate how to determine if a node matches
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 4.1
     */
    @NonNull
    Tristate hasPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate);

    /**
     * Checks to see if the object has a certain permission.
     *
     * <p>Although this method is named hasTransientPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @param equalityPredicate how to determine if a node matches
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 4.1
     */
    @NonNull
    Tristate hasTransientPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate);

    /**
     * Checks to see if the object inherits a certain permission.
     *
     * <p>Although this method is named inheritsPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @param equalityPredicate how to determine if a node matches
     * @return a Tristate for the holders inheritance status for the node
     * @throws NullPointerException if the node is null
     * @since 4.1
     */
    @NonNull
    Tristate inheritsPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate);

    /**
     * Checks to see if the object has a certain permission.
     *
     * <p>Although this method is named hasPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @NonNull
    Tristate hasPermission(@NonNull Node node);

    /**
     * Checks to see if the object has a certain permission.
     *
     * <p>Although this method is named hasTransientPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @NonNull
    Tristate hasTransientPermission(@NonNull Node node);

    /**
     * Checks to see if the object inherits a certain permission.
     *
     * <p>Although this method is named inheritsPermission, it can be used for all node types.</p>
     *
     * @param node the node to check for
     * @return a Tristate for the holders inheritance status for the node
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    @NonNull
    Tristate inheritsPermission(@NonNull Node node);

    /**
     * Check to see if this holder inherits another group in the global context.
     *
     * <p>"Global context" simply means an empty context set.</p>
     *
     * <p>This method only checks for direct inheritance - one hop up the inheritance tree.</p>
     *
     * @param group The group to check membership of
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.0
     */
    boolean inheritsGroup(@NonNull Group group);

    /**
     * Check to see if this holder inherits another group.
     *
     * <p>This method only checks for direct inheritance - one hop up the inheritance tree.</p>
     *
     * @param group The group to check membership of
     * @param contextSet the context set to filter by
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 4.0
     */
    boolean inheritsGroup(@NonNull Group group, @NonNull ContextSet contextSet);

    /**
     * Sets a permission node for the permission holder.
     *
     * <p>Although this method is named setPermission, it can be used for all node types.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @NonNull
    DataMutateResult setPermission(@NonNull Node node);

    /**
     * Sets a permission node for the permission holder.
     *
     * <p>Although this method is named setPermission, it can be used for all node types.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param node The node to be set
     * @param temporaryMergeBehaviour The behaviour used to merge temporary permission entries
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.3
     */
    @NonNull
    TemporaryDataMutateResult setPermission(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour);

    /**
     * Sets a transient permission for the permission holder.
     *
     * <p>A transient node is a permission that does not persist.
     * Whenever a user logs out of the server, or the server restarts, this permission will
     * disappear. It is never saved to the datastore, and therefore will not apply on other
     * servers.</p>
     *
     * <p>This is useful if you want to temporarily set a permission for a user while they're
     * online, but don't want it to persist, and have to worry about removing it when they log
     * out.</p>
     *
     * <p>For unsetting a transient permission, see {@link #unsetTransientPermission(Node)}.</p>
     *
     * <p>Although this method is named setTransientPermission, it can be used for all node types.</p>
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @NonNull
    DataMutateResult setTransientPermission(@NonNull Node node);

    /**
     * Sets a transient permission for the permission holder.
     *
     * <p>A transient node is a permission that does not persist.
     * Whenever a user logs out of the server, or the server restarts, this permission will
     * disappear. It is never saved to the datastore, and therefore will not apply on other
     * servers.</p>
     *
     * <p>This is useful if you want to temporarily set a permission for a user while they're
     * online, but don't want it to persist, and have to worry about removing it when they log
     * out.</p>
     *
     * <p>For unsetting a transient permission, see {@link #unsetTransientPermission(Node)}.</p>
     *
     * <p>Although this method is named setTransientPermission, it can be used for all node types.</p>
     *
     * @param node The node to be se
     * @param temporaryMergeBehaviour The behaviour used to merge temporary permission entries
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.3
     */
    @NonNull
    TemporaryDataMutateResult setTransientPermission(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour);

    /**
     * Unsets a permission for the permission holder.
     *
     * <p>Although this method is named unsetPermission, it can be used for all node types.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param node The node to be unset
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @NonNull
    DataMutateResult unsetPermission(@NonNull Node node);

    /**
     * Unsets a transient permission for the permission holder.
     *
     * <p>Although this method is named unsetTransientPermission, it can be used for all node types.</p>
     *
     * @param node The node to be unset
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 4.0
     */
    @NonNull
    DataMutateResult unsetTransientPermission(@NonNull Node node);

    /**
     * Clears any nodes from the holder which pass the predicate.
     *
     * <p>This method only targets enduring data.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param test the predicate to test for nodes which should be removed
     * @since 3.2
     */
    void clearMatching(@NonNull Predicate<Node> test);

    /**
     * Clears any transient nodes from the holder which pass the predicate.
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param test the predicate to test for nodes which should be removed
     * @since 3.2
     */
    void clearMatchingTransient(@NonNull Predicate<Node> test);

    /**
     * Clears all nodes held by the permission holder.
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @since 2.17
     */
    void clearNodes();

    /**
     * Clears all nodes held by the permission holder in a specific context.
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearNodes(@NonNull ContextSet contextSet);

    /**
     * Clears all parent groups.
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @since 2.17
     */
    void clearParents();

    /**
     * Clears all parent groups in a specific context.
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearParents(@NonNull ContextSet contextSet);

    /**
     * Clears all meta held by the permission holder.
     *
     * <p>Meta nodes in this case, are any nodes which have a {@link MetaType}, {@link PrefixType}
     * or {@link SuffixType} type.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @since 2.17
     */
    void clearMeta();

    /**
     * Clears all meta held by the permission holder in a specific context.
     *
     * <p>Meta nodes in this case, are any nodes which have a {@link MetaType}, {@link PrefixType}
     * or {@link SuffixType} type.</p>
     *
     * <p>The effect of this mutate operation will not persist in storage unless changes are
     * explicitly saved. If changes are not saved, the effect will only be observed until the next
     * time the holders permission data is (re)loaded. Changes to {@link User}s should be saved
     * using {@link UserManager#saveUser(User)}, and changes to {@link Group}s should be saved
     * using {@link GroupManager#saveGroup(Group)}.</p>
     *
     * <p>Before making changes to a user or group, it may be a good idea to load a fresh copy of
     * the backing data from the storage if you haven't done so already, to avoid overwriting changes
     * made already. This can be done via {@link UserManager#loadUser(UUID)} or
     * {@link GroupManager#loadGroup(String)} respectively.</p>
     *
     * @param contextSet the contexts to filter by
     * @since 3.2
     */
    void clearMeta(@NonNull ContextSet contextSet);

    /**
     * Clears all transient nodes the permission holder has.
     */
    void clearTransientNodes();

    /**
     * Sets a permission for the permission holder.
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 3.1
     * @deprecated now forwards to {@link #setPermission(Node)}.
     */
    @Deprecated
    default @NonNull DataMutateResult setPermissionUnchecked(@NonNull Node node) {
        return setPermission(node);
    }

    /**
     * Sets a transient permission for the permission holder.
     *
     * @param node The node to be set
     * @return the result of the operation
     * @throws NullPointerException if the node is null
     * @since 3.1
     * @deprecated now forwards to {@link #setTransientPermission(Node)}
     */
    @Deprecated
    default @NonNull DataMutateResult setTransientPermissionUnchecked(@NonNull Node node) {
        return setTransientPermission(node);
    }

    /**
     * Unsets a permission for the permission holder.
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     * @deprecated now forwards to {@link #unsetPermission(Node)}
     */
    @Deprecated
    default @NonNull DataMutateResult unsetPermissionUnchecked(@NonNull Node node) {
        return unsetPermission(node);
    }

    /**
     * Unsets a transient permission for the permission holder.
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     * @deprecated now forwards to {@link #unsetTransientPermission(Node)}
     */
    @Deprecated
    default @NonNull DataMutateResult unsetTransientPermissionUnchecked(@NonNull Node node) {
        return unsetTransientPermission(node);
    }

}
