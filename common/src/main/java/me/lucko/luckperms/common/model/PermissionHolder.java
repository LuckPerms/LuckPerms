/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.model;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.NodeEqualityPredicate;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.caching.HolderCachedData;
import me.lucko.luckperms.common.caching.handlers.StateListener;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.inheritance.InheritanceComparator;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.node.ImmutableLocalizedNode;
import me.lucko.luckperms.common.node.InheritanceInfo;
import me.lucko.luckperms.common.node.MetaType;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeTools;
import me.lucko.luckperms.common.node.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.GroupReference;
import me.lucko.luckperms.common.references.HolderReference;
import me.lucko.luckperms.common.references.HolderType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
 * <p>Data is stored in {@link NodeMap}s. A holder has two of these, one for
 * enduring nodes and one for transient nodes.</p>
 *
 * <p>This class provides a number of methods to perform inheritance lookups.
 * These lookup methods initially use Lists of nodes populated with the
 * inheritance tree. Nodes at the start of this list have priority over nodes at
 * the end. Nodes higher up the tree appear at the end of these lists. In order
 * to remove duplicate elements, the lists are flattened using the methods in
 * {@link NodeTools}. This is significantly faster than trying to prevent
 * duplicates throughout the process of accumulation, and reduces the need for
 * too much caching.</p>
 *
 * <p>Cached state is avoided in these instances to cut down on memory
 * footprint. The nodes are stored indexed to the contexts they apply in, so
 * doing context specific querying should be fast. Caching would be ineffective
 * here, due to the potentially vast amount of contexts being used by nodes,
 * and the potential for very large inheritance trees.</p>
 */
public abstract class PermissionHolder {

    /**
     * The name of this object.
     *
     * <p>Used as a base for identifying permission holding objects. Also acts
     * as a method for preventing circular inheritance issues.</p>
     *
     * @see User#getUuid()
     * @see Group#getName()
     * @see #getObjectName()
     */
    private final String objectName;

    /**
     * Reference to the main plugin instance
     * @see #getPlugin()
     */
    private final LuckPermsPlugin plugin;

    /**
     * The holders persistent nodes.
     *
     * <p>These (unlike transient nodes) are saved to the storage backing.</p>
     *
     * @see #getEnduringData()
     */
    private final NodeMap enduringNodes = new NodeMap(this);

    /**
     * The holders transient nodes.
     *
     * <p>These are nodes which are never stored or persisted to a file, and
     * only last until the end of the objects lifetime. (for a group, that's
     * when the server stops, and for a user, it's when they log out, or get
     * unloaded.)</p>
     *
     * @see #getTransientData()
     */
    private final NodeMap transientNodes = new NodeMap(this);

    /**
     * Caches the holders weight
     * @see #getWeight()
     */
    private final Cache<OptionalInt> weightCache = WeightCache.getFor(this);

    /**
     * Lock used by Storage implementations to prevent concurrent read/writes
     * @see #getIoLock()
     */
    private final Lock ioLock = new ReentrantLock();

    /**
     * Comparator used to ordering groups when calculating inheritance
     */
    private final Comparator<Group> inheritanceComparator = InheritanceComparator.getFor(this);

    /**
     * A set of runnables which are called when this objects state changes.
     */
    private final Set<StateListener> stateListeners = ConcurrentHashMap.newKeySet();

