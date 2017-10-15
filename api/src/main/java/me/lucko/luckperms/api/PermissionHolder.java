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

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An object which holds permissions.
 *
 * <p>Any changes made to permission holding objects will be lost unless the
 * instance is saved back to the {@link Storage}.</p>
 */
@SuppressWarnings("RedundantThrows")
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
     * Sets a permission for the object
     *
     * @param node The node to be set
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node is null
     * @since 2.6
     */
    void setPermission(@Nonnull Node node) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object
     *
     * @param node The node to be set
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     */
    @Nonnull
    DataMutateResult setPermissionUnchecked(@Nonnull Node node);

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
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node is null
     * @since 2.6
     */
    void setTransientPermission(@Nonnull Node node) throws ObjectAlreadyHasException;

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
     * @throws NullPointerException      if the node is null
     * @return the result of the operation
     * @since 3.1
     */
    @Nonnull
    DataMutateResult setTransientPermissionUnchecked(@Nonnull Node node);

    /**
     * Unsets a permission for the object
     *
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    void unsetPermission(@Nonnull Node node) throws ObjectLacksException;

    /**
     * Unsets a permission for the object
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     */
    @Nonnull
    DataMutateResult unsetPermissionUnchecked(@Nonnull Node node);

    /**
     * Unsets a transient permission for the object
     *
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @since 2.6
     */
    void unsetTransientPermission(@Nonnull Node node) throws ObjectLacksException;

    /**
     * Unsets a transient permission for the object
     *
     * @param node The node to be unset
     * @throws NullPointerException if the node is null
     * @return the result of the operation
     * @since 3.1
     */
    @Nonnull
    DataMutateResult unsetTransientPermissionUnchecked(@Nonnull Node node);

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
     * Checks to see if the object has a certain permission
     *
     * @param node The permission node
     * @param b    If the node is true/false(negated)
     * @return true if the object has the permission
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b);

    /**
     * Checks to see the the object has a permission on a certain server
     *
     * @param node   The permission node
     * @param b      If the node is true/false(negated)
     * @param server The server
     * @return true if the object has the permission
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b, @Nonnull String server);

    /**
     * Checks to see the the object has a permission on a certain server and world
     *
     * @param node   The permission node
     * @param b      If the node is true/false(negated)
     * @param server The server
     * @param world  The world
     * @return true if the object has the permission
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b, @Nonnull String server, @Nonnull String world);

    /**
     * Checks to see the the object has a permission
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b, boolean temporary);

    /**
     * Checks to see the the object has a permission on a certain server
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param server    The server to check on
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b, @Nonnull String server, boolean temporary);

    /**
     * Checks to see the the object has a permission on a certain server and world
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param server    The server to check on
     * @param world     The world to check on
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean hasPermission(@Nonnull String node, boolean b, @Nonnull String server, @Nonnull String world, boolean temporary);

    /**
     * Checks to see if the object inherits a certain permission
     *
     * @param node The permission node
     * @param b    If the node is true/false(negated)
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b);

    /**
     * Checks to see the the object inherits a permission on a certain server
     *
     * @param node   The permission node
     * @param b      If the node is true/false(negated)
     * @param server The server
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b, @Nonnull String server);

    /**
     * Checks to see the the object inherits a permission on a certain server and world
     *
     * @param node   The permission node
     * @param b      If the node is true/false(negated)
     * @param server The server
     * @param world  The world
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node server or world is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b, @Nonnull String server, @Nonnull String world);

    /**
     * Checks to see if the object inherits a permission
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b, boolean temporary);

    /**
     * Checks to see if the object inherits a permission on a certain server
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param server    The server
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b, @Nonnull String server, boolean temporary);

    /**
     * Checks to see if the object inherits a permission on a certain server and world
     *
     * @param node      The permission node
     * @param b         If the node is true/false(negated)
     * @param server    The server
     * @param world     The world
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world if invalid
     * @deprecated in favour of {@link #hasPermission(Node)}
     */
    @Deprecated
    boolean inheritsPermission(@Nonnull String node, boolean b, @Nonnull String server, @Nonnull String world, boolean temporary);

    /**
     * Sets a permission for the object
     *
     * @param node  The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node is null
     * @throws IllegalArgumentException  if the node is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(@Nonnull String node, boolean value) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object on a specific server
     *
     * @param node   The node to set
     * @param value  What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node or server is null
     * @throws IllegalArgumentException  if the node or server is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(@Nonnull String node, boolean value, @Nonnull String server) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object on a specific server and world
     *
     * @param node   The node to set
     * @param value  What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param world  The world to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node, server or world is null
     * @throws IllegalArgumentException  if the node, server or world is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(@Nonnull String node, boolean value, @Nonnull String server, @Nonnull String world) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object
     *
     * @param node     The node to set
     * @param value    What to set the node to - true/false(negated)
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node is null
     * @throws IllegalArgumentException  if the node is invalid or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(@Nonnull String node, boolean value, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object on a specific server
     *
     * @param node     The node to set
     * @param value    What to set the node to - true/false(negated)
     * @param server   The server to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node or server is null
     * @throws IllegalArgumentException  if the node/server is invalid or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(@Nonnull String node, boolean value, @Nonnull String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object on a specific server and world
     *
     * @param node     The node to set
     * @param value    What to set the node to - true/false(negated)
     * @param server   The server to set the permission on
     * @param world    The world to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException      if the node, server or world is null
     * @throws IllegalArgumentException  if the node/server/world is invalid, or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, @Nonnull String server, @Nonnull String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Unsets a permission for the object
     *
     * @param node      The node to be unset
     * @param temporary if the permission being removed is temporary
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node, boolean temporary) throws ObjectLacksException;

    /**
     * Unsets a permission for the object
     *
     * @param node The node to be unset
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     *
     * @param node   The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node, @Nonnull String server) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server and world
     *
     * @param node   The node to be unset
     * @param server The server to unset the node on
     * @param world  The world to unset the node on
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node, @Nonnull String server, @Nonnull String world) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     *
     * @param node      The node to be unset
     * @param server    The server to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node, @Nonnull String server, boolean temporary) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server and world
     *
     * @param node      The node to be unset
     * @param server    The server to unset the node on
     * @param world     The world to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException     if the node wasn't already set
     * @throws NullPointerException     if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(@Nonnull String node, @Nonnull String server, @Nonnull String world, boolean temporary) throws ObjectLacksException;

    /**
     * Clears all nodes held by the object on a specific server
     *
     * @param server the server to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearNodes(ContextSet)}
     */
    @Deprecated
    void clearNodes(@Nullable String server);

    /**
     * Clears all nodes held by the object on a specific server and world
     *
     * @param server the server to filter by, can be null
     * @param world the world to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearNodes(ContextSet)}
     */
    @Deprecated
    void clearNodes(@Nullable String server, @Nullable String world);

    /**
     * Clears all parents on a specific server
     *
     * @param server the server to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearParents(ContextSet)}
     */
    @Deprecated
    void clearParents(@Nullable String server);

    /**
     * Clears all parents on a specific server and world
     *
     * @param server the server to filter by, can be null
     * @param world the world to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearParents(ContextSet)}
     */
    @Deprecated
    void clearParents(@Nullable String server, @Nullable String world);

    /**
     * Clears all meta held by the object on a specific server
     *
     * @param server the server to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearMeta(ContextSet)}
     */
    @Deprecated
    void clearMeta(@Nullable String server);

    /**
     * Clears all meta held by the object on a specific server and world
     *
     * @param server the server to filter by, can be null
     * @param world the world to filter by, can be null
     * @since 2.17
     * @deprecated in favour of {@link #clearMeta(ContextSet)}
     */
    @Deprecated
    void clearMeta(@Nullable String server, @Nullable String world);

    /**
     * Clears all meta for a given key.
     *
     * @param key the meta key
     * @param server the server to filter by, can be null
     * @param world the world to filter by, can be null
     * @param temporary whether the query is for temporary nodes or not.
     * @deprecated in favour of {@link #clearMatching(Predicate)}
     */
    @Deprecated
    void clearMetaKeys(@Nonnull String key, @Nullable String server, @Nullable String world, boolean temporary);

}
