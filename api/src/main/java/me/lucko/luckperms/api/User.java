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
import java.util.UUID;

/**
 * Wrapper interface for internal User instances
 *
 * <p> The implementations of this interface limit access to the User and add parameter checks to further prevent
 * errors and ensure all API interactions to not damage the state of the user.
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
     * @param s the new primary group
     * @throws ObjectAlreadyHasException if the user already has this set as their primary group
     */
    void setPrimaryGroup(String s) throws ObjectAlreadyHasException;

    /**
     * Refresh and re-assign the users permissions
     */
    void refreshPermissions();

    /**
     * Check to see if the user is a member of a group
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     */
    boolean isInGroup(Group group);

    /**
     * Check to see if a user is a member of a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @return true if the user is a member of the group
     */
    boolean isInGroup(Group group, String server);

    /**
     * Check to see if a user is a member of a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @param world The world to check on
     * @return true if the user is a member of the group
     */
    boolean isInGroup(Group group, String server, String world);

    /**
     * Add a user to a group
     * @param group The group to add the user to
     * @throws ObjectAlreadyHasException if the user is already a member of the group
     */
    void addGroup(Group group) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    void addGroup(Group group, String server) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    void addGroup(Group group, String server, String world) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    void addGroup(Group group, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    void addGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    void addGroup(Group group, String server, String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    void removeGroup(Group group) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    void removeGroup(Group group, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    void removeGroup(Group group, String server) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    void removeGroup(Group group, String server, String world) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    void removeGroup(Group group, String server, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
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
     */
    List<String> getLocalGroups(String server, String world);

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     */
    List<String> getLocalGroups(String server);

}