    protected PermissionHolder(String objectName, LuckPermsPlugin plugin) {
        this.objectName = objectName;
        this.plugin = plugin;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public Lock getIoLock() {
        return this.ioLock;
    }

    public Set<StateListener> getStateListeners() {
        return this.stateListeners;
    }

    private void invalidateCache() {
        this.enduringNodes.invalidate();
        this.transientNodes.invalidate();
        this.weightCache.invalidate();

        // Invalidate listeners
        for (StateListener listener : this.stateListeners) {
            try {
                listener.onStateChange();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Declare new state to the state manager
        declareState();
    }

    private void declareState() {
        /* only declare state of groups. the state manager isn't really being used now the caches in this class
           are gone, but it's useful for command output. */
        if (this.getType().isGroup()) {
            this.plugin.getCachedStateManager().putAll(toReference(), getGroupReferences());
        }
    }

    /**
     * Gets the friendly name of this permission holder (for use in commands, etc)
     *
     * @return the holders "friendly" name
     */
    public abstract String getFriendlyName();

    /**
     * Gets the holders cached data
     *
     * @return the holders cached data
     */
    public abstract HolderCachedData<?> getCachedData();

    public abstract BufferedRequest<Void> getRefreshBuffer();

    /**
     * Forms a HolderReference for this PermissionHolder.
     *
     * @return this holders reference
     */
    public abstract HolderReference<?, ?> toReference();

    /**
     * Returns the type of this PermissionHolder.
     *
     * @return this holders type
     */
    public abstract HolderType getType();

    public Comparator<Group> getInheritanceComparator() {
        return this.inheritanceComparator;
    }

    public NodeMap getData(NodeMapType type) {
        switch (type) {
            case ENDURING:
                return this.enduringNodes;
            case TRANSIENT:
                return this.transientNodes;
            default:
                throw new AssertionError();
        }
    }

    public NodeMap getEnduringData() {
        return this.enduringNodes;
    }

    public NodeMap getTransientData() {
        return this.transientNodes;
    }

    public ImmutableSetMultimap<ImmutableContextSet, Node> getEnduringNodes() {
        return this.enduringNodes.immutable();
    }

    public ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes() {
        return this.transientNodes.immutable();
    }

    /**
     * Sets this objects nodes to the values in the set
     *
     * @param set the set of nodes to apply to the object
     */
    public void setEnduringNodes(Set<Node> set) {
        this.enduringNodes.setContent(set);
        invalidateCache();
    }

    /**
     * Replaces the multimap backing this object with another
     *
     * @param multimap the replacement multimap
     */
    public void replaceEnduringNodes(Multimap<ImmutableContextSet, Node> multimap) {
        this.enduringNodes.setContent(multimap);
        invalidateCache();
    }

    public List<Node> getOwnNodes() {
        List<Node> ret = new ArrayList<>();
        this.transientNodes.copyTo(ret);
        this.enduringNodes.copyTo(ret);
        return ret;
    }

    public List<Node> getOwnNodes(ContextSet filter) {
        List<Node> ret = new ArrayList<>();
        this.transientNodes.copyTo(ret, filter);
        this.enduringNodes.copyTo(ret, filter);
        return ret;
    }

    public List<Node> getOwnGroupNodes() {
        List<Node> ret = new ArrayList<>();
        this.transientNodes.copyGroupNodesTo(ret);
        this.enduringNodes.copyGroupNodesTo(ret);
        return ret;
    }

    public List<Node> getOwnGroupNodes(ContextSet filter) {
        List<Node> ret = new ArrayList<>();
        this.transientNodes.copyGroupNodesTo(ret, filter);
        this.enduringNodes.copyGroupNodesTo(ret, filter);
        return ret;
    }

    public SortedSet<LocalizedNode> getOwnNodesSorted() {
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        this.transientNodes.copyToLocalized(ret);
        this.enduringNodes.copyToLocalized(ret);
        return ret;
    }

    public boolean removeIf(Predicate<Node> predicate) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        if (!this.enduringNodes.removeIf(predicate)) {
            return false;
        }
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean removeIfTransient(Predicate<Node> predicate) {
        boolean result = this.transientNodes.removeIf(predicate);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    private void accumulateInheritancesTo(List<LocalizedNode> accumulator, Contexts context) {
        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph(context);
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<Node> nodes = holder.getOwnNodes(context.getContexts());
            for (Node node : nodes) {
                ImmutableLocalizedNode localizedNode = ImmutableLocalizedNode.of(node, holder.getObjectName());
                accumulator.add(localizedNode);
            }
        }
    }

    public List<LocalizedNode> resolveInheritances(Contexts context) {
        List<LocalizedNode> accumulator = new ArrayList<>();
        accumulateInheritancesTo(accumulator, context);
        return accumulator;
    }

    public SortedSet<LocalizedNode> resolveInheritancesAlmostEqual(Contexts contexts) {
        List<LocalizedNode> nodes = new LinkedList<>();
        accumulateInheritancesTo(nodes, contexts);

        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    public SortedSet<LocalizedNode> resolveInheritancesMergeTemp(Contexts contexts) {
        List<LocalizedNode> nodes = new LinkedList<>();
        accumulateInheritancesTo(nodes, contexts);

        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_VALUE_OR_IF_TEMPORARY);
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    private void accumulateInheritancesTo(List<LocalizedNode> accumulator) {
        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph();
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<Node> nodes = holder.getOwnNodes();
            for (Node node : nodes) {
                ImmutableLocalizedNode localizedNode = ImmutableLocalizedNode.of(node, holder.getObjectName());
                accumulator.add(localizedNode);
            }
        }
    }

    public List<LocalizedNode> resolveInheritances() {
        List<LocalizedNode> accumulator = new ArrayList<>();
        accumulateInheritancesTo(accumulator);
        return accumulator;
    }

    public SortedSet<LocalizedNode> resolveInheritancesAlmostEqual() {
        List<LocalizedNode> nodes = new LinkedList<>();
        accumulateInheritancesTo(nodes);

        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    public SortedSet<LocalizedNode> resolveInheritancesMergeTemp() {
        List<LocalizedNode> nodes = new LinkedList<>();
        accumulateInheritancesTo(nodes);

        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_VALUE_OR_IF_TEMPORARY);
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);
        return ret;
    }

    private List<LocalizedNode> getAllEntries(Contexts context) {
        List<LocalizedNode> entries = new LinkedList<>();
        if (context.isApplyGroups()) {
            accumulateInheritancesTo(entries, context);
        } else {
            for (Node n : getOwnNodes(context.getContexts())) {
                ImmutableLocalizedNode localizedNode = ImmutableLocalizedNode.of(n, getObjectName());
                entries.add(localizedNode);
            }
        }

        if (!context.isIncludeGlobal()) {
            entries.removeIf(n -> !n.isGroupNode() && !n.isServerSpecific());
        }
        if (!context.isApplyGlobalWorldGroups()) {
            entries.removeIf(n -> !n.isGroupNode() && !n.isWorldSpecific());
        }

        return entries;
    }

    public SortedSet<LocalizedNode> getAllNodes(Contexts context) {
        List<LocalizedNode> entries = getAllEntries(context);

        NodeTools.removeSamePermission(entries.iterator());
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(entries);
        return ret;
    }

    public Map<String, Boolean> exportNodesAndShorthand(Contexts context, boolean lowerCase) {
        List<LocalizedNode> entries = getAllEntries(context);

        Map<String, Boolean> perms = new HashMap<>();
        boolean applyShorthand = this.plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND);
        for (Node node : entries) {
            String perm = lowerCase ? node.getPermission().toLowerCase() : node.getPermission();

            if (perms.putIfAbsent(perm, node.getValuePrimitive()) == null) {
                if (applyShorthand) {
                    List<String> shorthand = node.resolveShorthand();
                    if (!shorthand.isEmpty()) {
                        for (String s : shorthand) {
                            perms.putIfAbsent(lowerCase ? s.toLowerCase() : s, node.getValuePrimitive());
                        }
                    }
                }
            }
        }

        return ImmutableMap.copyOf(perms);
    }

    public Map<String, Boolean> exportNodesAndShorthand(boolean lowerCase) {
        List<LocalizedNode> entries = resolveInheritances();

        Map<String, Boolean> perms = new HashMap<>();
        boolean applyShorthand = this.plugin.getConfiguration().get(ConfigKeys.APPLYING_SHORTHAND);
        for (Node node : entries) {
            String perm = lowerCase ? node.getPermission().toLowerCase().intern() : node.getPermission();

            if (perms.putIfAbsent(perm, node.getValuePrimitive()) == null && applyShorthand) {
                List<String> shorthand = node.resolveShorthand();
                if (!shorthand.isEmpty()) {
                    for (String s : shorthand) {
                        perms.putIfAbsent((lowerCase ? s.toLowerCase() : s).intern(), node.getValuePrimitive());
                    }
                }
            }
        }

        return ImmutableMap.copyOf(perms);
    }

    public MetaAccumulator accumulateMeta(MetaAccumulator accumulator, Contexts context) {
        if (accumulator == null) {
            accumulator = MetaAccumulator.makeFromConfig(this.plugin);
        }

        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph(context);
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<Node> nodes = holder.getOwnNodes(context.getContexts());
            for (Node node : nodes) {
                if (!node.getValuePrimitive()) continue;
                if (!node.isMeta() && !node.isPrefix() && !node.isSuffix()) continue;

                if (!((context.isIncludeGlobal() || node.isServerSpecific()) && (context.isIncludeGlobalWorld() || node.isWorldSpecific()))) {
                    continue;
                }

                accumulator.accumulateNode(ImmutableLocalizedNode.of(node, holder.getObjectName()));
            }

            OptionalInt w = holder.getWeight();
            if (w.isPresent()) {
                accumulator.accumulateWeight(w.getAsInt());
            }
        }

        return accumulator;
    }

