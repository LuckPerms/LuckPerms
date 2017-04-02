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

package me.lucko.luckperms.common.core.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.api.delegates.PermissionHolderDelegate;
import me.lucko.luckperms.common.caching.MetaAccumulator;
import me.lucko.luckperms.common.caching.handlers.GroupReference;
import me.lucko.luckperms.common.caching.handlers.HolderReference;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.core.ContextSetComparator;
import me.lucko.luckperms.common.core.DataMutateResult;
import me.lucko.luckperms.common.core.InheritanceInfo;
import me.lucko.luckperms.common.core.NodeComparator;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.PriorityComparator;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.common.utils.ImmutableLocalizedNode;
import me.lucko.luckperms.common.utils.NodeTools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents an object that can hold permissions, (a user or group)
 *
 * <p>Permissions are stored in Multimaps, with the context of the node being the key, and the actual Node object being
 * the value. The keys (context sets) are ordered according to their weight {@link ContextSetComparator}, and the values
 * are ordered according to the priority of the node, according to {@link NodeComparator}.</p>
 *
 * <p>This class also provides a number of methods to perform inheritance lookups. These lookup methods initially use
 * Lists of nodes populated with the inheritance tree. Nodes at the start of this list have priority over nodes at the
 * end. Nodes higher up the tree appear at the end of these lists. In order to remove duplicate elements, the lists are
 * flattened using the methods in {@link NodeTools}. This is significantly faster than trying to prevent duplicates
 * throughout the process of accumulation, and reduces the need for too much caching.</p>
 *
 * <p>Cached state is avoided in these instances to cut down on memory footprint. The nodes are stored indexed to the
 * contexts they apply in, so doing context specific querying should be fast. Caching would be ineffective here, due to
 * the potentially vast amount of contexts being used by nodes, and the potential for very large inheritance trees.</p>
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class PermissionHolder {

    /**
     * The name of this PermissionHolder object.
     *
     * <p>Used to prevent circular inheritance issues.</p>
     *
     * <p>For users, this value is a String representation of their {@link User#getUuid()}. For groups, it's just the
     * {@link Group#getName()}.</p>
     */
    @Getter
    private final String objectName;

    /**
     * Reference to the main plugin instance
     */
    @Getter
    private final LuckPermsPlugin plugin;

    /**
     * The holders persistent nodes.
     *
     * <p>These are nodes which are never stored or persisted to a file, and only
     * last until the end of the objects lifetime. (for a group, that's when the server stops, and for a user, it's when
     * they log out, or get unloaded.)</p>
     *
     * <p>Nodes are mapped by the result of {@link Node#getFullContexts()}, and keys are sorted by the weight of the
     * ContextSet. ContextSets are ordered first by the presence of a server key, then by the presence of a world
     * key, and finally by the overall size of the set. Nodes are ordered according to the priority rules
     * defined in {@link NodeComparator}.</p>
     */
    private final SortedSetMultimap<ImmutableContextSet, Node> nodes = MultimapBuilder
            .treeKeys(ContextSetComparator.reverse())
            .treeSetValues(NodeComparator.reverse())
            .build();

    /**
     * The holders transient nodes.
     *
     * <p>These are nodes which are never stored or persisted to a file, and only
     * last until the end of the objects lifetime. (for a group, that's when the server stops, and for a user, it's when
     * they log out, or get unloaded.)</p>
     *
     * <p>Nodes are mapped by the result of {@link Node#getFullContexts()}, and keys are sorted by the weight of the
     * ContextSet. ContextSets are ordered first by the presence of a server key, then by the presence of a world
     * key, and finally by the overall size of the set. Nodes are ordered according to the priority rules
     * defined in {@link NodeComparator}.</p>
     */
    private final SortedSetMultimap<ImmutableContextSet, Node> transientNodes = MultimapBuilder
            .treeKeys(ContextSetComparator.reverse())
            .treeSetValues(NodeComparator.reverse())
            .build();

    /**
     * Lock used by Storage implementations to prevent concurrent read/writes
     */
    @Getter
    private final Lock ioLock = new ReentrantLock();

    /**
     * A set of runnables which are called when this objects state changes.
     */
    @Getter
    private final Set<Runnable> stateListeners = ConcurrentHashMap.newKeySet();

    private void invalidateCache() {
        // Invalidate listeners
        for (Runnable r : stateListeners) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Declare new state to the state manager
        declareState();
    }

    protected void declareState() {
        /* only declare state of groups. the state manager isn't really being used now the caches in this class
           are gone, but it's useful for command output. */
        if (this instanceof Group) {
            plugin.getCachedStateManager().putAll(toReference(), getGroupReferences());
        }
    }

    /**
     * Gets the friendly name of this permission holder (for use in commands, etc)
     *
     * @return the holders "friendly" name
     */
    public abstract String getFriendlyName();

    /**
     * Forms a HolderReference for this PermissionHolder.
     *
     * @return this holders reference
     */
    public abstract HolderReference<?> toReference();

    /**
     * Gets the API delegate for this instance
     *
     * @return the api delegate
     */
    public abstract PermissionHolderDelegate getDelegate();

    /**
     * Returns an immutable copy of this objects nodes
     *
     * @return an immutable copy of the multimap storing this objects nodes
     */
    public ImmutableSetMultimap<ImmutableContextSet, Node> getNodes() {
        synchronized (nodes) {
            return ImmutableSetMultimap.copyOf(nodes);
        }
    }

    /**
     * Returns an immutable copy of this objects transient nodes
     *
     * @return an immutable copy of the multimap storing this objects transient nodes
     */
    public ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes() {
        synchronized (transientNodes) {
            return ImmutableSetMultimap.copyOf(transientNodes);
        }
    }

    /**
     * Sets this objects nodes to the values in the set
     *
     * @param set the set of nodes to apply to the object
     */
    public void setNodes(Set<Node> set) {
        synchronized (nodes) {
            nodes.clear();
            for (Node n : set) {
                nodes.put(n.getFullContexts().makeImmutable(), n);
            }
        }
        invalidateCache();
    }

    /**
     * Replaces the multimap backing this object with another
     *
     * @param multimap the replacement multimap
     */
    public void replaceNodes(Multimap<ImmutableContextSet, Node> multimap) {
        synchronized (nodes) {
            nodes.clear();
            nodes.putAll(multimap);
        }
        invalidateCache();
    }

    public void setTransientNodes(Set<Node> set) {
        synchronized (transientNodes) {
            transientNodes.clear();
            for (Node n : set) {
                transientNodes.put(n.getFullContexts().makeImmutable(), n);
            }
        }
        invalidateCache();
    }

    public void replaceTransientNodes(Multimap<ImmutableContextSet, Node> multimap) {
        synchronized (transientNodes) {
            transientNodes.clear();
            transientNodes.putAll(multimap);
        }
        invalidateCache();
    }

    /**
     * Merges enduring and transient permissions into one set
     *
     * @return a set containing the holders enduring and transient permissions
     */
    public LinkedHashSet<Node> mergePermissions() {
        LinkedHashSet<Node> ret = new LinkedHashSet<>();
        synchronized (transientNodes) {
            ret.addAll(transientNodes.values());
        }

        synchronized (nodes) {
            ret.addAll(nodes.values());
        }

        return ret;
    }

    public List<Node> mergePermissionsToList() {
        List<Node> ret = new ArrayList<>();
        synchronized (transientNodes) {
            ret.addAll(transientNodes.values());
        }
        synchronized (nodes) {
            ret.addAll(nodes.values());
        }

        return ret;
    }

    public SortedSet<LocalizedNode> mergePermissionsToSortedSet() {
        SortedSet<LocalizedNode> ret = new TreeSet<>(PriorityComparator.reverse());
        ret.addAll(mergePermissions().stream().map(n -> ImmutableLocalizedNode.of(n, getObjectName())).collect(Collectors.toSet()));
        return ret;
    }

    public LinkedHashSet<Node> flattenNodes() {
        synchronized (nodes) {
            return new LinkedHashSet<>(nodes.values());
        }
    }

    public LinkedHashSet<Node> flattenNodes(ContextSet filter) {
        synchronized (nodes) {
            LinkedHashSet<Node> set = new LinkedHashSet<>();
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : nodes.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    set.addAll(e.getValue());
                }
            }

            return set;
        }
    }

    public LinkedHashSet<Node> flattenTransientNodes() {
        synchronized (transientNodes) {
            return new LinkedHashSet<>(transientNodes.values());
        }
    }

    public LinkedHashSet<Node> flattenTransientNodes(ContextSet filter) {
        synchronized (transientNodes) {
            LinkedHashSet<Node> set = new LinkedHashSet<>();
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : transientNodes.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    set.addAll(e.getValue());
                }
            }

            return set;
        }
    }

    public List<Node> flattenNodesToList() {
        synchronized (nodes) {
             return new ArrayList<>(nodes.values());
        }
    }

    public List<Node> flattenNodesToList(ContextSet filter) {
        synchronized (nodes) {
            List<Node> set = new ArrayList<>();
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : nodes.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    set.addAll(e.getValue());
                }
            }

            return set;
        }
    }

    public List<Node> flattenTransientNodesToList() {
        synchronized (transientNodes) {
            return new ArrayList<>(transientNodes.values());
        }
    }

    public List<Node> flattenTransientNodesToList(ContextSet filter) {
        synchronized (transientNodes) {
            if (transientNodes.isEmpty()) {
                return Collections.emptyList();
            }

            List<Node> set = new ArrayList<>();
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : transientNodes.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    set.addAll(e.getValue());
                }
            }

            return set;
        }
    }

    public boolean removeIf(Predicate<Node> predicate) {
        boolean result;
        ImmutableSet<Node> before = ImmutableSet.copyOf(flattenNodes());

        synchronized (nodes) {
            result = nodes.values().removeIf(predicate);
        }

        if (!result) {
            return false;
        }

        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(flattenNodes());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean removeIfTransient(Predicate<Node> predicate) {
        boolean result;

        synchronized (nodes) {
            result = transientNodes.values().removeIf(predicate);
        }

        if (result) {
            invalidateCache();
        }

        return result;
    }

    /**
     * Resolves inherited nodes and returns them
     *
     * @param excludedGroups a list of groups to exclude
     * @param context       context to decide if groups should be applied
     * @return a set of nodes
     */
    protected List<LocalizedNode> resolveInheritances(List<LocalizedNode> accumulator, Set<String> excludedGroups, ExtractedContexts context) {
        if (accumulator == null) {
            accumulator = new ArrayList<>();
        }

        if (excludedGroups == null) {
            excludedGroups = new HashSet<>();
        }

        if (this instanceof Group) {
            excludedGroups.add(getObjectName().toLowerCase());
        }

        // get the objects own nodes
        flattenTransientNodesToList(context.getContextSet()).stream()
                .map(n -> ImmutableLocalizedNode.of(n, getObjectName()))
                .forEach(accumulator::add);

        flattenNodesToList(context.getContextSet()).stream()
                .map(n -> ImmutableLocalizedNode.of(n, getObjectName()))
                .forEach(accumulator::add);

        Contexts contexts = context.getContexts();
        String server = context.getServer();
        String world = context.getWorld();

        // screw effectively final
        Set<String> finalExcludedGroups = excludedGroups;
        List<LocalizedNode> finalAccumulator = accumulator;
        mergePermissions().stream()
                .filter(Node::getValue)
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, contexts.isApplyGlobalGroups(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)))
                .filter(n -> n.shouldApplyOnWorld(world, contexts.isApplyGlobalWorldGroups(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)))
                .filter(n -> n.shouldApplyWithContext(contexts.getContexts(), false))
                .map(Node::getGroupName)
                .distinct()
                .map(n -> Optional.ofNullable(plugin.getGroupManager().getIfLoaded(n)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(g -> !finalExcludedGroups.contains(g.getObjectName().toLowerCase()))
                .sorted((o1, o2) -> {
                    int result = Integer.compare(o1.getWeight().orElse(0), o2.getWeight().orElse(0));
                    return result == 1 ? -1 : 1;
                })
                .forEach(group -> group.resolveInheritances(finalAccumulator, finalExcludedGroups, context));

        return accumulator;
    }

    public List<LocalizedNode> resolveInheritances(ExtractedContexts context) {
        return resolveInheritances(null, null, context);
    }

    public SortedSet<LocalizedNode> resolveInheritancesAlmostEqual(ExtractedContexts contexts) {
        List<LocalizedNode> nodes = resolveInheritances(null, null, contexts);
        NodeTools.removeAlmostEqual(nodes.iterator());
        SortedSet<LocalizedNode> ret = new TreeSet<>(PriorityComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    public SortedSet<LocalizedNode> resolveInheritancesMergeTemp(ExtractedContexts contexts) {
        List<LocalizedNode> nodes = resolveInheritances(null, null, contexts);
        NodeTools.removeIgnoreValueOrTemp(nodes.iterator());
        SortedSet<LocalizedNode> ret = new TreeSet<>(PriorityComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    public SortedSet<LocalizedNode> getAllNodes(ExtractedContexts context) {
        Contexts contexts = context.getContexts();
        String server = context.getServer();
        String world = context.getWorld();

        List<LocalizedNode> entries;
        if (contexts.isApplyGroups()) {
            entries = resolveInheritances(null, null, context);
        } else {
            entries = flattenNodesToList(context.getContextSet()).stream().map(n -> ImmutableLocalizedNode.of(n, getObjectName())).collect(Collectors.toList());
        }

        entries.removeIf(node ->
                !node.isGroupNode() && (
                        !node.shouldApplyOnServer(server, contexts.isIncludeGlobal(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) ||
                        !node.shouldApplyOnWorld(world, contexts.isIncludeGlobalWorld(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) ||
                        !node.shouldApplyWithContext(context.getContextSet(), false)
                )
        );

        NodeTools.removeSamePermission(entries.iterator());
        SortedSet<LocalizedNode> ret = new TreeSet<>(PriorityComparator.reverse());
        ret.addAll(entries);
        return ret;
    }

    public Map<String, Boolean> exportNodes(ExtractedContexts context, boolean lowerCase) {
        Contexts contexts = context.getContexts();
        String server = context.getServer();
        String world = context.getWorld();

        List<? extends Node> entries;
        if (contexts.isApplyGroups()) {
            entries = resolveInheritances(null, null, context);
        } else {
            entries = flattenNodesToList(context.getContextSet());
        }

        entries.removeIf(node ->
                !node.isGroupNode() && (
                        !node.shouldApplyOnServer(server, contexts.isIncludeGlobal(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) ||
                        !node.shouldApplyOnWorld(world, contexts.isIncludeGlobalWorld(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)) ||
                        !node.shouldApplyWithContext(context.getContextSet(), false)
                )
        );

        Map<String, Boolean> perms = new HashMap<>();

        for (Node node : entries) {
            String perm = lowerCase ? node.getPermission().toLowerCase() : node.getPermission();
            if (!perms.containsKey(perm)) {
                perms.put(perm, node.getValue());

                if (plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND)) {
                    List<String> sh = node.resolveShorthand();
                    if (!sh.isEmpty()) {
                        sh.stream().map(s -> lowerCase ? s.toLowerCase() : s)
                                .filter(s -> !perms.containsKey(s))
                                .forEach(s -> perms.put(s, node.getValue()));
                    }
                }
            }
        }

        return ImmutableMap.copyOf(perms);
    }

    public MetaAccumulator accumulateMeta(MetaAccumulator accumulator, Set<String> excludedGroups, ExtractedContexts context) {
        if (accumulator == null) {
            accumulator = new MetaAccumulator(
                    plugin.getConfiguration().get(ConfigKeys.PREFIX_FORMATTING_OPTIONS).copy(),
                    plugin.getConfiguration().get(ConfigKeys.SUFFIX_FORMATTING_OPTIONS).copy()
            );
        }

        if (excludedGroups == null) {
            excludedGroups = new HashSet<>();
        }

        if (this instanceof Group) {
            excludedGroups.add(getObjectName().toLowerCase());
        }

        Contexts contexts = context.getContexts();
        String server = context.getServer();
        String world = context.getWorld();

        // screw effectively final
        Set<String> finalExcludedGroups = excludedGroups;
        MetaAccumulator finalAccumulator = accumulator;

        flattenTransientNodesToList(context.getContextSet()).stream()
                .filter(Node::getValue)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .filter(n -> n.shouldApplyOnServer(server, contexts.isIncludeGlobal(), false))
                .filter(n -> n.shouldApplyOnWorld(world, contexts.isIncludeGlobalWorld(), false))
                .filter(n -> n.shouldApplyWithContext(context.getContextSet(), false))
                .forEach(n -> finalAccumulator.accumulateNode(ImmutableLocalizedNode.of(n, getObjectName())));

        flattenNodesToList(context.getContextSet()).stream()
                .filter(Node::getValue)
                .filter(n -> n.isMeta() || n.isPrefix() || n.isSuffix())
                .filter(n -> n.shouldApplyOnServer(server, contexts.isIncludeGlobal(), false))
                .filter(n -> n.shouldApplyOnWorld(world, contexts.isIncludeGlobalWorld(), false))
                .filter(n -> n.shouldApplyWithContext(context.getContextSet(), false))
                .forEach(n -> finalAccumulator.accumulateNode(ImmutableLocalizedNode.of(n, getObjectName())));

        OptionalInt w = getWeight();
        if (w.isPresent()) {
            accumulator.accumulateWeight(w.getAsInt());
        }

        mergePermissions().stream()
                .filter(Node::getValue)
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, contexts.isApplyGlobalGroups(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)))
                .filter(n -> n.shouldApplyOnWorld(world, contexts.isApplyGlobalWorldGroups(), plugin.getConfiguration().get(ConfigKeys.APPLYING_REGEX)))
                .filter(n -> n.shouldApplyWithContext(contexts.getContexts(), false))
                .map(Node::getGroupName)
                .distinct()
                .map(n -> Optional.ofNullable(plugin.getGroupManager().getIfLoaded(n)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(g -> !finalExcludedGroups.contains(g.getObjectName().toLowerCase()))
                .sorted((o1, o2) -> {
                    int result = Integer.compare(o1.getWeight().orElse(0), o2.getWeight().orElse(0));
                    return result == 1 ? -1 : 1;
                })
                .forEach(group -> group.accumulateMeta(finalAccumulator, finalExcludedGroups, context));

        return accumulator;
    }

    /**
     * Removes temporary permissions that have expired
     *
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryPermissions() {
        boolean work = false;
        Set<Node> removed = new HashSet<>();

        ImmutableSet<Node> before = ImmutableSet.copyOf(mergePermissions());

        synchronized (nodes) {
            Iterator<Node> it = nodes.values().iterator();
            while (it.hasNext()) {
                Node entry = it.next();
                if (entry.hasExpired()) {
                    removed.add(entry);
                    work = true;
                    it.remove();
                }
            }
        }

        if (work) {
            invalidateCache();
            work = false;
        }

        synchronized (transientNodes) {
            Iterator<Node> it = transientNodes.values().iterator();
            while (it.hasNext()) {
                Node entry = it.next();
                if (entry.hasExpired()) {
                    removed.add(entry);
                    work = true;
                    it.remove();
                }
            }
        }

        if (work) {
            invalidateCache();
        }

        if (removed.isEmpty()) {
            return false;
        }

        ImmutableSet<Node> after = ImmutableSet.copyOf(mergePermissions());

        for (Node r : removed) {
            plugin.getApiProvider().getEventFactory().handleNodeRemove(r, this, before, after);
        }

        return true;
    }

    public Optional<Node> getAlmostEquals(Node node, boolean t) {
        for (Node n : t ? getTransientNodes().values() : getNodes().values()) {
            if (n.almostEquals(node)) {
                return Optional.of(n);
            }
        }

        return Optional.empty();
    }

    /**
     * Check if the holder has a permission node
     *
     * @param node the node to check
     * @param checkTransient    whether to check transient nodes
     * @return a tristate
     */
    public Tristate hasPermission(Node node, boolean checkTransient) {
        if (this instanceof Group && node.isGroupNode() && node.getGroupName().equalsIgnoreCase(getObjectName())) {
            return Tristate.TRUE;
        }

        return getAlmostEquals(node, checkTransient).map(Node::getTristate).orElse(Tristate.UNDEFINED);
    }

    public Tristate hasPermission(Node node) {
        return hasPermission(node, false);
    }

    public boolean hasPermission(String node, boolean value) {
        return hasPermission(NodeFactory.make(node, value)).asBoolean() == value;
    }

    public boolean hasPermission(String node, boolean value, String server) {
        return hasPermission(NodeFactory.make(node, value, server)).asBoolean() == value;
    }

    public boolean hasPermission(String node, boolean value, String server, String world) {
        return hasPermission(NodeFactory.make(node, value, server, world)).asBoolean() == value;
    }

    public boolean hasPermission(String node, boolean value, boolean temporary) {
        return hasPermission(NodeFactory.make(node, value, temporary)).asBoolean() == value;
    }

    public boolean hasPermission(String node, boolean value, String server, boolean temporary) {
        return hasPermission(NodeFactory.make(node, value, server, temporary)).asBoolean() == value;
    }

    public boolean hasPermission(String node, boolean value, String server, String world, boolean temporary) {
        return hasPermission(NodeFactory.make(node, value, server, world, temporary)).asBoolean() == value;
    }

    /**
     * Check if the holder inherits a node
     *
     * @param node the node to check
     * @return the result of the lookup
     */
    public InheritanceInfo inheritsPermissionInfo(Node node) {
        for (LocalizedNode n : resolveInheritances(ExtractedContexts.generate(Contexts.allowAll()))) {
            if (n.getNode().almostEquals(node)) {
                return InheritanceInfo.of(n);
            }
        }

        return InheritanceInfo.empty();
    }

    /**
     * Check if the holder inherits a node
     *
     * @param node the node to check
     * @return the Tristate result
     */
    public Tristate inheritsPermission(Node node) {
        return inheritsPermissionInfo(node).getResult();
    }

    public boolean inheritsPermission(String node, boolean value) {
        return inheritsPermission(NodeFactory.make(node, value)).asBoolean() == value;
    }

    public boolean inheritsPermission(String node, boolean value, String server) {
        return inheritsPermission(NodeFactory.make(node, value, server)).asBoolean() == value;
    }

    public boolean inheritsPermission(String node, boolean value, String server, String world) {
        return inheritsPermission(NodeFactory.make(node, value, server, world)).asBoolean() == value;
    }

    public boolean inheritsPermission(String node, boolean value, boolean temporary) {
        return inheritsPermission(NodeFactory.make(node, value, temporary)).asBoolean() == value;
    }

    public boolean inheritsPermission(String node, boolean value, String server, boolean temporary) {
        return inheritsPermission(NodeFactory.make(node, value, server, temporary)).asBoolean() == value;
    }

    public boolean inheritsPermission(String node, boolean value, String server, String world, boolean temporary) {
        return inheritsPermission(NodeFactory.make(node, value, server, world, temporary)).asBoolean() == value;
    }

    /**
     * Sets a permission node
     *
     * @param node the node to set
     */
    public DataMutateResult setPermission(Node node) {
        if (hasPermission(node, false) != Tristate.UNDEFINED) {
            return DataMutateResult.ALREADY_HAS;
        }

        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

        synchronized (nodes) {
            nodes.put(node.getFullContexts().makeImmutable(), node);
        }
        invalidateCache();

        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());

        plugin.getApiProvider().getEventFactory().handleNodeAdd(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Sets a permission node, applying a temporary modifier if the node is temporary.
     * @param node the node to set
     * @param modifier the modifier to use for the operation
     * @return the node that was actually set, respective of the modifier
     */
    public Map.Entry<DataMutateResult, Node> setPermission(Node node, TemporaryModifier modifier) {
        // If the node is temporary, we should take note of the modifier
        if (node.isTemporary()) {
            if (modifier == TemporaryModifier.ACCUMULATE) {
                // Try to accumulate with an existing node
                Optional<Node> existing = getAlmostEquals(node, false);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Create a new node with the same properties, but add the expiry dates together
                    Node newNode = NodeFactory.builderFromExisting(node).setExpiry(previous.getExpiryUnixTime() + node.getSecondsTilExpiry()).build();

                    ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

                    // Remove the old node & add the new one.
                    synchronized (nodes) {
                        nodes.remove(previous.getContexts().makeImmutable(), previous);
                        nodes.put(newNode.getFullContexts().makeImmutable(), newNode);
                    }

                    invalidateCache();
                    ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
                    plugin.getApiProvider().getEventFactory().handleNodeAdd(newNode, this, before, after);
                    return Maps.immutableEntry(DataMutateResult.SUCCESS, newNode);
                }

            } else if (modifier == TemporaryModifier.REPLACE) {
                // Try to replace an existing node
                Optional<Node> existing = getAlmostEquals(node, false);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Only replace if the new expiry time is greater than the old one.
                    if (node.getExpiryUnixTime() > previous.getExpiryUnixTime()) {

                        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

                        synchronized (nodes) {
                            nodes.remove(previous.getFullContexts().makeImmutable(), previous);
                            nodes.put(node.getFullContexts().makeImmutable(), node);
                        }

                        invalidateCache();
                        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
                        plugin.getApiProvider().getEventFactory().handleNodeAdd(node, this, before, after);
                        return Maps.immutableEntry(DataMutateResult.SUCCESS, node);
                    }
                }
            }

            // DENY behaviour is the default anyways.
        }

        // Fallback to the normal handling.
        return Maps.immutableEntry(setPermission(node), node);
    }

    /**
     * Sets a transient permission node
     *
     * @param node the node to set
     */
    public DataMutateResult setTransientPermission(Node node) {
        if (hasPermission(node, true) != Tristate.UNDEFINED) {
            return DataMutateResult.ALREADY_HAS;
        }

        ImmutableSet<Node> before = ImmutableSet.copyOf(getTransientNodes().values());

        synchronized (transientNodes) {
            transientNodes.put(node.getFullContexts().makeImmutable(), node);
        }
        invalidateCache();

        ImmutableSet<Node> after = ImmutableSet.copyOf(getTransientNodes().values());

        plugin.getApiProvider().getEventFactory().handleNodeAdd(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetPermission(Node node) {
        if (hasPermission(node, false) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

        synchronized (nodes) {
            this.nodes.get(node.getFullContexts().makeImmutable()).removeIf(e -> e.almostEquals(node));
        }
        invalidateCache();

        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeRemove(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetPermissionExact(Node node) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

        synchronized (nodes) {
            nodes.get(node.getFullContexts().makeImmutable()).removeIf(e -> e.equals(node));
        }
        invalidateCache();

        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());

        if (before.size() == after.size()) {
            return DataMutateResult.LACKS;
        }

        plugin.getApiProvider().getEventFactory().handleNodeRemove(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a transient permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetTransientPermission(Node node) {
        if (hasPermission(node, true) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        ImmutableSet<Node> before = ImmutableSet.copyOf(getTransientNodes().values());

        synchronized (transientNodes) {
            transientNodes.get(node.getFullContexts().makeImmutable()).removeIf(e -> e.almostEquals(node));
        }
        invalidateCache();

        ImmutableSet<Node> after = ImmutableSet.copyOf(getTransientNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeRemove(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    public boolean inheritsGroup(Group group) {
        return group.getName().equalsIgnoreCase(this.getObjectName()) || hasPermission("group." + group.getName(), true);
    }

    public boolean inheritsGroup(Group group, String server) {
        return group.getName().equalsIgnoreCase(this.getObjectName()) || hasPermission("group." + group.getName(), true, server);
    }

    public boolean inheritsGroup(Group group, String server, String world) {
        return group.getName().equalsIgnoreCase(this.getObjectName()) || hasPermission("group." + group.getName(), true, server, world);
    }

    public DataMutateResult setInheritGroup(Group group, ContextSet contexts) {
        return setPermission(NodeFactory.newBuilder("group." + group.getName()).withExtraContext(contexts).build());
    }

    public DataMutateResult unsetInheritGroup(Group group, ContextSet contexts) {
        return unsetPermission(NodeFactory.newBuilder("group." + group.getName()).withExtraContext(contexts).build());
    }

    /**
     * Clear all of the holders permission nodes
     */
    public void clearNodes() {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {
            nodes.clear();
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearNodes(ContextSet contextSet) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {
            nodes.removeAll(contextSet.makeImmutable());
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearParents(boolean giveDefault) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

        synchronized (nodes) {
            boolean b = nodes.values().removeIf(Node::isGroupNode);
            if (!b) {
                return;
            }
        }
        if (this instanceof User && giveDefault) {
            plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearParents(ContextSet contextSet, boolean giveDefault) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {
            SortedSet<Node> nodes = this.nodes.get(contextSet.makeImmutable());
            if (nodes == null) {
                return;
            }

            boolean b = nodes.removeIf(Node::isGroupNode);
            if (!b) {
                return;
            }
        }
        if (this instanceof User && giveDefault) {
            plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearMeta() {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());

        synchronized (nodes) {
            if (!nodes.values().removeIf(n -> n.isMeta() || n.isPrefix() || n.isSuffix())) {
                return;
            }
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearMeta(ContextSet contextSet) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {
            SortedSet<Node> nodes = this.nodes.get(contextSet.makeImmutable());
            if (nodes == null) {
                return;
            }

            boolean b = nodes.removeIf(n -> n.isMeta() || n.isPrefix() || n.isSuffix());
            if (!b) {
                return;
            }
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearMetaKeys(String key, boolean temp) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {
            boolean b = this.nodes.values().removeIf(n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key));
            if (!b) {
                return;
            }
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearMetaKeys(String key, ContextSet contextSet, boolean temp) {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getNodes().values());
        synchronized (nodes) {

            SortedSet<Node> nodes = this.nodes.get(contextSet.makeImmutable());
            if (nodes == null) {
                return;
            }

            boolean b = nodes.removeIf(n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key));
            if (!b) {
                return;
            }
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    public void clearTransientNodes() {
        ImmutableSet<Node> before = ImmutableSet.copyOf(getTransientNodes().values());

        synchronized (transientNodes) {
            transientNodes.clear();
        }
        invalidateCache();
        ImmutableSet<Node> after = ImmutableSet.copyOf(getTransientNodes().values());
        plugin.getApiProvider().getEventFactory().handleNodeClear(this, before, after);
    }

    /**
     * @return The temporary nodes held by the holder
     */
    public Set<Node> getTemporaryNodes() {
        return mergePermissionsToList().stream().filter(Node::isTemporary).collect(Collectors.toSet());
    }

    /**
     * @return The permanent nodes held by the holder
     */
    public Set<Node> getPermanentNodes() {
        return mergePermissionsToList().stream().filter(Node::isPermanent).collect(Collectors.toSet());
    }

    public Set<Node> getPrefixNodes() {
        return mergePermissionsToList().stream().filter(Node::isPrefix).collect(Collectors.toSet());
    }

    public Set<Node> getSuffixNodes() {
        return mergePermissionsToList().stream().filter(Node::isSuffix).collect(Collectors.toSet());
    }

    public Set<Node> getMetaNodes() {
        return mergePermissionsToList().stream().filter(Node::isMeta).collect(Collectors.toSet());
    }

    public OptionalInt getWeight() {
        if (this instanceof User) return OptionalInt.empty();

        OptionalInt weight = OptionalInt.empty();
        try {
            weight = mergePermissionsToList().stream()
                    .filter(n -> n.getPermission().startsWith("weight."))
                    .map(n -> n.getPermission().substring("weight.".length()))
                    .mapToInt(Integer::parseInt)
                    .max();
        } catch (Exception ignored) {}

        if (!weight.isPresent()) {
            Integer w = plugin.getConfiguration().get(ConfigKeys.GROUP_WEIGHTS).get(getObjectName().toLowerCase());
            if (w != null) {
                weight = OptionalInt.of(w);
            }
        }

        return weight;
    }

    public Set<HolderReference> getGroupReferences() {
        return mergePermissionsToList().stream()
                .filter(Node::isGroupNode)
                .map(Node::getGroupName)
                .map(String::toLowerCase)
                .map(GroupReference::of)
                .collect(Collectors.toSet());
    }
}
