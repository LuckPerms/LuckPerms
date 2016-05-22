package me.lucko.luckperms.users;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.PermissionObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public User(UUID uuid, LuckPermsPlugin plugin) {
        super(plugin, uuid.toString());
        this.uuid = uuid;
        this.name = null;
    }

    public User(UUID uuid, String name, LuckPermsPlugin plugin) {
        super(plugin, uuid.toString());
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
        return getLocalGroups(server).contains(group.getName());
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

        if (isInGroup(group, server)) {
            throw new ObjectAlreadyHasException();
        }

        if (server.equalsIgnoreCase("global")) {
            getNodes().put("luckperms.group." + group.getName(), true);
        } else {
            getNodes().put(server + "/luckperms.group." + group.getName(), true);
        }
    }

    /**
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksPermissionException
     */
    public void removeGroup(Group group) throws ObjectLacksPermissionException {
        removeGroup(group, "global");
    }

    /**
     * Remove the user from a group
     * @param group The group to remove the user from
     * @param server The server to remove the group on
     * @throws ObjectLacksPermissionException if the user isn't a member of the group
     */
    public void removeGroup(Group group, String server) throws ObjectLacksPermissionException {
        if (server == null) {
            server = "global";
        }

        if (!getLocalGroups(server).contains(group.getName())) {
            throw new ObjectLacksPermissionException();
        }

        if (server.equalsIgnoreCase("global")) {
            getNodes().remove("luckperms.group." + group.getName());
        } else {
            getNodes().remove(server + "/luckperms.group." + group.getName());
        }
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
        return getGroups(null, true, true);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server) {
        return getGroups(server, false, false);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server with the option to include global groups or all groups
     * @param server Which server to check on
     * @param includeGlobal Whether to include global groups
     * @param includeAll Whether to get all groups
     * @return a {@link List} of group names
     */
    public List<String> getGroups(String server, boolean includeGlobal, boolean includeAll) {
        List<String> groups = new ArrayList<>();

        if (server == null || server.equals("")) {
            server = "global";
        }

        for (String node : getNodes().keySet()) {
            String originalNode = node;
            // Has a defined server
            if (node.contains("/")) {
                String[] parts = node.split("\\/", 2);
                if (!parts[0].equalsIgnoreCase(server) && !includeAll) {
                    continue;
                }
                node = parts[1];
            } else {
                if (!includeGlobal) {
                    continue;
                }
            }

            if (node.matches("luckperms\\.group\\..*")) {
                if (getNodes().get(originalNode)) {
                    String groupName = node.split("\\.", 3)[2];
                    groups.add(groupName);
                }
            }
        }
        return groups;
    }

    @Override
    public String toString() {
        return getUuid().toString();
    }
}
