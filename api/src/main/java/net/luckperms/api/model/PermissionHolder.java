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

package net.luckperms.api.model;

import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.Tristate;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Generic superinterface for an object which holds permissions.
 */
public interface PermissionHolder {

    /**
     * Represents a way to identify distinct {@link PermissionHolder}s.
     */
    interface Identifier {

        /**
         * The {@link #getType() type} of {@link User} permission holders.
         */
        String USER_TYPE = "user";

        /**
         * The {@link #getType() type} of {@link Group} permission holders.
         */
        String GROUP_TYPE = "group";

        /**
         * Gets the {@link PermissionHolder}s generic name.
         *
         * <p>The result of this method is guaranteed to be a unique identifier for distinct instances
         * of the same type of object.</p>
         *
         * <p>For {@link User}s, this method returns a {@link UUID#toString() string} representation of
         * the users {@link User#getUniqueId() unique id}.</p>
         *
         * <p>For {@link Group}s, this method returns the {@link Group#getName() group name}.</p>
         *
         * <p>The {@link User#getUniqueId()}, {@link User#getUsername()} and {@link Group#getName()} methods
         * define a "tighter" specification for obtaining object identifiers.</p>
         *
         * @return the identifier for this object. Either a uuid string or name.
         */
        @NonNull String getName();

        /**
         * Gets the type of the {@link PermissionHolder}.
         *
         * @return the type
         */
        @NonNull String getType();
    }

    /**
     * Gets the identifier of the holder.
     *
     * @return the identifier
     */
    @NonNull Identifier getIdentifier();

    /**
     * Gets a friendly name for this holder, to be displayed in command output, etc.
     *
     * <p>This will <strong>always</strong> return a value, eventually falling back to
     * {@link Identifier#getName()} if no other "friendlier" identifiers are present.</p>
     *
     * <p>For {@link User}s, this method will attempt to return the {@link User#getUsername() username},
     * before falling back to {@link Identifier#getName()}.</p>
     *
     * <p>For {@link Group}s, this method will attempt to return the groups display name, before
     * falling back to {@link Identifier#getName()}.</p>
     *
     * @return a friendly identifier for this holder
     */
    @NonNull String getFriendlyName();

    /**
     * Gets the holders {@link CachedDataManager} cache.
     *
     * @return the holders cached data.
     */
    @NonNull CachedDataManager getCachedData();

    /**
     * Gets the {@link Data} of a particular type.
     *
     * @param dataType the data type
     * @return the data
     */
    Data getData(@NonNull DataType dataType);

    /**
     * Gets the holders {@link DataType#NORMAL} data.
     *
     * @return the normal data
     */
    @NonNull Data data();

    /**
     * Gets the holders {@link DataType#TRANSIENT} data.
     *
     * <p>Transient permissions only exist for the duration of the session.</p>
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
     * @return the transient data
     */
    @NonNull Data transientData();

    interface Data {

        /**
         * Gets the backing map containing every permission this holder has.
         *
         * <p>This method <b>does not</b> resolve inheritance rules, and returns a
         * view of what's 'in the file'.</p>
         *
         * @return the holders own nodes
         */
        @NonNull Map<ImmutableContextSet, Collection<Node>> getNodes();

        /**
         * Gets a flattened set of {@link Node}s the holder has.
         *
         * <p>Effectively combines the value collections of the map returned by
         * {@link #getNodes()}.</p>
         *
         * @return a flattened set of the holders own nodes
         */
        @NonNull Set<Node> getFlattenedNodes();

        /**
         * Checks to see if the object has a certain permission.
         *
         * @param node              the node to check for
         * @param equalityPredicate how to determine if a node matches
         * @return a Tristate for the holders permission status for the node
         * @throws NullPointerException if the node is null
         */
        @NonNull Tristate containsNode(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate);

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
         */
        @NonNull DataMutateResult addNode(@NonNull Node node);

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
         * @param node                    The node to be set
         * @param temporaryMergeBehaviour The behaviour used to merge temporary permission entries
         * @return the result of the operation
         * @throws NullPointerException if the node is null
         */
        @NonNull TemporaryDataMutateResult addNode(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour);

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
         */
        @NonNull DataMutateResult removeNode(@NonNull Node node);

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
         */
        void clearMatching(@NonNull Predicate<? super Node> test);

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
         */
        void clearParents(@NonNull ContextSet contextSet);

        /**
         * Clears all meta held by the permission holder.
         *
         * <p>Meta nodes in this case, are any nodes which have a {@link MetaNode}, {@link PrefixNode}
         * or {@link SuffixNode} type.</p>
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
         */
        void clearMeta();

        /**
         * Clears all meta held by the permission holder in a specific context.
         *
         * <p>Meta nodes in this case, are any nodes which have a {@link MetaNode}, {@link PrefixNode}
         * or {@link SuffixNode} type.</p>
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
         */
        void clearMeta(@NonNull ContextSet contextSet);

    }

    /**
     * Gets a flattened/squashed view of the holders permissions.
     *
     * <p>This list is constructed using the values
     * of both the transient and enduring backing multimaps.</p>
     *
     * <p>This means that it <b>may contain</b> duplicate entries.</p>
     *
     * <p>Use {@link #getDistinctNodes()} for a view without duplicates.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * @return a list of the holders own nodes.
     */
    @NonNull List<Node> getNodes();

    /**
     * Gets a sorted set of all held nodes.
     *
     * <p>Effectively a sorted version of {@link #getNodes()}, without duplicates. Use the
     * aforementioned method if you don't require either of these attributes.</p>
     *
     * <p>This method <b>does not</b> resolve inheritance rules.</p>
     *
     * @return an immutable set of permissions in priority order
     */
    @NonNull SortedSet<Node> getDistinctNodes();

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
     * @param queryOptions the query options
     * @return a list of nodes
     */
    @NonNull List<Node> resolveInheritedNodes(@NonNull QueryOptions queryOptions);

    /**
     * Gets a mutable sorted set of the nodes that this object has and inherits, filtered by context
     *
     * <p>Nodes are sorted into priority order. The order of inheritance is only important during
     * the process of flattening inherited entries.</p>
     *
     * @param queryOptions the query options
     * @return an immutable sorted set of permissions
     * @throws NullPointerException if the context is null
     */
    @NonNull SortedSet<Node> resolveDistinctInheritedNodes(@NonNull QueryOptions queryOptions);

    /**
     * Removes any temporary permissions that have expired.
     *
     * <p>This method is called periodically by the platform, so it is only necessary to run
     * if you want to guarantee that the current data is totally up-to-date.</p>
     */
    void auditTemporaryNodes();

}
