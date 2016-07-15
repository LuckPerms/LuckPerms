package me.lucko.luckperms.utils;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an object that can hold permissions
 * For example a User or a Group
 */
@Getter
public abstract class PermissionObject {

    /**
     * The UUID of the user / name of the group.
     * Used to prevent circular inheritance issues
     */
    private final String objectName;

    /**
     * Reference to the main plugin instance
     */
    private final LuckPermsPlugin plugin;

    /**
     * If false, only permissions specific to the server are applied
     */
    @Setter
    private boolean includeGlobalPermissions;

    /**
     * The user/group's permissions
     */
    @Setter
    private Map<String, Boolean> nodes = new HashMap<>();

    protected PermissionObject(LuckPermsPlugin plugin, String objectName) {
        this.objectName = objectName;
        this.plugin = plugin;
        this.includeGlobalPermissions = plugin.getConfiguration().getIncludeGlobalPerms();
    }

    /**
     * Checks to see if the object has a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, Boolean b) {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        return b ? getNodes().containsKey(node) && getNodes().get(node) : getNodes().containsKey(node) && !getNodes().get(node);
    }

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, Boolean b, String server) {
        return hasPermission(server + "/" + node, b);
    }

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, Boolean b) {
        if (node.contains("/")) {
            // Use other method
            final String[] parts = Patterns.SERVER_SPLIT.split(node, 2);
            return inheritsPermission(parts[1], b, parts[0]);
        }

        return inheritsPermission(node, b, "global");
    }

    /**
     * Checks to see the the object inherits a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, Boolean b, String server) {
        final Map<String, Boolean> local = getLocalPermissions(server, null);
        return b ? local.containsKey(node) && local.get(node) : local.containsKey(node) && !local.get(node);
    }

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, Boolean value) throws ObjectAlreadyHasException {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        if (hasPermission(node, value)) {
            throw new ObjectAlreadyHasException();
        }
        getNodes().put(node, value);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, Boolean value, String server) throws ObjectAlreadyHasException {
        setPermission(server + "/" + node, value);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksPermissionException if the node wasn't already set
     */
    public void unsetPermission(String node) throws ObjectLacksPermissionException {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        if (!getNodes().containsKey(node)) {
            throw new ObjectLacksPermissionException();
        }
        getNodes().remove(node);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksPermissionException if the node wasn't already set
     */
    public void unsetPermission(String node, String server) throws ObjectLacksPermissionException {
        unsetPermission(server + "/" + node);
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return getPermissions(server, excludedGroups, includeGlobalPermissions);
    }

    private Map<String, Boolean> getPermissions(String server, List<String> excludedGroups, boolean includeGlobal) {
        if (excludedGroups == null) {
            excludedGroups = new ArrayList<>();
        }

        excludedGroups.add(getObjectName());
        Map<String, Boolean> perms = new HashMap<>();

        if (server == null || server.equals("")) {
            server = "global";
        }

        /*
        Priority:

        1. server specific nodes
        2. user nodes
        3. server specific group nodes
        4. group nodes
        */

        final Map<String, Boolean> serverSpecificNodes = new HashMap<>();
        final Map<String, Boolean> userNodes = new HashMap<>();
        final Map<String, Boolean> serverSpecificGroups = new HashMap<>();
        final Map<String, Boolean> groupNodes = new HashMap<>();

        // Sorts the permissions and puts them into a priority order
        for (Map.Entry<String, Boolean> node : getNodes().entrySet()) {
            serverSpecific:
            if (node.getKey().contains("/")) {
                String[] parts = Patterns.SERVER_SPLIT.split(node.getKey(), 2);

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

                // SERVER SPECIFIC
                serverSpecificNodes.put(node.getKey(), node.getValue());
                continue;
            }

            // Skip adding global permissions if they are not requested
            if (!includeGlobal) continue;

            if (Patterns.GROUP_MATCH.matcher(node.getKey()).matches()) {
                // GROUP
                groupNodes.put(node.getKey(), node.getValue());
                continue;
            }

            // JUST NORMAL
            userNodes.put(node.getKey(), node.getValue());
        }

        // If a group is negated at a higher priority, the group should not then be applied at a lower priority
        serverSpecificGroups.entrySet().stream().filter(node -> !node.getValue()).forEach(node -> {
            groupNodes.remove(node.getKey());
            groupNodes.remove(Patterns.SERVER_SPLIT.split(node.getKey(), 2)[1]);
        });

        // Apply lowest priority: groupNodes
        for (Map.Entry<String, Boolean> groupNode : groupNodes.entrySet()) {
            // Add the actual group perm node, so other plugins can hook
            perms.put(groupNode.getKey(), groupNode.getValue());


            // Don't add negated groups
            if (!groupNode.getValue()) continue;

            String groupName = Patterns.DOT_SPLIT.split(groupNode.getKey(), 2)[1];
            if (!excludedGroups.contains(groupName)) {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group != null) {
                    perms.putAll(group.getLocalPermissions(server, excludedGroups));
                } else {
                    plugin.getLogger().warning("Error whilst refreshing the permissions of '" + objectName + "'." +
                            "\n The group '" + groupName + "' is not loaded.");
                }
            }
        }

        // Apply next priority: serverSpecificGroups
        for (Map.Entry<String, Boolean> groupNode : serverSpecificGroups.entrySet()) {
            final String rawNode = Patterns.SERVER_SPLIT.split(groupNode.getKey())[1];

            // Add the actual group perm node, so other plugins can hook
            perms.put(rawNode, groupNode.getValue());

            // Don't add negated groups
            if (!groupNode.getValue()) continue;

            String groupName = Patterns.DOT_SPLIT.split(rawNode, 2)[1];
            if (!excludedGroups.contains(groupName)) {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group != null) {
                    perms.putAll(group.getLocalPermissions(server, excludedGroups));
                } else {
                    plugin.getLogger().warning("Error whilst refreshing the permissions of '" + objectName + "'." +
                            "\n The group '" + groupName + "' is not loaded.");
                }
            }
        }

        // Apply next priority: userNodes
        perms.putAll(userNodes);

        // Apply highest priority: serverSpecificNodes
        for (Map.Entry<String, Boolean> node : serverSpecificNodes.entrySet()) {
            final String rawNode = Patterns.SERVER_SPLIT.split(node.getKey())[1];
            perms.put(rawNode, node.getValue());
        }

        return perms;
    }
}
