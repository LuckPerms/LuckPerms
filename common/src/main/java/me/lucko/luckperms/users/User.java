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

package me.lucko.luckperms.users;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.event.events.GroupAddEvent;
import me.lucko.luckperms.api.implementation.internal.GroupLink;
import me.lucko.luckperms.api.implementation.internal.PermissionHolderLink;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Identifiable;

import java.util.UUID;

@ToString(of = {"uuid"})
@EqualsAndHashCode(of = {"uuid"}, callSuper = false)
public abstract class User extends PermissionHolder implements Identifiable<UserIdentifier> {

    /**
     * The users Mojang UUID
     */
    @Getter
    private final UUID uuid;

    /**
     * The last known username of a player
     */
    @Getter
    @Setter
    private String name;

    /**
     * The users primary group
     */
    @Getter
    @Setter
    private String primaryGroup = null;

    public User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = null;
    }

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UserIdentifier getId() {
        return UserIdentifier.of(uuid, name);
    }

    /**
     * Refresh and re-assign the users permissions
     */
    public abstract void refreshPermissions();

    /**
     * Check to see if the user is a member of a group
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     */
    public boolean isInGroup(Group group) {
        return hasPermission("group." + group.getName(), true);
    }

    /**
     * Check to see if a user is a member of a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @return true if the user is a member of the group
     */
    public boolean isInGroup(Group group, String server) {
        return hasPermission("group." + group.getName(), true, server);
    }

    /**
     * Check to see if a user is a member of a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @param world The world to check on
     * @return true if the user is a member of the group
     */
    public boolean isInGroup(Group group, String server, String world) {
        return hasPermission("group." + group.getName(), true, server, world);
    }

    /**
     * Add a user to a group
     * @param group The group to add the user to
     * @throws ObjectAlreadyHasException if the user is already a member of the group
     */
    public void addGroup(Group group) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), null, null, 0L));
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, server);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, null, 0L));
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server, String world) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, server, world);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, world, 0L));
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, long expireAt) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), null, null, expireAt));
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, server, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, null, expireAt));
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server, String world, long expireAt) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, server, world, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, world, expireAt));
    }

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group) throws ObjectLacksException {
        unsetPermission("group." + group.getName());
    }

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, boolean temporary) throws ObjectLacksException {
        unsetPermission("group." + group.getName(), temporary);
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server) throws ObjectLacksException {
        unsetPermission("group." + group.getName(), server);
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server, String world) throws ObjectLacksException {
        unsetPermission("group." + group.getName(), server, world);
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server, boolean temporary) throws ObjectLacksException {
        unsetPermission("group." + group.getName(), server, temporary);
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server, String world, boolean temporary) throws ObjectLacksException {
        unsetPermission("group." + group.getName(), server, world, temporary);
    }

    /**
     * Clear all of the users permission nodes
     */
    public void clearNodes() {
        getNodes().clear();
        getPlugin().getUserManager().giveDefaultIfNeeded(this, false);
    }
}
