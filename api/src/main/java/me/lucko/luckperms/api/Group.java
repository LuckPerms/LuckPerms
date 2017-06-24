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

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.OptionalInt;

import javax.annotation.Nonnull;

/**
 * A group which holds permission data.
 */
public interface Group extends PermissionHolder {

    /**
     * Get the name of the group
     *
     * @return the name of the group
     */
    @Nonnull
    String getName();

    /**
     * Check to see if a group inherits another group directly
     *
     * @param group The group to check membership of
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     */
    boolean inheritsGroup(@Nonnull Group group);

    /**
     * Check to see if a group inherits another group directly
     *
     * @param group The group to check membership of
     * @param contextSet the context set to filter by
     * @return true if the group inherits the other group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @since 3.2
     */
    boolean inheritsGroup(@Nonnull Group group, @Nonnull ContextSet contextSet);

    /**
     * Gets the weight of this group, if present.
     *
     * @return the group weight
     * @since 2.17
     */
    @Nonnull
    OptionalInt getWeight();

    /**
     * Check to see if the group inherits a group on a specific server
     *
     * @param group  The group to check membership of
     * @param server The server to check on
     * @return true if the group inherits the group on the server
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server is invalid
     * @deprecated in favour of {@link #inheritsGroup(Group, ContextSet)}
     */
    @Deprecated
    boolean inheritsGroup(@Nonnull Group group, @Nonnull String server);

    /**
     * Check to see if the group inherits a group on a specific server and world
     *
     * @param group  The group to check membership of
     * @param server The server to check on
     * @param world  The world to check on
     * @return true if the group inherits the group on the server and world
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server or world is invalid
     * @deprecated in favour of {@link #inheritsGroup(Group, ContextSet)}
     */
    @Deprecated
    boolean inheritsGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world);

    /**
     * Make this group inherit another group
     *
     * @param group the group to be inherited
     * @throws ObjectAlreadyHasException if the group already inherits the group
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group) throws ObjectAlreadyHasException;

    /**
     * Make this group inherit another group on a specific server
     *
     * @param group  the group to be inherited
     * @param server The server to inherit the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     * @throws NullPointerException      if the group or server is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group, @Nonnull String server) throws ObjectAlreadyHasException;

    /**
     * Make this group inherit another group on a specific server and world
     *
     * @param group  the group to be inherited
     * @param server The server to inherit the group on
     * @param world  The world to inherit the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server and world
     * @throws NullPointerException      if the group, server or world is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the server or world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world) throws ObjectAlreadyHasException;

    /**
     * Make this group inherit another group temporarily
     *
     * @param group    the group to be inherited
     * @param expireAt the unix time when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group temporarily
     * @throws NullPointerException      if the group is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Make this group inherit another group on a specific server temporarily
     *
     * @param group    the group to be inherited
     * @param server   The server inherit add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server temporarily
     * @throws NullPointerException      if the group or server is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past or the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group, @Nonnull String server, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Make this group inherit another group on a specific server and world temporarily
     *
     * @param group    the group to be inherited
     * @param server   The server to inherit the group on
     * @param world    The world to inherit the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server and world temporarily
     * @throws NullPointerException      if the group, server or world is null
     * @throws IllegalStateException     if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException  if the expiry time is in the past or the server/world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void setInheritGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world, long expireAt) throws ObjectAlreadyHasException;

    /**
     * Remove a previously set inheritance rule
     *
     * @param group the group to uninherit
     * @throws ObjectLacksException  if the group does not already inherit the group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group) throws ObjectLacksException;

    /**
     * Remove a previously set inheritance rule
     *
     * @param group     the group to uninherit
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException  if the group does not already inherit the group
     * @throws NullPointerException  if the group is null
     * @throws IllegalStateException if the group instance was not obtained from LuckPerms.
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group, boolean temporary) throws ObjectLacksException;

    /**
     * Remove a previously set inheritance rule on a specific server
     *
     * @param group  the group to uninherit
     * @param server The server to remove the group on
     * @throws ObjectLacksException     if the group does not already inherit the group on that server
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group, @Nonnull String server) throws ObjectLacksException;

    /**
     * Remove a previously set inheritance rule on a specific server and world
     *
     * @param group  the group to uninherit
     * @param server The server to remove the group on
     * @param world  The world to remove the group on
     * @throws ObjectLacksException     if the group does not already inherit the group
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the server or world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world) throws ObjectLacksException;

    /**
     * Remove a previously set inheritance rule on a specific server
     *
     * @param group     the group to uninherit
     * @param server    The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException     if the group does not already inherit the group
     * @throws NullPointerException     if the group or server is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group, @Nonnull String server, boolean temporary) throws ObjectLacksException;

    /**
     * Remove a previously set inheritance rule on a specific server and world
     *
     * @param group     the group to uninherit
     * @param server    The server to remove the group on
     * @param world     The world to remove the group on
     * @param temporary if the group being removed was set temporarily
     * @throws ObjectLacksException     if the group does not already inherit the group
     * @throws NullPointerException     if the group, server or world is null
     * @throws IllegalStateException    if the group instance was not obtained from LuckPerms.
     * @throws IllegalArgumentException if the expiry time is in the past or the server/world is invalid
     * @deprecated in favour of {@link NodeFactory#makeGroupNode(Group)}
     */
    @Deprecated
    void unsetInheritGroup(@Nonnull Group group, @Nonnull String server, @Nonnull String world, boolean temporary) throws ObjectLacksException;

    /**
     * Get a {@link List} of all of the groups the group inherits, on all servers
     *
     * @return a {@link List} of group names
     * @deprecated in favour of just querying a users permissions
     */
    @Deprecated
    @Nonnull
    List<String> getGroupNames();

    /**
     * Get a {@link List} of the groups the group inherits on a specific server
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


    /**
     * Get a {@link List} of the groups the group inherits on a specific server and world
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

}
