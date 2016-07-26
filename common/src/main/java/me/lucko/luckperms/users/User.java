package me.lucko.luckperms.users;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;
import me.lucko.luckperms.utils.PermissionObject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ToString(of = {"uuid"})
@EqualsAndHashCode(of = {"uuid"}, callSuper = false)
public abstract class User extends PermissionObject {

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

    User(UUID uuid, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = null;
    }

    User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(uuid.toString(), plugin);
        this.uuid = uuid;
        this.name = name;
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
        return isInGroup(group, "global");
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
        addGroup(group, "global");
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server);
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param world The world to add the group on
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server, String world) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world);
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, long expireAt) throws ObjectAlreadyHasException {
        setPermission("group." + group.getName(), true, expireAt);
    }

    /**
     * Add a user to a group on a specific server
     * @param group The group to add the user to
     * @param server The server to add the group on
     * @param expireAt when the group should expire
     * @throws ObjectAlreadyHasException if the user is already a member of the group on that server
     */
    public void addGroup(Group group, String server, long expireAt) throws ObjectAlreadyHasException {
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, expireAt);
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
        if (server == null) {
            server = "global";
        }

        setPermission("group." + group.getName(), true, server, world, expireAt);
    }

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group) throws ObjectLacksException {
        removeGroup(group, "global");
    }

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @param temporary if the group being removed is temporary
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, boolean temporary) throws ObjectLacksException {
        removeGroup(group, "global", temporary);
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server) throws ObjectLacksException {
        if (server == null) {
            server = "global";
        }

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
        if (server == null) {
            server = "global";
        }

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
        if (server == null) {
            server = "global";
        }

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
        if (server == null) {
            server = "global";
        }

        unsetPermission("group." + group.getName(), server, world, temporary);
    }

    /**
     * Clear all of the users permission nodes
     */
    public void clearNodes() {
        String defaultGroupNode = getPlugin().getConfiguration().getDefaultGroupNode();
        getNodes().clear();
        getNodes().put(defaultGroupNode, true);
    }

    /**
     * Get a {@link List} of all of the groups the user is a member of, on all servers
     * @return a {@link List} of group names
     */
    public List<String> getGroupNames() {
        return getGroups(null, null, true);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @param world the world to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server, String world) {
        return getGroups(server, world, false);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server) {
        return getLocalGroups(server, null);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server with the option to include global
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
