package me.lucko.luckperms.users;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;
import me.lucko.luckperms.utils.PermissionObject;

import java.util.*;
import java.util.stream.Collectors;

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
        super(plugin, uuid.toString());
        this.uuid = uuid;
        this.name = null;
    }

    User(UUID uuid, String name, LuckPermsPlugin plugin) {
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
        return hasPermission("group." + group.getName(), true, server);
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
     * Remove the user from a group
     * @param group the group to remove the user from
     * @throws ObjectLacksException if the user isn't a member of the group
     */
    public void removeGroup(Group group) throws ObjectLacksException {
        removeGroup(group, "global");
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
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server) {
        return getGroups(server, null, false);
    }

    /**
     * Get a {@link List} of the groups the user is a member of on a specific server with the option to include global groups or all groups
     * @param server Which server to check on
     * @param excludedGroups groups to exclude (prevents circular inheritance issues)
     * @param includeGlobal Whether to include global groups
     * @return a {@link List} of group names
     */
    private List<String> getGroups(String server, List<String> excludedGroups, boolean includeGlobal) {
        if (excludedGroups == null) {
            excludedGroups = new ArrayList<>();
        }

        excludedGroups.add(getObjectName());
        List<String> groups = new ArrayList<>();

        if (server == null || server.equals("")) {
            server = "global";
        }

        /*
        Priority:

        1. server specific group nodes
        2. group nodes
        */

        final Map<String, Boolean> serverSpecificGroups = new HashMap<>();
        final Map<String, Boolean> groupNodes = new HashMap<>();

        // Sorts the permissions and puts them into a priority order
        for (Map.Entry<String, Boolean> node : getNodes().entrySet()) {
            serverSpecific:
            if (node.getKey().contains("/")) {
                String[] parts = node.getKey().split("\\/", 2);

                if (parts[0].equalsIgnoreCase("global")) {
                    // REGULAR
                    break serverSpecific;
                }

                if (!parts[0].equalsIgnoreCase(server)) {
                    // SERVER SPECIFIC BUT DOES NOT APPLY
                    continue;
                }

                if (Patterns.GROUP_MATCH.matcher(parts[1]).matches()) {
                    // SERVER SPECIFIC AND GROUP
                    serverSpecificGroups.put(node.getKey(), node.getValue());
                    continue;
                }

                continue;
            }

            // Skip adding global permissions if they are not requested
            if (!includeGlobal) continue;

            if (Patterns.GROUP_MATCH.matcher(node.getKey()).matches()) {
                // GROUP
                groupNodes.put(node.getKey(), node.getValue());
            }

        }

        // If a group is negated at a higher priority, the group should not then be applied at a lower priority
        serverSpecificGroups.entrySet().stream().filter(node -> !node.getValue()).forEach(node -> {
            groupNodes.remove(node.getKey());
            groupNodes.remove(Patterns.SERVER_SPLIT.split(node.getKey(), 2)[1]);
        });

        groups.addAll(serverSpecificGroups.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> Patterns.DOT_SPLIT.split(e.getKey(), 2)[1])
                .collect(Collectors.toList())
        );
        groups.addAll(groupNodes.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> Patterns.DOT_SPLIT.split(e.getKey(), 2)[1])
                .collect(Collectors.toList())
        );
        return groups;
    }

    @Override
    public String toString() {
        return getUuid().toString();
    }
}
