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

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Wrapper interface for internal PermissionHolder (object/group) instances
 */
@SuppressWarnings("unused")
public interface PermissionHolder {

    /**
     * @return the identifier for this object. either a uuid string or name
     * However, you should really just use {@link User#getUuid()}, {@link User#getName()} or {@link Group#getName()}
     */
    String getObjectName();

    /**
     * Gets an immutable Set of the objects permission nodes
     * @return an immutable set of permissions in priority order
     * @since 1.6
     */
    SortedSet<Node> getPermissions();

    /**
     * Similar to {@link #getPermissions()}, except excluding transient permissions
     * @return a set of nodes
     * @since 1.6
     */
    Set<Node> getEnduringPermissions();

    /**
     * Similar to {@link #getPermissions()}, except excluding non-transient permissions
     * @return a set of nodes
     * @since 1.6
     */
    Set<Node> getTransientPermissions();


    /**
     * Gets an immutable set of the nodes that this object has and inherits
     * @return an immutable set of permissions
     * @since 1.6
     */
    Set<Node> getAllNodes();

    /**
     * Gets an immutable Map of the objects permission nodes
     * @return an immutable map of permissions
     * @deprecated in favour of {@link #getPermissions()}
     */
    @Deprecated
    Map<String, Boolean> getNodes();

    /**
     * Checks to see if the object has a certain permission
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    Tristate hasPermission(Node node);

    /**
     * Checks to see if the object has a certain permission
     * @param node the node to check for
     * @return a Tristate for the holders permission status for the node
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    Tristate hasTransientPermission(Node node);

    /**
     * Checks to see if the object has a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the object has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean hasPermission(String node, boolean b);

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the object has the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
    boolean hasPermission(String node, boolean b, String server);

    /**
     * Checks to see the the object has a permission on a certain server and world
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param world The world
     * @return true if the object has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     */
    boolean hasPermission(String node, boolean b, String server, String world);

    /**
     * Checks to see the the object has a permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean hasPermission(String node, boolean b, boolean temporary);

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server to check on
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
    boolean hasPermission(String node, boolean b, String server, boolean temporary);

    /**
     * Checks to see the the object has a permission on a certain server and world
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server to check on
     * @param world The world to check on
     * @param temporary if the permission is temporary
     * @return true if the object has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     */
    boolean hasPermission(String node, boolean b, String server, String world, boolean temporary);

    /**
     * Cheks to see if the object inherits a certain permission
     * @param node the node to check for
     * @return a Tristate for the holders inheritance status for the node
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    Tristate inheritsPermission(Node node);

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean inheritsPermission(String node, boolean b);

    /**
     * Checks to see the the object inherits a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
    boolean inheritsPermission(String node, boolean b, String server);

    /**
     * Checks to see the the object inherits a permission on a certain server and world
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param world The world
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node server or world is invalid
     */
    boolean inheritsPermission(String node, boolean b, String server, String world);

    /**
     * Checks to see if the object inherits a permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean inheritsPermission(String node, boolean b, boolean temporary);

    /**
     * Checks to see if the object inherits a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
    boolean inheritsPermission(String node, boolean b, String server, boolean temporary);

    /**
     * Checks to see if the object inherits a permission on a certain server and world
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param world The world
     * @param temporary if the permission is temporary
     * @return true if the object inherits the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world if invalid
     */
    boolean inheritsPermission(String node, boolean b, String server, String world, boolean temporary);

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    void setPermission(Node node) throws ObjectAlreadyHasException;

    /**
     * Sets a transient permission for the object
     *
     * <p> A transient node is a permission that does not persist.
     * Whenever a user logs out of the server, or the server restarts, this permission will disappear.
     * It is never saved to the datastore, and therefore will not apply on other servers.
     *
     * This is useful if you want to temporarily set a permission for a user while they're online, but don't
     * want it to persist, and have to worry about removing it when they log out.
     *
     * For unsetting a transient permission, see {@link #unsetTransientPermission(Node)}
     *
     * @param node The node to be set
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    void setTransientPermission(Node node) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object on a specific server
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, String server) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object on a specific server and world
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param world The world to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, String server, String world) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object on a specific server
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node/server is invalid or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object on a specific server and world
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param world The world to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node/server/world is invalid, or if the expiry time is in the past
     * @deprecated in favour of {@link #setPermission(Node)}
     */
    @Deprecated
    void setPermission(String node, boolean value, String server, String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    void unsetPermission(Node node) throws ObjectLacksException;

    /**
     * Unsets a transient permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @since 1.6
     */
    void unsetTransientPermission(Node node) throws ObjectLacksException;

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param temporary if the permission being removed is temporary
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node, boolean temporary) throws ObjectLacksException;

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node, String server) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server and world
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param world The world to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node, String server, String world) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node, String server, boolean temporary) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server and world
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param world The world to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     * @deprecated in favour of {@link #unsetPermission(Node)}
     */
    @Deprecated
    void unsetPermission(String node, String server, String world, boolean temporary) throws ObjectLacksException;

    /**
     * Gets the permissions and inherited permissions that apply to a specific server and world
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     * @deprecated in favour of {@link #getPermissions(String, String, Map, boolean, List, boolean)}
     */
    @Deprecated
    Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups, List<String> possibleNodes);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server and world
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     * @deprecated in favour of {@link #getPermissions(String, String, Map, boolean, List, boolean)}
     */
    @Deprecated
    Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     * @deprecated in favour of {@link #getPermissions(String, String, Map, boolean, List, boolean)}
     */
    @Deprecated
    Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups, List<String> possibleNodes);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     * @deprecated in favour of {@link #getPermissions(String, String, Map, boolean, List, boolean)}
     */
    @Deprecated
    Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups);

    /**
     * Convert the holders nodes into a Map of permissions to be applied on the platform
     * @param server the server
     * @param world the world
     * @param extraContext any extra context to filter by
     * @param includeGlobal whether to include global nodes
     * @param possibleNodes a list of possible permissions for resolving wildcards
     * @param applyGroups if inherited group permissions should be included
     * @return a map of permissions
     * @since 1.6
     */
    Map<String, Boolean> getPermissions(String server, String world, Map<String, String> extraContext, boolean includeGlobal, List<String> possibleNodes, boolean applyGroups);

    /**
     * Processes the nodes and returns the temporary ones.
     * @return a map of temporary nodes
     * @deprecated in favour of {@link #getTemporaryPermissionNodes()}
     */
    @Deprecated
    Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes();

    /**
     * Processes the nodes and returns the temporary ones.
     * @return a set of temporary nodes
     * @since 1.6
     */
    Set<Node> getTemporaryPermissionNodes();

    /**
     * Processes the nodes and returns the non-temporary ones.
     * @return a map of permanent nodes
     * @deprecated in favour of {@link #getPermanentPermissionNodes()}
     */
    @Deprecated
    Map<String, Boolean> getPermanentNodes();

    /**
     * Processes the nodes and returns the non-temporary ones.
     * @return a set of permanent nodes
     * @since 1.6
     */
    Set<Node> getPermanentPermissionNodes();

    /**
     * Removes temporary permissions that have expired
     */
    void auditTemporaryPermissions();

}
