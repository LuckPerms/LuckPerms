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
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.event.events.GroupRemoveEvent;
import me.lucko.luckperms.api.event.events.PermissionNodeExpireEvent;
import me.lucko.luckperms.api.event.events.PermissionNodeSetEvent;
import me.lucko.luckperms.api.event.events.PermissionNodeUnsetEvent;
import me.lucko.luckperms.api.implementation.internal.PermissionHolderLink;
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
    private Set<Node> nodes = ConcurrentHashMap.newKeySet();

    /**
     * The user/group's transient permissions
     */
    @Getter
    private Set<Node> transientNodes = ConcurrentHashMap.newKeySet();

    /**
     * Returns a Set of nodes in priority order
     * @return the holders transient and permanent nodes
     */
    public SortedSet<Node> getPermissions() {
        // Returns no duplicate nodes. as in, nodes with the same value.

        TreeSet<Node> combined = new TreeSet<>(PRIORITY_COMPARATOR);
        combined.addAll(nodes);
        combined.addAll(transientNodes);

        TreeSet<Node> permissions = new TreeSet<>(PRIORITY_COMPARATOR);

        combined:
        for (Node node : combined) {
            for (Node other : permissions) {
                if (node.equalsIgnoringValue(other)) {
                    continue combined;
                }
            }

            permissions.add(node);
        }

        return permissions;
    }

    /**
     * Removes temporary permissions that have expired
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryPermissions() {
        boolean work = false;

        Iterator<Node> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            Node element = iterator.next();
            if (element.hasExpired()) {
                iterator.remove();

                work = true;
                plugin.getApiProvider().fireEventAsync(new PermissionNodeExpireEvent(new PermissionHolderLink(this), element));
            }
        }

        Iterator<Node> iterator2 = transientNodes.iterator();
        while (iterator2.hasNext()) {
            Node element = iterator2.next();
            if (element.hasExpired()) {
                iterator2.remove();

                work = true;
                plugin.getApiProvider().fireEventAsync(new PermissionNodeExpireEvent(new PermissionHolderLink(this), element));
            }
        }

        return work;
    }

    /**
     * Gets all of the nodes that this holder has and inherits
     * @param excludedGroups a list of groups to exclude
     * @return a set of nodes
     */
    public SortedSet<Node> getAllNodes(List<String> excludedGroups) {
        SortedSet<Node> all = getPermissions();

        if (excludedGroups == null) {
            excludedGroups = new ArrayList<>();
        }

        excludedGroups.add(getObjectName().toLowerCase());

        Set<String> parents = getPermissions().stream()
                .filter(Node::isGroupNode)
                .map(Node::getGroupName)
                .collect(Collectors.toSet());

        for (String parent : parents) {
            Group group = plugin.getGroupManager().get(parent);
            if (group == null) {
                continue;
            }

            if (excludedGroups.contains(group.getObjectName())) {
                continue;
            }

            inherited:
            for (Node inherited : group.getAllNodes(excludedGroups)) {
                for (Node existing : all) {
                    if (existing.almostEquals(inherited)) {
                        continue inherited;
                    }
                }

                all.add(inherited);
            }
        }

        return all;
    }

    /**
     * Gets all of the nodes that this holder has (and inherits), given the context
     * @param server the server
     * @param world the world
     * @param extraContext any extra context to filter by
     * @param includeGlobal whether to include global nodes
     * @param applyGroups if inherited group permissions should be included
     * @return a map of permissions
     */
    public Set<Node> getAllNodesFiltered(String server, String world, Map<String, String> extraContext, boolean includeGlobal, boolean applyGroups) {
        Set<Node> perms = ConcurrentHashMap.newKeySet();
        SortedSet<Node> allNodes;

        if (applyGroups) {
            allNodes = sort(getAllNodes(null), true);
        } else {
            allNodes = sort(getPermissions(), true);
        }

        for (Node node : allNodes) {
            if (!node.shouldApplyOnServer(server, includeGlobal, plugin.getConfiguration().getApplyRegex())) {
                continue;
            }

            if (!node.shouldApplyOnWorld(world, includeGlobal, plugin.getConfiguration().getApplyRegex())) {
                continue;
            }

            if (!node.shouldApplyWithContext(extraContext)) {
                continue;
            }

            perms.add(node);
        }

        return perms;
    }

    /**
     * Converts the output of {@link #getAllNodesFiltered(String, String, Map, boolean, boolean)}, and expands wildcards/regex/shorthand perms
     * @param server the server
     * @param world the world
     * @param extraContext any extra context to filter by
     * @param includeGlobal whether to include global nodes
     * @param applyGroups if inherited group permissions should be included
     * @param possibleNodes a list of possible nodes for wildcards and regex permissions
     * @return a map of permissions
     */
    public Map<String, Boolean> exportNodes(String server, String world, Map<String, String> extraContext, boolean includeGlobal, boolean applyGroups, List<String> possibleNodes) {
        Map<String, Boolean> perms = new HashMap<>();

        for (Node node : getAllNodesFiltered(server, world, extraContext, includeGlobal, applyGroups)) {
            if (plugin.getConfiguration().getApplyWildcards()) {
                if (node.getPermission().equals("*") || node.getPermission().equals("'*'")) {
                    possibleNodes.forEach(n -> perms.put(n, true));
                    continue;
                }
            }

            perms.put(node.getPermission(), node.getValue());

            if (plugin.getConfiguration().getApplyShorthand()) {
                node.resolveShorthand().stream()
                        .filter(s -> !perms.containsKey(s))
                        .forEach(s -> perms.put(s, node.getValue()));
            }

            if (plugin.getConfiguration().getApplyWildcards()) {
                node.resolveWildcard(possibleNodes).stream()
                        .filter(s -> !perms.containsKey(s))
                        .forEach(s -> perms.put(s, node.getValue()));
            }
        }

        return Collections.unmodifiableMap(perms);
    }

    public void setNodes(Set<Node> nodes) {
        this.nodes.clear();
        this.nodes.addAll(nodes);
        auditTemporaryPermissions();
    }

    public void setTransiestNodes(Set<Node> nodes) {
        this.transientNodes.clear();
        this.transientNodes.addAll(nodes);
        auditTemporaryPermissions();
    }

    public static Map<String, Boolean> exportToLegacy(Set<Node> nodes) {
        Map<String, Boolean> m = new HashMap<>();
        for (Node node : nodes) {
            m.put(node.toSerializedNode(), node.getValue());
        }
        return Collections.unmodifiableMap(m);
    }

    // Convenience method
    private static Node.Builder buildNode(String permission) {
        return new me.lucko.luckperms.utils.Node.Builder(permission);
    }

    @Deprecated
    public void setNodes(Map<String, Boolean> nodes) {
        this.nodes.clear();

        this.nodes.addAll(nodes.entrySet().stream()
                .map(e -> me.lucko.luckperms.utils.Node.fromSerialisedNode(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));

        auditTemporaryPermissions();
    }

    private static Tristate hasPermission(Set<Node> toQuery, Node node) {
        for (Node n : toQuery) {
            if (n.almostEquals(node)) {
                return n.getTristate();
            }
        }

        return Tristate.UNDEFINED;
    }

    /**
     * Check if the holder has a permission node
     * @param node the node to check
     * @param t whether to check transient nodes
     * @return a tristate
     */
    public Tristate hasPermission(Node node, boolean t) {
        return hasPermission(t ? transientNodes : nodes, node);
    }

    public Tristate hasPermission(Node node) {
        return hasPermission(node, false);
    }

    public boolean hasPermission(String node, boolean b) {
        return hasPermission(buildNode(node).setValue(b).build()).asBoolean() == b;
    }

    public boolean hasPermission(String node, boolean b, String server) {
        return hasPermission(buildNode(node).setValue(b).setServer(server).build()).asBoolean() == b;
    }

    public boolean hasPermission(String node, boolean b, String server, String world) {
        return hasPermission(buildNode(node).setValue(b).setServer(server).setWorld(world).build()).asBoolean() == b;
    }

    public boolean hasPermission(String node, boolean b, boolean temporary) {
        return hasPermission(buildNode(node).setValue(b).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    public boolean hasPermission(String node, boolean b, String server, boolean temporary) {
        return hasPermission(buildNode(node).setValue(b).setServer(server).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    public boolean hasPermission(String node, boolean b, String server, String world, boolean temporary) {
        return hasPermission(buildNode(node).setValue(b).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    /**
     * Check if the holder inherits a node
     * @param node the node to check
     * @return a tristate
     */
    public Tristate inheritsPermission(Node node) {
        return hasPermission(getAllNodes(null), node);
    }

    public boolean inheritsPermission(String node, boolean b) {
        return inheritsPermission(buildNode(node).setValue(b).build()).asBoolean() == b;
    }

    public boolean inheritsPermission(String node, boolean b, String server) {
        return inheritsPermission(buildNode(node).setValue(b).setServer(server).build()).asBoolean() == b;
    }

    public boolean inheritsPermission(String node, boolean b, String server, String world) {
        return inheritsPermission(buildNode(node).setValue(b).setServer(server).setWorld(world).build()).asBoolean() == b;
    }

    public boolean inheritsPermission(String node, boolean b, boolean temporary) {
        return inheritsPermission(buildNode(node).setValue(b).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    public boolean inheritsPermission(String node, boolean b, String server, boolean temporary) {
        return inheritsPermission(buildNode(node).setValue(b).setServer(server).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    public boolean inheritsPermission(String node, boolean b, String server, String world, boolean temporary) {
        return inheritsPermission(buildNode(node).setValue(b).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build()).asBoolean() == b;
    }

    /**
     * Sets a permission node
     * @param node the node to set
     * @throws ObjectAlreadyHasException if the holder has this permission already
     */
    public void setPermission(Node node) throws ObjectAlreadyHasException {
        if (hasPermission(node, false) == Tristate.TRUE) {
            throw new ObjectAlreadyHasException();
        }

        nodes.add(node);
        plugin.getApiProvider().fireEventAsync(new PermissionNodeSetEvent(new PermissionHolderLink(this), node));
    }

    /**
     * Sets a transient permission node
     * @param node the node to set
     * @throws ObjectAlreadyHasException if the holder has this permission already
     */
    public void setTransientPermission(Node node) throws ObjectAlreadyHasException {
        if (hasPermission(node, true) == Tristate.TRUE) {
            throw new ObjectAlreadyHasException();
        }

        transientNodes.add(node);
        plugin.getApiProvider().fireEventAsync(new PermissionNodeSetEvent(new PermissionHolderLink(this), node));
    }

    public void setPermission(String node, boolean value) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).build());
    }

    public void setPermission(String node, boolean value, String server) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).setServer(server).build());
    }

    public void setPermission(String node, boolean value, String server, String world) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).setServer(server).setWorld(world).build());
    }

    public void setPermission(String node, boolean value, long expireAt) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).setExpiry(expireAt).build());
    }

    public void setPermission(String node, boolean value, String server, long expireAt) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).setServer(server).setExpiry(expireAt).build());
    }

    public void setPermission(String node, boolean value, String server, String world, long expireAt) throws ObjectAlreadyHasException {
        setPermission(buildNode(node).setValue(value).setServer(server).setWorld(world).setExpiry(expireAt).build());
    }

    /**
     * Unsets a permission node
     * @param node the node to unset
     * @throws ObjectLacksException if the holder doesn't have this node already
     */
    public void unsetPermission(Node node) throws ObjectLacksException {
        if (hasPermission(node, false) != Tristate.TRUE) {
            throw new ObjectLacksException();
        }

        Iterator<Node> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            Node entry = iterator.next();
            if (entry.almostEquals(node)) {
                iterator.remove();
            }
        }

        if (node.isGroupNode()) {
            plugin.getApiProvider().fireEventAsync(new GroupRemoveEvent(new PermissionHolderLink(this),
                    node.getGroupName(), node.getServer().orElse(null), node.getWorld().orElse(null), node.isTemporary()));
        } else {
            plugin.getApiProvider().fireEventAsync(new PermissionNodeUnsetEvent(new PermissionHolderLink(this), node));
        }
    }

    /**
     * Unsets a transient permission node
     * @param node the node to unset
     * @throws ObjectLacksException if the holder doesn't have this node already
     */
    public void unsetTransientPermission(Node node) throws ObjectLacksException {
        if (hasPermission(node, true) == Tristate.FALSE) {
            throw new ObjectLacksException();
        }

        Iterator<Node> iterator = transientNodes.iterator();
        while (iterator.hasNext()) {
            Node entry = iterator.next();
            if (entry.almostEquals(node)) {
                iterator.remove();
            }
        }

        if (node.isGroupNode()) {
            plugin.getApiProvider().fireEventAsync(new GroupRemoveEvent(new PermissionHolderLink(this),
                    node.getGroupName(), node.getServer().orElse(null), node.getWorld().orElse(null), node.isTemporary()));
        } else {
            plugin.getApiProvider().fireEventAsync(new PermissionNodeUnsetEvent(new PermissionHolderLink(this), node));
        }
    }

    public void unsetPermission(String node, boolean temporary) throws ObjectLacksException {
        unsetPermission(buildNode(node).setExpiry(temporary ? 10L : 0L).build());
    }

    public void unsetPermission(String node) throws ObjectLacksException {
        unsetPermission(buildNode(node).build());
    }

    public void unsetPermission(String node, String server) throws ObjectLacksException {
        unsetPermission(buildNode(node).setServer(server).build());
    }

    public void unsetPermission(String node, String server, String world) throws ObjectLacksException {
        unsetPermission(buildNode(node).setServer(server).setWorld(world).build());
    }

    public void unsetPermission(String node, String server, boolean temporary) throws ObjectLacksException {
        unsetPermission(buildNode(node).setServer(server).setExpiry(temporary ? 10L : 0L).build());
    }

    public void unsetPermission(String node, String server, String world, boolean temporary) throws ObjectLacksException {
        unsetPermission(buildNode(node).setServer(server).setWorld(world).setExpiry(temporary ? 10L : 0L).build());
    }

    /**
     * @return The temporary nodes held by the holder
     */
    public Set<Node> getTemporaryNodes() {
        return getPermissions().stream().filter(Node::isTemporary).collect(Collectors.toSet());
    }

    @Deprecated
    public Map<Map.Entry<String, Boolean>, Long> getTemporaryNodesLegacy() {
        Map<Map.Entry<String, Boolean>, Long> m = new HashMap<>();

        for (Node node : getTemporaryNodes()) {
            m.put(new AbstractMap.SimpleEntry<>(node.getKey(), node.getValue()), node.getExpiryUnixTime());
        }

        return m;
    }

    /**
     * @return The permanent nodes held by the holder
     */
    public Set<Node> getPermanentNodes() {
        return getPermissions().stream().filter(Node::isPermanent).collect(Collectors.toSet());
    }

    /*
     * Don't use these methods, only here for compat reasons
     */
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups, List<String> possibleNodes) {
        return exportNodes(server, world, null, plugin.getConfiguration().getIncludeGlobalPerms(), true, possibleNodes);
    }

    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups) {
        return exportNodes(server, world, null, plugin.getConfiguration().getIncludeGlobalPerms(), true, null);
    }

    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups, List<String> possibleNodes) {
        return getLocalPermissions(server, null, excludedGroups, possibleNodes);
    }

    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return getLocalPermissions(server, null, excludedGroups, null);
    }

    public static SortedSet<Node> sort(Set<Node> toSort, boolean reversed) {
        TreeSet<Node> set = new TreeSet<>(reversed ? PRIORITY_COMPARATOR.reversed() : PRIORITY_COMPARATOR);
        set.addAll(toSort);
        return set;
    }

    private static final PriorityComparator PRIORITY_COMPARATOR = new PriorityComparator();
    private static class PriorityComparator implements Comparator<Node> {

        @Override
        public int compare(Node o1, Node o2) {
            if (o1.equals(o2)) {
                return 0;
            }

            if (o1.isOverride() != o2.isOverride()) {
                return o1.isOverride() ? 1 : -1;
            }

            if (o1.isServerSpecific() != o2.isServerSpecific()) {
                return o1.isServerSpecific() ? 1 : -1;
            }

            if (o1.isWorldSpecific() != o2.isWorldSpecific()) {
                return o1.isWorldSpecific() ? 1 : -1;
            }

            if (o1.isTemporary() != o2.isTemporary()) {
                return o1.isTemporary() ? 1 : -1;
            }

            if (o1.isWildcard() != o2.isWildcard()) {
                return o1.isWildcard() ? 1 : -1;
            }

            if (o1.isTemporary()) {
                return o1.getSecondsTilExpiry() < o2.getSecondsTilExpiry() ? 1 : -1;
            }

            if (o1.isWildcard()) {
                return o1.getWildcardLevel() > o2.getWildcardLevel() ? 1 : -1;
            }

            return 1;
        }
    }
}
