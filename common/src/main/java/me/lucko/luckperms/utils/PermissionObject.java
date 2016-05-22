package me.lucko.luckperms.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.NonNull;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public PermissionObject(LuckPermsPlugin plugin, String objectName) {
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
        if (b) {
            return getNodes().containsKey(node) && getNodes().get(node);
        }
        return getNodes().containsKey(node) && !getNodes().get(node);
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

        for (String node : getNodes().keySet()) {
            String originalNode = node;
            // Has a defined server
            if (node.contains("/")) {
                String[] parts = node.split("\\/", 2);
                if (!parts[0].equalsIgnoreCase(server)) {
                    continue;
                }
                node = parts[1];

                perms.put(node, getNodes().get(originalNode));
                continue;
            }

            if (node.matches("luckperms\\.group\\..*")) {
                if (getNodes().get(originalNode)) {
                    String groupName = node.split("\\.", 3)[2];
                    Group group = plugin.getGroupManager().getGroup(groupName);

                    if (!excludedGroups.contains(groupName)) {
                        if (group != null) {
                            perms.putAll(group.getLocalPermissions(server, excludedGroups));
                        } else {
                            plugin.getLogger().warning("Error whilst refreshing the permissions of '" + objectName + "'." +
                            "\n The group '" + groupName + "' is not loaded.");
                        }
                    }
                }

                perms.put(node, getNodes().get(originalNode));
                continue;
            }

            if (includeGlobal) perms.put(node, getNodes().get(originalNode));
        }
        return perms;
    }

    /**
     * Loads a list of semi-serialised nodes into the object
     * @param data The data to be loaded
     */
    public void loadNodes(List<String> data) {
        // String is the node in format "server/plugin.command-false" or "plugin.command-false" or "server/plugin.command"
        // or just "plugin.command"

        for (String s : data) {
            String[] parts = s.split("-", 2);

            if (parts.length == 2) {
                nodes.put(parts[0], Boolean.valueOf(parts[1]));
            } else {
                nodes.put(parts[0], true);
            }
        }
    }

    /**
     * Convert the permission nodes map to a list of strings
     * @return a {@link List} of nodes
     */
    public List<String> getNodesAsString() {
        List<String> data = new ArrayList<>();

        for (String node : nodes.keySet()) {
            if (nodes.get(node)) {
                data.add(node);
            } else {
                data.add(node + "-false");
            }
        }

        return data;
    }

    /**
     * Serialize the nodes in the object to be saved in the datastore
     * @return A serialized string
     */
    public String serializeNodes() {
        if (nodes.isEmpty()) return "#";
        return getNodesAsString().stream().collect(Collectors.joining(":"));
    }
}