    public MetaAccumulator accumulateMeta(MetaAccumulator accumulator) {
        if (accumulator == null) {
            accumulator = MetaAccumulator.makeFromConfig(this.plugin);
        }

        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph();
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<Node> nodes = holder.getOwnNodes();
            for (Node node : nodes) {
                if (!node.getValuePrimitive()) continue;
                if (!node.isMeta() && !node.isPrefix() && !node.isSuffix()) continue;

                accumulator.accumulateNode(ImmutableLocalizedNode.of(node, holder.getObjectName()));
            }

            OptionalInt w = getWeight();
            if (w.isPresent()) {
                accumulator.accumulateWeight(w.getAsInt());
            }
        }

        return accumulator;
    }

    /**
     * Removes temporary permissions that have expired
     *
     * @return true if permissions had expired and were removed
     */
    public boolean auditTemporaryPermissions() {
        // audit temporary nodes first, but don't track ones which are removed
        // we don't call events for transient nodes
        boolean transientWork = this.transientNodes.auditTemporaryNodes(null);

        ImmutableCollection<Node> before = getEnduringNodes().values();
        Set<Node> removed = new HashSet<>();

        boolean enduringWork = this.enduringNodes.auditTemporaryNodes(removed);
        if (enduringWork) {
            // invalidate
            invalidateCache();

            // call event
            ImmutableCollection<Node> after = getEnduringNodes().values();
            for (Node r : removed) {
                this.plugin.getEventFactory().handleNodeRemove(r, this, before, after);
            }
        }

        if (transientWork && !enduringWork) {
            invalidateCache();
        }

        return transientWork || enduringWork;
    }

    private Optional<Node> searchForMatch(NodeMapType type, Node node, NodeEqualityPredicate equalityPredicate) {
        for (Node n : getData(type).immutable().values()) {
            if (n.equals(node, equalityPredicate)) {
                return Optional.of(n);
            }
        }
        return Optional.empty();
    }

    /**
     * Check if the holder has a permission node
     *
     * @param type which backing map to check
     * @param node the node to check
     * @param equalityPredicate how to match
     * @return a tristate, returns undefined if no match
     */
    public Tristate hasPermission(NodeMapType type, Node node, NodeEqualityPredicate equalityPredicate) {
        if (this.getType().isGroup() && node.isGroupNode() && node.getGroupName().equalsIgnoreCase(getObjectName())) {
            return Tristate.TRUE;
        }

        return searchForMatch(type, node, equalityPredicate).map(Node::getTristate).orElse(Tristate.UNDEFINED);
    }

    public Tristate hasPermission(NodeMapType type, Node node) {
        return hasPermission(type, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    public Tristate hasPermission(Node node, NodeEqualityPredicate equalityPredicate) {
        return hasPermission(NodeMapType.ENDURING, node, equalityPredicate);
    }

    public Tristate hasPermission(Node node) {
        return hasPermission(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    /**
     * Check if the holder inherits a node
     *
     * @param node the node to check
     * @param equalityPredicate how to match
     * @return the result of the lookup
     */
    public InheritanceInfo searchForInheritedMatch(Node node, NodeEqualityPredicate equalityPredicate) {
        for (LocalizedNode n : resolveInheritances()) {
            if (n.getNode().equals(node, equalityPredicate)) {
                return InheritanceInfo.of(n);
            }
        }

        return InheritanceInfo.empty();
    }

    /**
     * Check if the holder inherits a node
     *
     * @param node the node to check
     * @param equalityPredicate how to match
     * @return the Tristate result
     */
    public Tristate inheritsPermission(Node node, NodeEqualityPredicate equalityPredicate) {
        return searchForInheritedMatch(node, equalityPredicate).getResult();
    }

    public Tristate inheritsPermission(Node node) {
        return inheritsPermission(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    /**
     * Sets a permission node
     *
     * @param node the node to set
     */
    public DataMutateResult setPermission(Node node) {
        if (hasPermission(NodeMapType.ENDURING, node) != Tristate.UNDEFINED) {
            return DataMutateResult.ALREADY_HAS;
        }

        ImmutableCollection<Node> before = getEnduringNodes().values();
        this.enduringNodes.add(node);
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeAdd(node, this, before, after);
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
                Optional<Node> existing = searchForMatch(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Create a new node with the same properties, but add the expiry dates together
                    Node newNode = node.toBuilder().setExpiry(previous.getExpiryUnixTime() + node.getSecondsTilExpiry()).build();

                    // Remove the old node & add the new one.
                    ImmutableCollection<Node> before = getEnduringNodes().values();
                    this.enduringNodes.replace(newNode, previous);
                    invalidateCache();
                    ImmutableCollection<Node> after = getEnduringNodes().values();

                    this.plugin.getEventFactory().handleNodeAdd(newNode, this, before, after);
                    return Maps.immutableEntry(DataMutateResult.SUCCESS, newNode);
                }

            } else if (modifier == TemporaryModifier.REPLACE) {
                // Try to replace an existing node
                Optional<Node> existing = searchForMatch(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Only replace if the new expiry time is greater than the old one.
                    if (node.getExpiryUnixTime() > previous.getExpiryUnixTime()) {

                        ImmutableCollection<Node> before = getEnduringNodes().values();
                        this.enduringNodes.replace(node, previous);
                        invalidateCache();
                        ImmutableCollection<Node> after = getEnduringNodes().values();

                        this.plugin.getEventFactory().handleNodeAdd(node, this, before, after);
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
        if (hasPermission(NodeMapType.TRANSIENT, node) != Tristate.UNDEFINED) {
            return DataMutateResult.ALREADY_HAS;
        }

        this.transientNodes.add(node);
        invalidateCache();
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetPermission(Node node) {
        if (hasPermission(NodeMapType.ENDURING, node) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        ImmutableCollection<Node> before = getEnduringNodes().values();
        this.enduringNodes.remove(node);
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeRemove(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a transient permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetTransientPermission(Node node) {
        if (hasPermission(NodeMapType.TRANSIENT, node) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        this.transientNodes.remove(node);
        invalidateCache();
        return DataMutateResult.SUCCESS;
    }

    public boolean inheritsGroup(Group group) {
        return group.getName().equalsIgnoreCase(this.getObjectName()) || hasPermission(NodeFactory.buildGroupNode(group.getName()).build()).asBoolean();
    }

    public boolean inheritsGroup(Group group, ContextSet contextSet) {
        return group.getName().equalsIgnoreCase(this.getObjectName()) || hasPermission(NodeFactory.buildGroupNode(group.getName()).withExtraContext(contextSet).build()).asBoolean();
    }

    /**
     * Clear all of the holders permission nodes
     */
    public boolean clearNodes() {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        this.enduringNodes.clear();
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        if (before.size() == after.size()) {
            return false;
        }

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearNodes(ContextSet contextSet) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        this.enduringNodes.clear(contextSet);
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        if (before.size() == after.size()) {
            return false;
        }

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearParents(boolean giveDefault) {
        ImmutableCollection<Node> before = getEnduringNodes().values();

        if (!this.enduringNodes.removeIf(Node::isGroupNode)) {
            return false;
        }
        if (this.getType().isUser() && giveDefault) {
            this.plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
        }

        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearParents(ContextSet contextSet, boolean giveDefault) {
        ImmutableCollection<Node> before = getEnduringNodes().values();

        if (!this.enduringNodes.removeIf(contextSet, Node::isGroupNode)) {
            return false;
        }
        if (this.getType().isUser() && giveDefault) {
            this.plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
        }

        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearMeta(MetaType type) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        if (!this.enduringNodes.removeIf(type::matches)) {
            return false;
        }
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearMeta(MetaType type, ContextSet contextSet) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        if (!this.enduringNodes.removeIf(contextSet, type::matches)) {
            return false;
        }
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearMetaKeys(String key, boolean temp) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        if (!this.enduringNodes.removeIf(n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key))) {
            return false;
        }
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearMetaKeys(String key, ContextSet contextSet, boolean temp) {
        ImmutableCollection<Node> before = getEnduringNodes().values();
        if (!this.enduringNodes.removeIf(contextSet, n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key))) {
            return false;
        }
        invalidateCache();
        ImmutableCollection<Node> after = getEnduringNodes().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearTransientNodes() {
        this.transientNodes.clear();
        invalidateCache();
        return true;
    }

    public OptionalInt getWeight() {
        return this.weightCache.get();
    }

    public Set<HolderReference> getGroupReferences() {
        return getOwnGroupNodes().stream()
                .map(Node::getGroupName)
                .map(String::toLowerCase)
                .map(GroupReference::of)
                .collect(Collectors.toSet());
    }
}
