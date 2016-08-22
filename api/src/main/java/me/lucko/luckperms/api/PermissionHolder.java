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

/**
 * Wrapper interface for internal PermissionHolder (user/group) instances
 */
@SuppressWarnings("unused")
public interface PermissionHolder {

    /**
     * @return the identifier for this object. either a uuid string or name
     * However, you should really use {@link User#getUuid()}, {@link User#getName()} or {@link Group#getName()}
     */
    String getObjectName();

    /**
     * Gets an immutable Map of the objects permission nodes
     * @return an immutable map of permissions
     */
    Map<String, Boolean> getNodes();

    /**
     * Checks to see if the object has a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean hasPermission(String node, boolean b);

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the user has the permission
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
     * @return true if the user has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     */
    boolean hasPermission(String node, boolean b, String server, String world);

    /**
     * Checks to see the the object has a permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the user has the permission
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
     * @return true if the user has the permission
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
     * @return true if the user has the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalud
     */
    boolean hasPermission(String node, boolean b, String server, String world, boolean temporary);

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user inherits the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    boolean inheritsPermission(String node, boolean b);

    /**
     * Checks to see the the object inherits a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the user inherits the permission
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
     * @return true if the user inherits the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node server or world is invalid
     */
    boolean inheritsPermission(String node, boolean b, String server, String world);

    /**
     * Checks to see if the object inherits a permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the user inherits the permission
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
     * @return true if the user inherits the permission
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
     * @return true if the user inherits the permission
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world if invalid
     */
    boolean inheritsPermission(String node, boolean b, String server, String world, boolean temporary);

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    void setPermission(String node, boolean value) throws ObjectAlreadyHasException;

    /**
     * Sets a permission for the object on a specific server
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
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
     */
    void setPermission(String node, boolean value, String server, String world) throws ObjectAlreadyHasException;

    /**
     * Sets a temporary permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid or if the expiry time is in the past
     */
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
     */
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
     */
    void setPermission(String node, boolean value, String server, String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param temporary if the permission being removed is temporary
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    void unsetPermission(String node, boolean temporary) throws ObjectLacksException;

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node is null
     * @throws IllegalArgumentException if the node is invalid
     */
    void unsetPermission(String node) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
    void unsetPermission(String node, String server) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server and world
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param world The world to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node, server or world is null
     * @throws IllegalArgumentException if the node, server or world is invalid
     */
    void unsetPermission(String node, String server, String world) throws ObjectLacksException;

    /**
     * Unsets a permission for the object on a specific server
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     * @throws NullPointerException if the node or server is null
     * @throws IllegalArgumentException if the node or server is invalid
     */
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
     */
    void unsetPermission(String node, String server, String world, boolean temporary) throws ObjectLacksException;

    /**
     * Gets the permissions and inherited permissions that apply to a specific server and world
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     */
    Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups, List<String> possibleNodes);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server and world
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     */
    Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups, List<String> possibleNodes);

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups);

    /**
     * Processes the nodes and returns the temporary ones.
     * @return a map of temporary nodes
     */
    Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes();

    /**
     * Processes the nodes and returns the non-temporary ones.
     * @return a map of permanent nodes
     */
    Map<String, Boolean> getPermanentNodes();

    /**
     * Removes temporary permissions that have expired
     */
    void auditTemporaryPermissions();

}
