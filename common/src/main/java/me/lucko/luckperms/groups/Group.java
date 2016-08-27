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

package me.lucko.luckperms.groups;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.event.events.GroupAddEvent;
import me.lucko.luckperms.api.implementation.internal.GroupLink;
import me.lucko.luckperms.api.implementation.internal.PermissionHolderLink;
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.utils.Identifiable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Group extends PermissionHolder implements Identifiable<String> {

    /**
     * The name of the group
     */
    @Getter
    private final String name;

    Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name;
    }

    @Override
    public String getId() {
        return name;
    }

    /**
     * check to see if a group inherits a group
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     */
    public boolean inheritsGroup(Group group) {
        return group.getName().equalsIgnoreCase(this.getName()) || inheritsGroup(group, "global");
    }

    /**
     * check to see if the group inherits a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @return true if the group inherits the group
     */
    public boolean inheritsGroup(Group group, String server) {
        return group.getName().equalsIgnoreCase(this.getName()) || hasPermission("group." + group.getName(), true, server);
    }

    /**
     * check to see if the group inherits a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @param world The world to check on
     * @return true if the group inherits the group
     */
    public boolean inheritsGroup(Group group, String server, String world) {
        return group.getName().equalsIgnoreCase(this.getName()) || hasPermission("group." + group.getName(), true, server, world);
    }

    /**
     * Make this group inherit another group
     * @param group the group to be inherited
     * @throws ObjectAlreadyHasException if the group already inherits the group
     */
    public void setInheritGroup(Group group) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        setPermission("group." + group.getName(), true);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), null, null, 0L));
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, null, 0L));
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server, String world) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, world, 0L));
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, long expireAt) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        setPermission("group." + group.getName(), true, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), null, null, expireAt));
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, null, expireAt));
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server, String world, long expireAt) throws ObjectAlreadyHasException {
        if (group.getName().equalsIgnoreCase(this.getName())) {
            throw new ObjectAlreadyHasException();
        }

        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world, expireAt);
        getPlugin().getApiProvider().fireEventAsync(new GroupAddEvent(new PermissionHolderLink(this), new GroupLink(group), server, world, expireAt));
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group) throws ObjectLacksException {
        unsetInheritGroup(group, "global");
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group, boolean temporary) throws ObjectLacksException {
        unsetInheritGroup(group, "global", temporary);
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @param server The server to remove the group on
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group, String server) throws ObjectLacksException {
        if (server == null) {
            server = "global";
        }

        unsetPermission("group." + group.getName(), server);
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group, String server, String world) throws ObjectLacksException {
        if (server == null) {
            server = "global";
        }

        unsetPermission("group." + group.getName(), server, world);
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @param server The server to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group, String server, boolean temporary) throws ObjectLacksException {
        if (server == null) {
            server = "global";
        }

        unsetPermission("group." + group.getName(), server, temporary);
    }

    /**
     * Remove a previously set inheritance
     * @param group the group to uninherit
     * @param server The server to remove the group on
     * @param world The world to remove the group on
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the group does not already inherit the group
     */
    public void unsetInheritGroup(Group group, String server, String world, boolean temporary) throws ObjectLacksException {
        if (server == null) {
            server = "global";
        }

        unsetPermission("group." + group.getName(), server, world, temporary);
    }

    /**
     * Clear all of the groups permission nodes
     */
    public void clearNodes() {
        getNodes().clear();
    }

    /**
     * Get a {@link List} of all of the groups the group inherits, on all servers
     * @return a {@link List} of group names
     */
    public List<String> getGroupNames() {
        return getGroups(null, null, true);
    }

    /**
     * Get a {@link List} of the groups the group inherits on a specific server
     * @param server the server to check
     * @param world the world to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server, String world) {
        return getGroups(server, world, false);
    }

    /**
     * Get a {@link List} of the groups the group inherits on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server) {
        return getLocalGroups(server, null);
    }

    /**
     * Get a {@link List} of the groups the group inherits on a specific server with the option to include global
     * groups or all groups
     * @param server Which server to check on
     * @param world Which world to check on
     * @param includeGlobal Whether to include global groups
     * @return a {@link List} of group names
     */
    private List<String> getGroups(String server, String world, boolean includeGlobal) {
        // Call super #getPermissions method, and just sort through those
        Map<String, Boolean> perms = exportNodes(server, world, null, includeGlobal, true, null);
        return perms.keySet().stream()
                .filter(s -> Patterns.GROUP_MATCH.matcher(s).matches())
                .map(s -> Patterns.DOT.split(s, 2)[1])
                .collect(Collectors.toList());
    }
}
