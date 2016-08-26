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

package me.lucko.luckperms.core;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.event.events.GroupRemoveEvent;
import me.lucko.luckperms.api.event.events.PermissionExpireEvent;
import me.lucko.luckperms.api.event.events.PermissionSetEvent;
import me.lucko.luckperms.api.event.events.PermissionUnsetEvent;
import me.lucko.luckperms.api.implementation.internal.PermissionHolderLink;
import me.lucko.luckperms.constants.Patterns;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.DateUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Represents an object that can hold permissions
 * For example a User or a Group
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PermissionHolder {

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

        node = Patterns.TEMP_DELIMITER.split(node)[0];

        for (Map.Entry<String, Boolean> e : toQuery.entrySet()) {
            if (e.getKey().contains("$")) {
                String[] parts = Patterns.TEMP_DELIMITER.split(e.getKey());
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
     * @param server The server
     * @param world The world
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b, String server, String world) {
        return hasPermission(server + "-" + world + "/" + node, b);
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
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server to check on
     * @param temporary if the permission is temporary
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b, String server, boolean temporary) {
        return hasPermission(server + "/" + node + (temporary ? "$a" : ""), b);
    }

    /**
     * Checks to see the the object has a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server to check on
     * @param world The world to check on
     * @param temporary if the permission is temporary
     * @return true if the user has the permission
     */
    public boolean hasPermission(String node, boolean b, String server, String world, boolean temporary) {
        return hasPermission(server + "-" + world + "/" + node + (temporary ? "$a" : ""), b);
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
            final String[] parts = Patterns.SERVER_DELIMITER.split(node, 2);
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
        if (server.contains("-")) {
            // Use other method
            final String[] parts = Patterns.WORLD_DELIMITER.split(server, 2);
            return inheritsPermission(node, b, parts[0], parts[1]);
        }

        final Map<String, Boolean> local = getLocalPermissions(server, null);
        return hasPermission(local, node, b);
    }

    /**
     * Checks to see the the object inherits a permission on a certain server
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param world The world
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, boolean b, String server, String world) {
        final Map<String, Boolean> local = getLocalPermissions(server, world, null);
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
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param temporary if the permission is temporary
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, boolean b, String server, boolean temporary) {
        return inheritsPermission(server + "/" + node + (temporary ? "$a" : ""), b);
    }

    /**
     * Checks to see if the object inherits a certain permission
     * @param node The permission node
     * @param b If the node is true/false(negated)
     * @param server The server
     * @param world The world
     * @param temporary if the permission is temporary
     * @return true if the user inherits the permission
     */
    public boolean inheritsPermission(String node, boolean b, String server, String world, boolean temporary) {
        return inheritsPermission(server + "-" + world + "/" + node + (temporary ? "$a" : ""), b);
    }

    /**
     * Sets a permission for the object
     * @param node The node to be set
     * @param value What to set the node to - true/false(negated)
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value) throws ObjectAlreadyHasException {
        setPermission(node, value, null, null, 0L);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, String server) throws ObjectAlreadyHasException {
        setPermission(node, value, server, null, 0L);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param world The world to set the permission on
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, String server, String world) throws ObjectAlreadyHasException {
        setPermission(node, value, server, world, 0L);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, long expireAt) throws ObjectAlreadyHasException {
        setPermission(node, value, null, null, expireAt);
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
        setPermission(node, value, server, null, expireAt);
    }

    /**
     * Sets a permission for the object
     * @param node The node to set
     * @param value What to set the node to - true/false(negated)
     * @param server The server to set the permission on
     * @param world The world to set the permission on
     * @param expireAt The time in unixtime when the permission will expire
     * @throws ObjectAlreadyHasException if the object already has the permission
     */
    public void setPermission(String node, boolean value, String server, String world, long expireAt) throws ObjectAlreadyHasException {
        if (node.startsWith("global/")) node = node.replace("global/", "");

        if (server != null && server.equals("")) server = null;
        if (world != null && world.equals("")) world = null;

        StringBuilder builder = new StringBuilder();

        if (server != null) {
            builder.append(server);

            if (world != null) {
                builder.append("-").append(world);
            }
            builder.append("/");
        } else {
            if (world != null) {
                builder.append("global-").append(world).append("/");
            }
        }

        builder.append(node);

        if (expireAt != 0L) {
            builder.append("$").append(expireAt);
        }

        final String finalNode = builder.toString();


        if (hasPermission(finalNode, value)) {
            throw new ObjectAlreadyHasException();
        }

        this.nodes.put(finalNode, value);
        plugin.getApiProvider().fireEventAsync(new PermissionSetEvent(
                new PermissionHolderLink(this), node, value, server, world, expireAt));
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param temporary if the permission being removed is temporary
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, boolean temporary) throws ObjectLacksException {
        unsetPermission(node, null, null, temporary);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node) throws ObjectLacksException {
        unsetPermission(node, null, null, false);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server) throws ObjectLacksException {
        unsetPermission(node, server, null, false);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param world The world to unset the node on
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server, String world) throws ObjectLacksException {
        unsetPermission(node, server, world, false);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server, boolean temporary) throws ObjectLacksException {
        unsetPermission(node, server, null, temporary);
    }

    /**
     * Unsets a permission for the object
     * @param node The node to be unset
     * @param server The server to unset the node on
     * @param world The world to unset the node on
     * @param temporary if the permission being unset is temporary
     * @throws ObjectLacksException if the node wasn't already set
     */
    public void unsetPermission(String node, String server, String world, boolean temporary) throws ObjectLacksException {
        if (node.startsWith("global/")) node = node.replace("global/", "");

        if (server != null && server.equals("")) server = null;
        if (world != null && world.equals("")) world = null;

        StringBuilder builder = new StringBuilder();

        if (server != null) {
            builder.append(server);

            if (world != null) {
                builder.append("-").append(world);
            }
            builder.append("/");
        } else {
            if (world != null) {
                builder.append("global-").append(world).append("/");
            }
        }

        builder.append(node);

        final String finalNode = builder.toString();
        Optional<String> match = Optional.empty();

        if (temporary) {
            match = this.nodes.keySet().stream()
                    .filter(n -> n.contains("$"))
                    .filter(n -> Patterns.TEMP_DELIMITER.split(n)[0].equalsIgnoreCase(finalNode))
                    .findFirst();
        } else {
            if (this.nodes.containsKey(finalNode)) {
                match = Optional.of(finalNode);
            }
        }

        if (match.isPresent()) {
            this.nodes.remove(match.get());
            plugin.getApiProvider().fireEventAsync(new PermissionUnsetEvent(
                    new PermissionHolderLink(this), node, server, world, temporary));
            if (node.startsWith("group.")) {
                plugin.getApiProvider().fireEventAsync(new GroupRemoveEvent(
                        new PermissionHolderLink(this), Patterns.DOT.split(node, 2)[1], server, world, temporary));
            }

        } else {
            throw new ObjectLacksException();
        }
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups, List<String> possibleNodes) {
        return getPermissions(server, world, excludedGroups, plugin.getConfiguration().getIncludeGlobalPerms(), possibleNodes);
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param world The world to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups) {
        return getPermissions(server, world, excludedGroups, plugin.getConfiguration().getIncludeGlobalPerms(), null);
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @param possibleNodes A list of possible permission nodes for wildcard permission handling
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups, List<String> possibleNodes) {
        return getLocalPermissions(server, null, excludedGroups, possibleNodes);
    }

    /**
     * Gets the permissions and inherited permissions that apply to a specific server
     * @param server The server to get nodes for
     * @param excludedGroups Groups that shouldn't be inherited (to prevent circular inheritance issues)
     * @return a {@link Map} of the permissions
     */
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return getLocalPermissions(server, null, excludedGroups, null);
    }

    /**
     * Processes the objects and returns the temporary ones.
     * @return a map of temporary nodes
     */
    public Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes() {
        return this.nodes.entrySet().stream().filter(e -> e.getKey().contains("$")).map(e -> {
            final String[] parts = Patterns.TEMP_DELIMITER.split(e.getKey());
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
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryPermissions() {
        List<String> toExpire = this.nodes.keySet().stream()
                .filter(s -> s.contains("$"))
                .filter(s -> DateUtil.shouldExpire(Long.parseLong(Patterns.TEMP_DELIMITER.split(s)[1])))
                .collect(Collectors.toList());

        toExpire.forEach(s -> {
            plugin.getApiProvider().fireEventAsync(new PermissionExpireEvent(new PermissionHolderLink(this), s));
            this.nodes.remove(s);
        });
        return !toExpire.isEmpty();
    }

    public Map<String, Boolean> convertTemporaryPerms() {
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

    public Map<String, Boolean> getPermissions(String server, String world, List<String> excludedGroups, boolean includeGlobal, List<String> possibleNodes) {
        return getPermissions(server, world, excludedGroups, includeGlobal, possibleNodes, true);
    }

    public Map<String, Boolean> getPermissions(String server, String world, List<String> excludedGroups, boolean includeGlobal, List<String> possibleNodes, boolean applyGroups) {
        if (excludedGroups == null) {
            excludedGroups = new ArrayList<>();
        }

        excludedGroups.add(getObjectName());
        Map<String, Boolean> perms = new HashMap<>();

        if (server == null || server.equals("")) {
            server = "global";
        }

        if (world != null && world.equalsIgnoreCase("")) {
            world = null;
        }

        /*
        Priority:

        1. server+world specific nodes
        2. server specific nodes
        3. user nodes
        4. server+world specific group nodes
        5. server specific group nodes
        6. group nodes
        */

        final Map<String, Boolean> serverWorldSpecificNodes = new HashMap<>();
        final Map<String, Boolean> serverSpecificNodes = new HashMap<>();
        final Map<String, Boolean> userNodes = new HashMap<>();
        final Map<String, Boolean> serverWorldSpecificGroups = new HashMap<>();
        final Map<String, Boolean> serverSpecificGroups = new HashMap<>();
        final Map<String, Boolean> groupNodes = new HashMap<>();

        // Sorts the permissions and puts them into a priority order
        for (Map.Entry<String, Boolean> node : convertTemporaryPerms().entrySet()) {
            serverSpecific:
            if (node.getKey().contains("/")) {
                String[] parts = Patterns.SERVER_DELIMITER.split(node.getKey(), 2);
                // 0=server(+world)   1=node

                // WORLD SPECIFIC
                if (parts[0].contains("-")) {
                    String[] serverParts = Patterns.WORLD_DELIMITER.split(parts[0], 2);
                    // 0=server   1=world

                    if ((!serverParts[0].equalsIgnoreCase("global") || !includeGlobal) && (!matches(server, serverParts[0]))) {
                        // GLOBAL AND UNWANTED OR SERVER SPECIFIC BUT DOES NOT APPLY :(((
                        continue;
                    }

                    if (world != null && !matches(world, serverParts[1])) {
                        // WORLD SPECIFIC BUT DOES NOT APPLY
                        continue;
                    }

                    if (Patterns.GROUP_MATCH.matcher(parts[1]).matches()) {
                        // SERVER+WORLD SPECIFIC AND GROUP
                        serverWorldSpecificGroups.put(node.getKey(), node.getValue());
                        continue;
                    }

                    // SERVER+WORLD SPECIFIC
                    serverWorldSpecificNodes.put(node.getKey(), node.getValue());
                    continue;
                }

                if (parts[0].equalsIgnoreCase("global")) {
                    // REGULAR
                    break serverSpecific;
                }

                if (!matches(server, parts[0])) {
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

            // Could be here if the server was set to global.
            String n = node.getKey();
            if (n.contains("/")) {
                n = Patterns.SERVER_DELIMITER.split(n, 2)[1];
            }

            if (Patterns.GROUP_MATCH.matcher(n).matches()) {
                // GROUP
                groupNodes.put(n, node.getValue());
                continue;
            }

            // JUST NORMAL
            userNodes.put(n, node.getValue());
        }

        // If a group is negated at a higher priority, the group should not then be applied at a lower priority
        serverWorldSpecificGroups.entrySet().stream().filter(node -> !node.getValue()).forEach(node -> {
            groupNodes.remove(node.getKey());
            groupNodes.remove(Patterns.SERVER_DELIMITER.split(node.getKey(), 2)[1]);
            serverSpecificGroups.remove(node.getKey());
            serverSpecificGroups.remove(Patterns.SERVER_DELIMITER.split(node.getKey(), 2)[1]);
            serverSpecificGroups.remove(Patterns.WORLD_DELIMITER.split(node.getKey(), 2)[0] + "/" + Patterns.SERVER_DELIMITER.split(node.getKey(), 2)[1]);
        });
        serverSpecificGroups.entrySet().stream().filter(node -> !node.getValue()).forEach(node -> {
            groupNodes.remove(node.getKey());
            groupNodes.remove(Patterns.SERVER_DELIMITER.split(node.getKey(), 2)[1]);
        });

        if (applyGroups) {
            // Apply lowest priority: groupNodes
            for (Map.Entry<String, Boolean> groupNode : groupNodes.entrySet()) {
                // Add the actual group perm node, so other plugins can hook
                perms.put(groupNode.getKey(), groupNode.getValue());

                // Don't add negated groups
                if (!groupNode.getValue()) continue;

                String groupName = Patterns.DOT.split(groupNode.getKey(), 2)[1];
                if (!excludedGroups.contains(groupName)) {
                    Group group = plugin.getGroupManager().get(groupName);
                    if (group != null) {
                        perms.putAll(group.getLocalPermissions(server, excludedGroups));
                    } else {
                        plugin.getLog().warn("Error whilst refreshing the permissions of '" + objectName + "'." +
                                "\n The group '" + groupName + "' is not loaded.");
                    }
                }
            }

            applyShorthandIfEnabled(perms);

            // Apply next priorities: serverSpecificGroups and then serverWorldSpecificGroups
            for (Map<String, Boolean> m : Arrays.asList(serverSpecificGroups, serverWorldSpecificGroups)) {
                for (Map.Entry<String, Boolean> groupNode : m.entrySet()) {
                    final String rawNode = Patterns.SERVER_DELIMITER.split(groupNode.getKey())[1];

                    // Add the actual group perm node, so other plugins can hook
                    perms.put(rawNode, groupNode.getValue());

                    // Don't add negated groups
                    if (!groupNode.getValue()) continue;

                    String groupName = Patterns.DOT.split(rawNode, 2)[1];
                    if (!excludedGroups.contains(groupName)) {
                        Group group = plugin.getGroupManager().get(groupName);
                        if (group != null) {
                            perms.putAll(group.getLocalPermissions(server, excludedGroups));
                        } else {
                            plugin.getLog().warn("Error whilst refreshing the permissions of '" + objectName + "'." +
                                    "\n The group '" + groupName + "' is not loaded.");
                        }
                    }
                }
                applyShorthandIfEnabled(perms);
            }
        }

        // Apply next priority: userNodes
        perms.putAll(userNodes);
        applyShorthandIfEnabled(perms);

        // Apply final priorities: serverSpecificNodes and then serverWorldSpecificNodes
        for (Map<String, Boolean> m : Arrays.asList(serverSpecificNodes, serverWorldSpecificNodes)) {
            for (Map.Entry<String, Boolean> node : m.entrySet()) {
                final String rawNode = Patterns.SERVER_DELIMITER.split(node.getKey())[1];
                perms.put(rawNode, node.getValue());
            }
            applyShorthandIfEnabled(perms);
        }

        if (plugin.getConfiguration().getApplyRegex()) {
            if (possibleNodes != null && !possibleNodes.isEmpty()) {
                perms =  applyRegex(perms, possibleNodes);
            } else {
                perms =  applyRegex(perms, plugin.getPossiblePermissions());
            }
        }

        applyShorthandIfEnabled(perms);

        if (plugin.getConfiguration().getApplyWildcards()) {
            if (possibleNodes != null && !possibleNodes.isEmpty()) {
                perms =  applyWildcards(perms, possibleNodes);
            } else {
                perms =  applyWildcards(perms, plugin.getPossiblePermissions());
            }
        }

        return perms;
    }

    private boolean matches(String entry, String possibleRegex) {
        if (possibleRegex.toLowerCase().startsWith("r=") && plugin.getConfiguration().getApplyRegex()) {
            Pattern p = Patterns.compile(possibleRegex.substring(2));
            if (p == null) {
                return false;
            }
            return p.matcher(entry).matches();
        }

        if (possibleRegex.startsWith("(") && possibleRegex.endsWith(")") && possibleRegex.contains("|")) {
            final String bits = possibleRegex.substring(1, possibleRegex.length() - 1);
            String[] parts = Patterns.VERTICAL_BAR.split(bits);

            for (String s : parts) {
                if (s.equalsIgnoreCase(entry)) {
                    return true;
                }
            }

            return false;
        }

        return entry.equalsIgnoreCase(possibleRegex);
    }

    private Map<String, Boolean> applyRegex(Map<String, Boolean> input, List<String> possibleNodes) {
        for (Map.Entry<String, Boolean> e : input.entrySet()) {
            if (!e.getKey().startsWith("r=") && !e.getKey().startsWith("R=")) {
                continue;
            }

            final Pattern node = Patterns.compile(e.getKey().substring(2));
            if (node == null) continue;
            possibleNodes.stream()
                    .filter(n -> node.matcher(n).matches())
                    .filter(n -> !input.containsKey(n))
                    .forEach(n -> input.put(n, e.getValue()));
        }

        return input;
    }

    // TODO Support the "Sponge way" of doing wildcards
    private static Map<String, Boolean> applyWildcards(Map<String, Boolean> input, List<String> possibleNodes) {
        SortedMap<Integer, Map<String, Boolean>> wildcards = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, Boolean> e : input.entrySet()) {
            if (e.getKey().equals("*") || e.getKey().equals("'*'")) {
                wildcards.put(0, Collections.singletonMap("*", e.getValue()));
                continue;
            }

            if (!e.getKey().endsWith(".*")) {
                continue;
            }

            final String node = e.getKey().substring(0, e.getKey().length() - 2);
            final String[] parts = Patterns.DOT.split(node);

            if (!wildcards.containsKey(parts.length)) {
                wildcards.put(parts.length, new HashMap<>());
            }

            wildcards.get(parts.length).put(node, e.getValue());
        }

        for (Map.Entry<Integer, Map<String, Boolean>> e : wildcards.entrySet()) {
            if (e.getKey() == 0) {
                // Apply all permissions
                possibleNodes.stream()
                        .filter(n -> !input.containsKey(n)) // Don't override existing nodes
                        .forEach(n -> input.put(n, e.getValue().get("*")));
                break;
            }

            for (Map.Entry<String, Boolean> wc : e.getValue().entrySet()) {
                possibleNodes.stream()
                        .filter(n -> n.startsWith(wc.getKey() + ".")) // Only nodes that match the wildcard are applied
                        .filter(n -> !input.containsKey(n)) // Don't override existing nodes
                        .forEach(n -> input.put(n, wc.getValue()));
            }
        }

        return input;
    }

    private void applyShorthandIfEnabled(Map<String, Boolean> map) {
        if (plugin.getConfiguration().getApplyShorthand()) {
            applyShorthand(map);
        }
    }

    private static Map<String, Boolean> applyShorthand(Map<String, Boolean> input) {
        for (Map.Entry<String, Boolean> e : input.entrySet()) {
            if (!Patterns.SHORTHAND_NODE.matcher(e.getKey()).find()) {
                continue;
            }

            if (!e.getKey().contains(".")) {
                continue;
            }

            String[] parts = Patterns.DOT.split(e.getKey());
            List<Set<String>> nodeParts = new ArrayList<>();

            for (String s : parts) {
                if ((!s.startsWith("(") || !s.endsWith(")")) || !s.contains("|")) {
                    nodeParts.add(Collections.singleton(s));
                    continue;
                }

                final String bits = s.substring(1, s.length() - 1);
                nodeParts.add(new HashSet<>(Arrays.asList(Patterns.VERTICAL_BAR.split(bits))));
            }

            Set<String> nodes = new HashSet<>();
            for (Set<String> set : nodeParts) {
                final Set<String> newNodes = new HashSet<>();
                if (nodes.isEmpty()) {
                    newNodes.addAll(set);
                } else {
                    nodes.forEach(str -> newNodes.addAll(set.stream()
                            .map(add -> str + "." + add)
                            .collect(Collectors.toList()))
                    );
                }
                nodes = newNodes;
            }

            nodes.stream()
                    .filter(n -> !input.containsKey(n)) // Don't override existing nodes
                    .forEach(n -> input.put(n, e.getValue()));
        }

        return input;
    }

    private static String stripTime(String s) {
        if (s.contains("$")) {
            return Patterns.TEMP_DELIMITER.split(s)[0];
        }
        return s;
    }
}
