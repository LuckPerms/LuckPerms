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

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for internal User instances
 */
@SuppressWarnings("unused")
public interface User extends PermissionHolder {

    /**
     * @return the users Mojang assigned unique id
     */
    UUID getUuid();

    /**
     * @return the Users Username
     */
    String getName();

    /**
     * Gets the users primary group
     * @return the users primary group
     */
    String getPrimaryGroup();

    /**
     * Sets a users primary group
     * @param group the new primary group
     * @throws ObjectAlreadyHasException if the user already has this set as their primary group
     * @throws IllegalStateException if the user is not a member of that group
     * @throws NullPointerException if the group is null
     */
    void setPrimaryGroup(String group) throws ObjectAlreadyHasException;

    /**
     * Refresh and re-assign the users permissions
     */
    void refreshPermissions();

    /**
     * Gets the user's {@link UserData} cache, if they have one setup.
     * @return an optional, possibly containing the user's cached lookup data.
     * @since 2.13
     */
    Optional<UserData> getUserDataCache();

    /**
     * Check to see if the user is a member of a group
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     * @throws NullPointerException if the group is null
     */
    boolean isInGroup(Group group);

    /**
     * Check to see if a user is a member of a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @return true if the user is a member of the group
     * @throws NullPointerException if the group or server is null
     * @throws IllegalArgumentException if the server is invalid
     */
    boolean isInGroup(Group group, String server);

    /**
     * Check to see if a user is a member of a group on a specific server and world
     * @param group The group to check membership of
     * @param server The server to check on
     * @param world The world to check on
     * @return true if the user is a member of the group
     * @throws NullPointerException if the group, server or world is null
     * @throws IllegalArgumentException if the server or world is invalid
     */
    boolean isInGroup(Group group, String server, String world);

    /**
     * Add a user to a group
     * @param group The group to add the user to
     * @throws ObjectAlreadyHasException if the user is already a member of the group
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void addGroup(Group group) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException if the group or server is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server is invalid
     */
    void addGroup(Group group, String server) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server and world
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException if the group, server or world is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server or world is invalid
     */
    void addGroup(Group group, String server, String world) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server
     * @param group The group to add the user to
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past
     */
    void addGroup(Group group, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException if the group or server is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server is invalid
     */
    void addGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server and world
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException if the group, server or world is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server/world is invalid
     */
    void addGroup(Group group, String server, String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void removeGroup(Group group) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    void removeGroup(Group group, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group or server is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server is invalid
     */
    void removeGroup(Group group, String server) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server and world
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group, server or world is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server or world is invalid
     */
    void removeGroup(Group group, String server, String world) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group or server is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server is invalid
     */
    void removeGroup(Group group, String server, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server and world
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     * @throws NullPointerException if the group, server or world is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server/world is invalid
     */
    void removeGroup(Group group, String server, String world, boolean temporary) throws ObjectLacksException;

    /**
     * Clear all of the users permission nodes
     */
    void clearNodes();

    /**
     * Get a {@link List} of all of the groups the user is a member of, on all servers
     * @return a {@link List} of group names
     */
    List<String> getGroupNames();

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @param world the world to check
     * @return a {@link List} of group names
     * @throws NullPointerException if the server or world is null
     * @throws IllegalArgumentException if the server or world is invalid
     */
    List<String> getLocalGroups(String server, String world);

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     * @throws NullPointerException if the server is null
     * @throws IllegalArgumentException if the server is invalid
     */
    List<String> getLocalGroups(String server);

}
