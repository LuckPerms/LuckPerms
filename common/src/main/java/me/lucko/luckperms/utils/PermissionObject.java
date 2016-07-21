package me.lucko.luckperms.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Represents an object that can hold permissions
 * For example a User or a Group
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PermissionObject {

    /**
     * The UUID of the user / name of the group.
     * Used to prevent circular inheritance issues
     */
    @Getter
    private final String objectName;

    /**
     * Reference to the main plugin instance
     */
    @Getter(AccessLevel.PROTECTED)
    private final LuckPermsPlugin plugin;

    /**
     * The user/group's permissions
     */
    @Getter
    private Map<String, Boolean> nodes = new ConcurrentHashMap<>();

    public void setNodes(Map<String, Boolean> nodes) {
        this.nodes.clear();
        this.nodes.putAll(nodes);
        auditTemporaryPermissions();
    }

    /**
     * Utility method for checking if a map has a certain permission. Used by both #hasPermission and #inheritsPermission
     */
    private static boolean hasPermission(Map<String, Boolean> toQuery, String node, boolean b) {
        // Not temporary
        if (!node.contains("$")) {
            return b ? toQuery.containsKey(node) && toQuery.get(node) : toQuery.containsKey(node) && !toQuery.get(node);
        }

        node = Patterns.TEMP_SPLIT.split(node)[0];

        for (Map.Entry<String, Boolean> e : toQuery.entrySet()) {
            if (e.getKey().contains("$")) {
                String[] parts = Patterns.TEMP_SPLIT.split(e.getKey());
                if (parts[0].equalsIgnoreCase(node)) {
                    return b ? e.getValue() : !e.getValue();
                }
            }
        }

        return false;
    }

    /**
     * Checks to see if the object has a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b) {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        return hasPermission(this.nodes, node, b);
    }

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b, String server) {
        return hasPermission(server + "/" + node, b);
    }

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b, boolean temporary) {
        return hasPermission(node + (temporary ? "$a" : ""), b);
    }

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, boolean b) {
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
    public boolean inheritsPermission(String node, boolean b, String server) {
        final Map<String, Boolean> local = getLocalPermissions(server, null);
        return hasPermission(local, node, b);
    }

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param temporary if the permission is temporary
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, boolean b, boolean temporary) {
        return inheritsPermission(node + (temporary ? "$a" : ""), b);
    }

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value) throws ObjectAlreadyHasException {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        if (hasPermission(node, value)) {
            throw new ObjectAlreadyHasException();
        }
        this.nodes.put(node, value);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, String server) throws ObjectAlreadyHasException {
        setPermission(server + "/" + node, value);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, long expireAt) throws ObjectAlreadyHasException {
        setPermission(node + "$" + expireAt, value);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, String server, long expireAt) throws ObjectAlreadyHasException {
        setPermission(node + "$" + expireAt, value, server);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param temporary if the permission being removed is temporary
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, boolean temporary) throws ObjectLacksException {
        if (node.startsWith("global/")) node = node.replace("global/", "");
        final String fNode = node;
        Optional<String> match = Optional.empty();

        if (temporary) {
            match = this.nodes.keySet().stream()
                    .filter(n -> n.contains("$")).filter(n -> Patterns.TEMP_SPLIT.split(n)[0].equalsIgnoreCase(fNode))
                    .findFirst();
        } else {
            if (this.nodes.containsKey(fNode)) {
                match = Optional.of(fNode);
            }
        }

        if (match.isPresent()) {
            this.nodes.remove(match.get());
        } else {
            throw new ObjectLacksException();
        }
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node) throws ObjectLacksException {
        unsetPermission(node, node.contains("$"));
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server) throws ObjectLacksException {
        unsetPermission(server + "/" + node);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server, boolean temporary) throws ObjectLacksException {
        unsetPermission(server + "/" + node, temporary);
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return getPermissions(server, excludedGroups, plugin.getConfiguration().getIncludeGlobalPerms());
    }

    /**
     * Processes the objects and returns the temporary ones.
     * @return a map of temporary nodes
     */
    public Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes() {
        return this.nodes.entrySet().stream().filter(e -> e.getKey().contains("$")).map(e -> {
            final String[] parts = Patterns.TEMP_SPLIT.split(e.getKey());
            final long expiry = Long.parseLong(parts[1]);
            return new AbstractMap.SimpleEntry<Map.Entry<String, Boolean>, Long>(new AbstractMap.SimpleEntry<>(parts[0], e.getValue()), expiry);

        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Processes the objects and returns the non-temporary ones.
     * @return a map of permanent nodes
     */
    public Map<String, Boolean> getPermanentNodes() {
        return this.nodes.entrySet().stream().filter(e -> !e.getKey().contains("$"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Removes temporary permissions that have expired
     */
    public void auditTemporaryPermissions() {
        this.nodes.keySet().stream()
                .filter(s -> s.contains("$"))
                .filter(s -> DateUtil.shouldExpire(Long.parseLong(Patterns.TEMP_SPLIT.split(s)[1])))
                .forEach(s -> this.nodes.remove(s));
    }

    protected Map<String, Boolean> convertTemporaryPerms() {
        auditTemporaryPermissions();

        Map<String, Boolean> nodes = new HashMap<>();
        Map<String, Boolean> tempNodes = new HashMap<>();

        for (Map.Entry<String, Boolean> e : this.nodes.entrySet()) {
            if (e.getKey().contains("$")) {
                tempNodes.put(e.getKey(), e.getValue());
            } else {
                nodes.put(e.getKey(), e.getValue());
            }
        }

        // temporary permissions override non-temporary permissions
        tempNodes.entrySet().forEach(e -> nodes.put(stripTime(e.getKey()), e.getValue()));
        return nodes;
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
        for (Map.Entry<String, Boolean> node : convertTemporaryPerms().entrySet()) {
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

    private static String stripTime(String s) {
        if (s.contains("$")) {
            return Patterns.TEMP_SPLIT.split(s)[0];
        }
        return s;
    }
}
