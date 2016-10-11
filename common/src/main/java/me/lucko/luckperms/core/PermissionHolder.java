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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
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
import me.lucko.luckperms.utils.Cache;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
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

    private final Set<Node> nodes = new HashSet<>();
    private final Set<Node> transientNodes = new HashSet<>();

    private Cache<ImmutableSortedSet<LocalizedNode>> cache = new Cache<>();
    private Cache<ImmutableSortedSet<LocalizedNode>> mergedCache = new Cache<>();
    private Cache<ImmutableSet<Node>> enduringCache = new Cache<>();
    private Cache<ImmutableSet<Node>> transientCache = new Cache<>();

    @Getter
    private final Lock ioLock = new ReentrantLock();

    public abstract String getFriendlyName();

    private void invalidateCache(boolean enduring) {
        if (enduring) {
            enduringCache.invalidate();
        } else {
            transientCache.invalidate();
        }
        cache.invalidate();
        mergedCache.invalidate();
    }

    public Set<Node> getNodes() {
        synchronized (nodes) {
            return enduringCache.get(() -> ImmutableSet.copyOf(nodes));
        }
    }

    public Set<Node> getTransientNodes() {
        synchronized (transientNodes) {
            return transientCache.get(() -> ImmutableSet.copyOf(transientNodes));
        }
    }

    public void setNodes(Set<Node> nodes) {
        synchronized (this.nodes) {
            if (!this.nodes.equals(nodes)) {
                invalidateCache(true);
            }

            this.nodes.clear();
            this.nodes.addAll(nodes);
        }

        auditTemporaryPermissions();
    }

    public void setTransientNodes(Set<Node> nodes) {
        synchronized (this.transientNodes) {
            if (!this.transientNodes.equals(nodes)) {
                invalidateCache(false);
            }

            this.transientNodes.clear();
            this.transientNodes.addAll(nodes);
        }

        auditTemporaryPermissions();
    }

    @Deprecated
    public void setNodes(Map<String, Boolean> nodes) {
        synchronized (this.nodes) {
            if (!this.nodes.equals(nodes)) {
                invalidateCache(true);
            }

            this.nodes.clear();
            this.nodes.addAll(nodes.entrySet().stream()
                    .map(e -> me.lucko.luckperms.core.Node.fromSerialisedNode(e.getKey(), e.getValue()))
                    .collect(Collectors.toList()));
        }

        auditTemporaryPermissions();
    }

    public void addNodeUnchecked(Node node) {
        synchronized (nodes) {
            nodes.add(node);
            invalidateCache(true);
        }
    }

    /**
     * Clear all of the holders permission nodes
     */
    public void clearNodes() {
        synchronized (nodes) {
            nodes.clear();
            invalidateCache(true);
        }
    }

    public void clearNodes(String server) {
        if (server == null) {
            server = "global";
        }
        String finalServer = server;

        synchronized (nodes) {
            nodes.removeIf(n -> n.getServer().orElse("global").equalsIgnoreCase(finalServer));
            invalidateCache(true);
        }
    }

    public void clearNodes(String server, String world) {
        if (server == null) {
            server = "global";
        }
        String finalServer = server;

        if (world == null) {
            world = "null";
        }
        String finalWorld = world;

        synchronized (nodes) {
            nodes.removeIf(n -> n.getServer().orElse("global").equalsIgnoreCase(finalServer) && n.getWorld().orElse("null").equalsIgnoreCase(finalWorld));
            invalidateCache(true);
        }
    }

    public void clearMeta() {
        synchronized (nodes) {
            nodes.removeIf(n -> n.isMeta() || n.isPrefix() || n.isSuffix());
            invalidateCache(true);
        }
    }

    public void clearMeta(String server) {
        if (server == null) {
            server = "global";
        }
        String finalServer = server;

        synchronized (nodes) {
            nodes.removeIf(n -> (n.isMeta() || n.isPrefix() || n.isSuffix()) &&
                    n.getServer().orElse("global").equalsIgnoreCase(finalServer)
            );
            invalidateCache(true);
        }
    }

    public void clearMeta(String server, String world) {
        if (server == null) {
            server = "global";
        }
        String finalServer = server;

        if (world == null) {
            world = "null";
        }
        String finalWorld = world;

        synchronized (nodes) {
            nodes.removeIf(n -> (n.isMeta() || n.isPrefix() || n.isSuffix()) &&
                    (n.getServer().orElse("global").equalsIgnoreCase(finalServer) &&
                            n.getWorld().orElse("null").equalsIgnoreCase(finalWorld))
            );
            invalidateCache(true);
        }
    }

    public void clearTransientNodes() {
        synchronized (transientNodes) {
            transientNodes.clear();
            invalidateCache(false);
        }
    }

    /**
     * Combines and returns this holders nodes in a priority order.
     * @return the holders transient and permanent nodes
     */
    public SortedSet<LocalizedNode> getPermissions(boolean mergeTemp) {
        Supplier<ImmutableSortedSet<LocalizedNode>> supplier = () -> {
            TreeSet<LocalizedNode> combined = new TreeSet<>(PriorityComparator.reverse());

            getNodes().stream()
                    .map(n -> me.lucko.luckperms.utils.LocalizedNode.of(n, getObjectName()))
                    .forEach(combined::add);

            getTransientNodes().stream()
                    .map(n -> me.lucko.luckperms.utils.LocalizedNode.of(n, getObjectName()))
                    .forEach(combined::add);

            TreeSet<LocalizedNode> permissions = new TreeSet<>(PriorityComparator.reverse());

            combined:
            for (LocalizedNode node : combined) {
                for (LocalizedNode other : permissions) {
                    if (mergeTemp ? node.getNode().equalsIgnoringValueOrTemp(other.getNode()) : node.getNode().almostEquals(other.getNode())) {
                        continue combined;
                    }
                }

                permissions.add(node);
            }

            return ImmutableSortedSet.copyOfSorted(permissions);
        };

        return mergeTemp ? mergedCache.get(supplier) : cache.get(supplier);
    }

    /**
     * Removes temporary permissions that have expired
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryPermissions() {
        boolean work = false;
        final PermissionHolder instance = this;

        synchronized (nodes) {
            boolean w = nodes.removeIf(node -> {
                if (node.hasExpired()) {
                    plugin.getApiProvider().fireEventAsync(new PermissionNodeExpireEvent(new PermissionHolderLink(instance), node));
                    return true;
                }
                return false;
            });
            if (w) {
                invalidateCache(true);
                work = true;
            }
        }

        synchronized (transientNodes) {
            boolean w = transientNodes.removeIf(node -> {
                if (node.hasExpired()) {
                    plugin.getApiProvider().fireEventAsync(new PermissionNodeExpireEvent(new PermissionHolderLink(instance), node));
                    return true;
                }
                return false;
            });
            if (w) {
                invalidateCache(false);
                work = true;
            }
        }

        return work;
    }

    /**
     * Resolves inherited nodes and returns them
     * @param excludedGroups a list of groups to exclude
     * @param context context to decide if groups should be applied
     * @return a set of nodes
     */
    public SortedSet<LocalizedNode> getAllNodes(List<String> excludedGroups, Contexts context) {
        SortedSet<LocalizedNode> all = new TreeSet<>((SortedSet<LocalizedNode>) getPermissions(true));

        if (excludedGroups == null) {
            excludedGroups = new ArrayList<>();
        }

        excludedGroups.add(getObjectName().toLowerCase());

        Set<Node> parents = getPermissions(true).stream()
                .map(LocalizedNode::getNode)
                .filter(Node::isGroupNode)
                .collect(Collectors.toSet());

        Map<String, String> contexts = new HashMap<>(context.getContext());
        String server = contexts.get("server");
        String world = contexts.get("world");
        contexts.remove("server");
        contexts.remove("world");

        parents.removeIf(node ->
                !node.shouldApplyOnServer(server, context.isApplyGlobalGroups(), plugin.getConfiguration().isApplyingRegex()) ||
                !node.shouldApplyOnWorld(world, context.isApplyGlobalWorldGroups(), plugin.getConfiguration().isApplyingRegex()) ||
                !node.shouldApplyWithContext(contexts, false)
        );

        for (Node parent : parents) {
            Group group = plugin.getGroupManager().get(parent.getGroupName());
            if (group == null) {
                continue;
            }

            if (excludedGroups.contains(group.getObjectName())) {
                continue;
            }

            inherited:
            for (LocalizedNode inherited : group.getAllNodes(excludedGroups, context)) {
                for (LocalizedNode existing : all) {
                    if (existing.getNode().almostEquals(inherited.getNode())) {
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
     * @param context the context for this request
     * @return a map of permissions
     */
    public Set<LocalizedNode> getAllNodesFiltered(Contexts context) {
        SortedSet<LocalizedNode> allNodes;

        if (context.isApplyGroups()) {
            allNodes = getAllNodes(null, context);
        } else {
            allNodes = new TreeSet<>((SortedSet<LocalizedNode>) getPermissions(true));
        }

        Map<String, String> contexts = new HashMap<>(context.getContext());
        String server = contexts.get("server");
        String world = contexts.get("world");
        contexts.remove("server");
        contexts.remove("world");

        allNodes.removeIf(node ->
                !node.shouldApplyOnServer(server, context.isIncludeGlobal(), plugin.getConfiguration().isApplyingRegex()) ||
                !node.shouldApplyOnWorld(world, context.isIncludeGlobalWorld(), plugin.getConfiguration().isApplyingRegex()) ||
                !node.shouldApplyWithContext(contexts, false)
        );

        Set<LocalizedNode> perms = ConcurrentHashMap.newKeySet();

        all:
        for (LocalizedNode ln : allNodes) {
            // Force higher priority nodes to override
            for (LocalizedNode alreadyIn : perms) {
                if (ln.getNode().getPermission().equals(alreadyIn.getNode().getPermission())) {
                    continue all;
                }
            }

            perms.add(ln);
        }

        return perms;
    }

    /**
     * Converts the output of {@link #getAllNodesFiltered(Contexts)}, and expands wildcards/regex/shorthand perms
     * @param context the context for this request
     * @param possibleNodes a list of possible nodes for wildcards and regex permissions
     * @return a map of permissions
     */
    public Map<String, Boolean> exportNodes(Contexts context, List<String> possibleNodes, boolean lowerCase) {
        Map<String, Boolean> perms = new HashMap<>();

        for (LocalizedNode ln : getAllNodesFiltered(context)) {
            Node node = ln.getNode();
            if (possibleNodes != null && !possibleNodes.isEmpty()) {
                if (node.getPermission().equals("*") || node.getPermission().equals("'*'")) {
                    if (plugin.getConfiguration().isApplyingWildcards()) {
                        possibleNodes.forEach(n -> perms.put(lowerCase ? n.toLowerCase() : n, true));
                    }
                }
            }

            perms.put(lowerCase ? node.getPermission().toLowerCase() : node.getPermission(), node.getValue());

            if (plugin.getConfiguration().isApplyingShorthand()) {
                node.resolveShorthand().stream()
                        .map(s -> lowerCase ? s.toLowerCase() : s)
                        .filter(s -> !perms.containsKey(s))
                        .forEach(s -> perms.put(s, node.getValue()));
            }

            if (possibleNodes != null && !possibleNodes.isEmpty()) {
                if (plugin.getConfiguration().isApplyingWildcards()) {
                    node.resolveWildcard(possibleNodes).stream()
                            .map(s -> lowerCase ? s.toLowerCase() : s)
                            .filter(s -> !perms.containsKey(s))
                            .forEach(s -> perms.put(s, node.getValue()));
                }
            }
        }

        return ImmutableMap.copyOf(perms);
    }

    /**
     * Check if the holder has a permission node
     * @param node the node to check
     * @param t whether to check transient nodes
     * @return a tristate
     */
    public Tristate hasPermission(Node node, boolean t) {
        for (Node n : t ? getTransientNodes() : getNodes()) {
            if (n.almostEquals(node)) {
                return n.getTristate();
            }
        }

        return Tristate.UNDEFINED;
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
     * @return the result of the lookup
     */
    public InheritanceInfo inheritsPermissionInfo(Node node) {
        for (LocalizedNode n : getAllNodes(null, Contexts.allowAll())) {
            if (n.getNode().almostEquals(node)) {
                return InheritanceInfo.of(n);
            }
        }

        return InheritanceInfo.empty();
    }

    /**
     * Check if the holder inherits a node
     * @param node the node to check
     * @return the Tristate result
     */
    public Tristate inheritsPermission(Node node) {
        return inheritsPermissionInfo(node).getResult();
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
        if (hasPermission(node, false) != Tristate.UNDEFINED) {
            throw new ObjectAlreadyHasException();
        }

        synchronized (nodes) {
            nodes.add(node);
            invalidateCache(true);
        }

        plugin.getApiProvider().fireEventAsync(new PermissionNodeSetEvent(new PermissionHolderLink(this), node));
    }

    /**
     * Sets a transient permission node
     * @param node the node to set
     * @throws ObjectAlreadyHasException if the holder has this permission already
     */
    public void setTransientPermission(Node node) throws ObjectAlreadyHasException {
        if (hasPermission(node, true) != Tristate.UNDEFINED) {
            throw new ObjectAlreadyHasException();
        }

        synchronized (transientNodes) {
            transientNodes.add(node);
            invalidateCache(false);
        }

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
        if (hasPermission(node, false) == Tristate.UNDEFINED) {
            throw new ObjectLacksException();
        }

        synchronized (nodes) {
            nodes.removeIf(e -> e.almostEquals(node));
            invalidateCache(true);
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
        if (hasPermission(node, true) == Tristate.UNDEFINED) {
            throw new ObjectLacksException();
        }

        synchronized (transientNodes) {
            transientNodes.removeIf(e -> e.almostEquals(node));
            invalidateCache(false);
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
        return getPermissions(false).stream().filter(Node::isTemporary).collect(Collectors.toSet());
    }

    /**
     * @return The permanent nodes held by the holder
     */
    public Set<Node> getPermanentNodes() {
        return getPermissions(false).stream().filter(Node::isPermanent).collect(Collectors.toSet());
    }

    /**
     * Get a {@link List} of all of the groups the holder inherits, on all servers
     * @return a {@link List} of group names
     */
    public List<String> getGroupNames() {
        return getNodes().stream()
                .filter(Node::isGroupNode)
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    /**
     * Get a {@link List} of the groups the holder inherits on a specific server and world
     * @param server the server to check
     * @param world the world to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server, String world) {
        return getNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnWorld(world, false, true))
                .filter(n -> n.shouldApplyOnServer(server, false, true))
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    /**
     * Get a {@link List} of the groups the holder inherits on a specific server
     * @param server the server to check
     * @return a {@link List} of group names
     */
    public List<String> getLocalGroups(String server) {
        return getNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, false, true))
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    public static Map<String, Boolean> exportToLegacy(Set<Node> nodes) {
        Map<String, Boolean> m = new HashMap<>();
        for (Node node : nodes) {
            m.put(node.toSerializedNode(), node.getValue());
        }
        return m;
    }

    private static Node.Builder buildNode(String permission) {
        return new me.lucko.luckperms.core.Node.Builder(permission);
    }
}
