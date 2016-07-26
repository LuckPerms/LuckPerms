package me.lucko.luckperms.groups;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.utils.Patterns;
import me.lucko.luckperms.utils.PermissionObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Group extends PermissionObject {

    /**
     * The name of the group
     */
    @Getter
    private final String name;

    Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name;
    }

    /**
     * check to see if a group inherits a group
     * @param group The group to check membership of
     * @return true if the user is a member of the group
     */
    public boolean inheritsGroup(Group group) {
        return inheritsGroup(group, "global");
    }

    /**
     * check to see if the group inherits a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @return true if the group inherits the group
     */
    public boolean inheritsGroup(Group group, String server) {
        return hasPermission("group." + group.getName(), true, server);
    }

    /**
     * check to see if the group inherits a group on a specific server
     * @param group The group to check membership of
     * @param server The server to check on
     * @param world The world to check on
     * @return true if the group inherits the group
     */
    public boolean inheritsGroup(Group group, String server, String world) {
        return hasPermission("group." + group.getName(), true, server, world);
    }

    /**
     * Make this group inherit another group
     * @param group the group to be inherited
     * @throws ObjectAlreadyHasException if the group already inherits the group
     */
    public void setInheritGroup(Group group) throws ObjectAlreadyHasException {
        setInheritGroup(group, "global");
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server);
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server, String world) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world);
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, long expireAt) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, expireAt);
    }

    /**
     * Make this group inherit another group on a specific server
     * @param group the group to be inherited
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the group already inherits the group on that server
     */
    public void setInheritGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, expireAt);
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
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world, expireAt);
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
        Map<String, Boolean> perms = getPermissions(server, world, null, includeGlobal);
        return perms.keySet().stream()
                .filter(s -> Patterns.GROUP_MATCH.matcher(s).matches())
                .map(s -> Patterns.DOT_SPLIT.split(s, 2)[1])
                .collect(Collectors.toList());
    }
}
