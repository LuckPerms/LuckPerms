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

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A player which holds permission data.
 */
public interface User extends PermissionHolder {

    /**
     * Gets the users unique ID
     *
     * @return the users Mojang assigned unique id
     */
    @Nonnull
    UUID getUuid();

    /**
     * Gets the users username
     *
     * <p>Returns null if no username is associated with this user.</p>
     *
     * @return the users username
     */
    @Nullable
    String getName();

    /**
     * Gets the users current primary group.
     *
     * <p>The result of this method depends on which method is configured for primary group calculation. It may not
     * be the same as any value set through {@link #setPrimaryGroup(String)}.</p>
     *
     * @return the users primary group
     */
    @Nonnull
    String getPrimaryGroup();

    /**
     * Sets a users primary group. This will only take effect if platform is using stored primary groups.
     *
     * @param group the new primary group
     * @throws ObjectAlreadyHasException if the user already has this set as their primary group
     * @throws IllegalStateException     if the user is not a member of that group
     * @throws NullPointerException      if the group is null
     */
    void setPrimaryGroup(@Nonnull String group) throws ObjectAlreadyHasException;

    /**
     * Refresh and re-assign the users permissions.
     *
     * <p>This request is not buffered, and the refresh call will be ran directly. This should be called on an
     * asynchronous thread.</p>
     */
    void refreshPermissions();

    /**
     * Gets the user's {@link UserData} cache.
     *
     * @return the users cached data.
     * @since 3.2
     */
    @Nonnull
    UserData getCachedData();

    /**
     * Pre-calculates some values in the user's data cache.
     *
     * <p>Is it <b>not</b> necessary to call this method before
     * using {@link #getCachedData()}.</p>
     *
     * @since 2.17
     */
    void setupDataCache();

    /**
     * Check to see if the user is a direct member of a group
     *
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     * @throws NullPointerException if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    boolean isInGroup(@Nonnull Group group);

    /**
     * Check to see if the user is a direct member of a group in a specific context
     *
     * @param group the group to check membership of
     * @param contextSet the context set to filter by
     * @return true if the user is a member of the group
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 3.2
     */
    boolean isInGroup(@Nonnull Group group, @Nonnull ContextSet contextSet);

    /**
     * Gets the user's {@link UserData} cache, if they have one setup.
     *
     * @return an optional, possibly containing the user's cached lookup data.
     * @since 2.13
     * @deprecated in version 3.2, as this cache is now always loaded
     */
    @Deprecated
    @Nonnull
    Optional<UserData> getUserDataCache();

    /**
     * Check to see if a user is a member of a group on a specific server
     *
     * @param group  The group to check membership of
     * @param server The server to check on
     * @return true if the user is a member of the group
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalArgumentException if the server is invalid
     * @deprecated in favour of {@link #isInGroup(Group, ContextSet)}
     */
    @Deprecated
    boolean isInGroup(@Nonnull Group group, @Nonnull String server);

    /**
     * Check to see if a user is a member of a group on a specific server and world
     *
     * @param group  The group to check membership of
     * @param server The server to check on
     * @param world  The world to check on
     * @return true if the user is a member of the group
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalArgumentException if the server or world is invalid
     * @deprecated in favour of {@link #isInGroup(Group, ContextSet)}
     */
    @Deprecated
    boolean isInGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world);

    /**
     * Add a user to a group
     *
     * @param group The group to add the user to
     * @throws ObjectAlreadyHasException if the user is already a member of the group
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server
     *
     * @param group  The group to add the user to
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException      if the group or server is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group, @Nonnull String server) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group on a specific server and world
     *
     * @param group  The group to add the user to
     * @param server The server to add the group on
     * @param world  The world to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException      if the group, server or world is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the server or world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server
     *
     * @param group    The group to add the user to
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server
     *
     * @param group    The group to add the user to
     * @param server   The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException      if the group or server is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past or the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group, @Nonnull String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Add a user to a group temporarily on a specific server and world
     *
     * @param group    The group to add the user to
     * @param server   The server to add the group on
     * @param world    The world to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     * @throws NullPointerException      if the group, server or world is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past or the server/world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void addGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Remove the user from a group
     *
     * @param group the group to remove the user from
     * @throws ObjectLacksException  if the user isn't a member of the group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group) throws ObjectLacksException;

    /**
     * Remove the user from a group
     *
     * @param group     the group to remove the user from
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException  if the user isn't a member of the group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server
     *
     * @param group  The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksException     if the user isn't a member of the group
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group, @Nonnull String server) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server and world
     *
     * @param group  The group to remove the user from
     * @param server The server to remove the group on
     * @param world  The world to remove the group on
     * @throws ObjectLacksException     if the user isn't a member of the group
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server or world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server
     *
     * @param group     The group to remove the user from
     * @param server    The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException     if the user isn't a member of the group
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group, @Nonnull String server, boolean temporary) throws ObjectLacksException;

    /**
     * Remove the user from a group on a specific server and world
     *
     * @param group     The group to remove the user from
     * @param server    The server to remove the group on
     * @param world     The world to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException     if the user isn't a member of the group
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server/world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void removeGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world, boolean temporary) throws ObjectLacksException;

    /**
     * Get a {@link List} of all of the groups the user is a member of, on all servers
     *
     * @return a {@link List} of group names
     * @deprecated in favour of just querying a users permissions
     */
    @Deprecated
    @Nonnull
    List<String> getGroupNames();

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     *
     * @param server the server to check
     * @param world  the world to check
     * @return a {@link List} of group names
     * @throws NullPointerException     if the server or world is null
     * @throws IllegalArgumentException if the server or world is invalid
     * @deprecated in favour of just querying a users permissions
     */
    @Deprecated
    @Nonnull
    List<String> getLocalGroups(@Nonnull String server, @Nonnull String world);

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     *
     * @param server the server to check
     * @return a {@link List} of group names
     * @throws NullPointerException     if the server is null
     * @throws IllegalArgumentException if the server is invalid
     * @deprecated in favour of just querying a users permissions
     */
    @Deprecated
    @Nonnull
    List<String> getLocalGroups(@Nonnull String server);

}
