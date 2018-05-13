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
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.NodeEqualityPredicate;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.caching.HolderCachedData;
import me.lucko.luckperms.common.caching.type.MetaAccumulator;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.inheritance.InheritanceComparator;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.utils.InheritanceInfo;
import me.lucko.luckperms.common.node.utils.MetaType;
import me.lucko.luckperms.common.node.utils.NodeTools;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

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
     * Reference to the main plugin instance
     * @see #getPlugin()
     */
    private final LuckPermsPlugin plugin;

    /**
     * The holders persistent nodes.
     *
     * <p>These (unlike transient nodes) are saved to the storage backing.</p>
     *
     * @see #enduringData()
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
     * @see #transientData()
     */
    private final NodeMap transientNodes = new NodeMap(this);

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
     * Creates a new instance
     *
     * @param plugin the plugin instance
     */
    protected PermissionHolder(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    // getters

    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    public Lock getIoLock() {
        return this.ioLock;
    }

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

    public NodeMap enduringData() {
        return this.enduringNodes;
    }

    public NodeMap transientData() {
        return this.transientNodes;
    }

    /**
     * Gets the unique name of this holder object.
     *
     * <p>Used as a base for identifying permission holding objects. Also acts
     * as a method for preventing circular inheritance issues.</p>
     *
     * @return the object name
     */
    public abstract String getObjectName();

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

    /**
     * Returns the type of this PermissionHolder.
     *
     * @return this holders type
     */
    public abstract HolderType getType();

    /**
     * Invalidates the holder's cached data.
     */
    public void invalidateCachedData() {
        getCachedData().invalidate();
    }

    protected void invalidateCache() {
        this.enduringNodes.invalidate();
        this.transientNodes.invalidate();

        invalidateCachedData();
        getPlugin().getEventFactory().handleDataRecalculate(this);
    }

    public void setNodes(NodeMapType type, Set<? extends Node> set) {
        getData(type).setContent(set);
        invalidateCache();
    }

    public void replaceNodes(NodeMapType type, Multimap<ImmutableContextSet, ? extends Node> multimap) {
        getData(type).setContent(multimap);
        invalidateCache();
    }

    public List<LocalizedNode> getOwnNodes() {
        List<LocalizedNode> ret = new ArrayList<>();
        this.transientNodes.copyTo(ret);
        this.enduringNodes.copyTo(ret);
        return ret;
    }

    public List<LocalizedNode> getOwnNodes(ContextSet filter) {
        List<LocalizedNode> ret = new ArrayList<>();
        this.transientNodes.copyTo(ret, filter);
        this.enduringNodes.copyTo(ret, filter);
        return ret;
    }

    public List<LocalizedNode> getOwnGroupNodes() {
        List<LocalizedNode> ret = new ArrayList<>();
        this.transientNodes.copyGroupNodesTo(ret);
        this.enduringNodes.copyGroupNodesTo(ret);
        return ret;
    }

    public List<LocalizedNode> getOwnGroupNodes(ContextSet filter) {
        List<LocalizedNode> ret = new ArrayList<>();
        this.transientNodes.copyGroupNodesTo(ret, filter);
        this.enduringNodes.copyGroupNodesTo(ret, filter);
        return ret;
    }

    public SortedSet<LocalizedNode> getOwnNodesSorted() {
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        this.transientNodes.copyTo(ret);
        this.enduringNodes.copyTo(ret);
        return ret;
    }

    public boolean removeIf(Predicate<? super LocalizedNode> predicate) {
        return removeIf(predicate, null);
    }

    public boolean removeIf(Predicate<? super LocalizedNode> predicate, Runnable taskIfSuccess) {
        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        if (!this.enduringNodes.removeIf(predicate)) {
            return false;
        }
        if (taskIfSuccess != null) {
            taskIfSuccess.run();
        }
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean removeIf(ContextSet contextSet, Predicate<? super LocalizedNode> predicate) {
        return removeIf(contextSet, predicate, null);
    }

    public boolean removeIf(ContextSet contextSet, Predicate<? super LocalizedNode> predicate, Runnable taskIfSuccess) {
        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        if (!this.enduringNodes.removeIf(contextSet, predicate)) {
            return false;
        }
        if (taskIfSuccess != null) {
            taskIfSuccess.run();
        }
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean removeIfTransient(Predicate<? super LocalizedNode> predicate) {
        boolean result = this.transientNodes.removeIf(predicate);
        if (result) {
            invalidateCache();
        }
        return result;
    }

    public void accumulateInheritancesTo(List<? super LocalizedNode> accumulator, Contexts context) {
        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph(context);
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<? extends LocalizedNode> nodes = holder.getOwnNodes(context.getContexts());
            accumulator.addAll(nodes);
        }
    }

    public List<LocalizedNode> resolveInheritances(Contexts context) {
        List<LocalizedNode> accumulator = new ArrayList<>();
        accumulateInheritancesTo(accumulator, context);
        return accumulator;
    }

    public void accumulateInheritancesTo(List<? super LocalizedNode> accumulator) {
        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph();
        Iterable<PermissionHolder> traversal = graph.traverse(this.plugin.getConfiguration().get(ConfigKeys.INHERITANCE_TRAVERSAL_ALGORITHM), this);
        for (PermissionHolder holder : traversal) {
            List<? extends LocalizedNode> nodes = holder.getOwnNodes();
            accumulator.addAll(nodes);
        }
    }

    public List<LocalizedNode> resolveInheritances() {
        List<LocalizedNode> accumulator = new ArrayList<>();
        accumulateInheritancesTo(accumulator);
        return accumulator;
    }

    public List<LocalizedNode> getAllEntries(Contexts context) {
        List<LocalizedNode> entries = new LinkedList<>();
        if (context.hasSetting(LookupSetting.RESOLVE_INHERITANCE)) {
            accumulateInheritancesTo(entries, context);
        } else {
            entries.addAll(getOwnNodes(context.getContexts()));
        }

        if (!context.hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER)) {
            entries.removeIf(n -> !n.isGroupNode() && !n.isServerSpecific());
        }
        if (!context.hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD)) {
            entries.removeIf(n -> !n.isGroupNode() && !n.isWorldSpecific());
        }

        return entries;
    }

    public Map<String, Boolean> exportPermissions(Contexts context, boolean convertToLowercase, boolean resolveShorthand) {
        List<LocalizedNode> entries = getAllEntries(context);
        return processExportedPermissions(entries, convertToLowercase, resolveShorthand);
    }

    public Map<String, Boolean> exportPermissions(boolean convertToLowercase, boolean resolveShorthand) {
        List<LocalizedNode> entries = resolveInheritances();
        return processExportedPermissions(entries, convertToLowercase, resolveShorthand);
    }

    private static ImmutableMap<String, Boolean> processExportedPermissions(List<LocalizedNode> entries, boolean convertToLowercase, boolean resolveShorthand) {
        Map<String, Boolean> perms = new HashMap<>(entries.size());
        for (Node node : entries) {
            if (convertToLowercase) {
                perms.putIfAbsent(node.getPermission().toLowerCase(), node.getValue());
            } else {
                perms.putIfAbsent(node.getPermission(), node.getValue());
            }
        }

        if (resolveShorthand) {
            for (Node node : entries) {
                List<String> shorthand = node.resolveShorthand();
                for (String s : shorthand) {
                    if (convertToLowercase) {
                        perms.putIfAbsent(s.toLowerCase(), node.getValue());
                    } else {
                        perms.putIfAbsent(s, node.getValue());
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
            List<? extends LocalizedNode> nodes = holder.getOwnNodes(context.getContexts());
            for (LocalizedNode node : nodes) {
                if (!node.getValue()) continue;
                if (!node.isMeta() && !node.isPrefix() && !node.isSuffix()) continue;

                if (!((context.hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER) || node.isServerSpecific()) && (context.hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD) || node.isWorldSpecific()))) {
                    continue;
                }

                accumulator.accumulateNode(node);
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
            List<? extends LocalizedNode> nodes = holder.getOwnNodes();
            for (LocalizedNode node : nodes) {
                if (!node.getValue()) continue;
                if (!node.isMeta() && !node.isPrefix() && !node.isSuffix()) continue;

                accumulator.accumulateNode(node);
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

        ImmutableCollection<? extends LocalizedNode> before = enduringData().immutable().values();
        Set<Node> removed = new HashSet<>();

        boolean enduringWork = this.enduringNodes.auditTemporaryNodes(removed);
        if (enduringWork) {
            // invalidate
            invalidateCache();

            // call event
            ImmutableCollection<? extends Node> after = enduringData().immutable().values();
            for (Node r : removed) {
                this.plugin.getEventFactory().handleNodeRemove(r, this, before, after);
            }
        }

        if (transientWork && !enduringWork) {
            invalidateCache();
        }

        return transientWork || enduringWork;
    }

    private Optional<LocalizedNode> searchForMatch(NodeMapType type, Node node, NodeEqualityPredicate equalityPredicate) {
        for (LocalizedNode n : getData(type).immutable().values()) {
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

    /**
     * Sets a permission node
     *
     * @param node the node to set
     */
    public DataMutateResult setPermission(Node node) {
        if (hasPermission(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE) != Tristate.UNDEFINED) {
            return DataMutateResult.ALREADY_HAS;
        }

        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        this.enduringNodes.add(node);
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

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
                Optional<? extends Node> existing = searchForMatch(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Create a new node with the same properties, but add the expiry dates together
                    Node newNode = node.toBuilder().setExpiry(previous.getExpiryUnixTime() + node.getSecondsTilExpiry()).build();

                    // Remove the old node & add the new one.
                    ImmutableCollection<? extends Node> before = enduringData().immutable().values();
                    this.enduringNodes.replace(newNode, previous);
                    invalidateCache();
                    ImmutableCollection<? extends Node> after = enduringData().immutable().values();

                    this.plugin.getEventFactory().handleNodeAdd(newNode, this, before, after);
                    return Maps.immutableEntry(DataMutateResult.SUCCESS, newNode);
                }

            } else if (modifier == TemporaryModifier.REPLACE) {
                // Try to replace an existing node
                Optional<? extends Node> existing = searchForMatch(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

                // An existing node was found
                if (existing.isPresent()) {
                    Node previous = existing.get();

                    // Only replace if the new expiry time is greater than the old one.
                    if (node.getExpiryUnixTime() > previous.getExpiryUnixTime()) {

                        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
                        this.enduringNodes.replace(node, previous);
                        invalidateCache();
                        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

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
        if (hasPermission(NodeMapType.TRANSIENT, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE) != Tristate.UNDEFINED) {
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
        if (hasPermission(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        this.enduringNodes.remove(node);
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

        this.plugin.getEventFactory().handleNodeRemove(node, this, before, after);
        return DataMutateResult.SUCCESS;
    }

    /**
     * Unsets a transient permission node
     *
     * @param node the node to unset
     */
    public DataMutateResult unsetTransientPermission(Node node) {
        if (hasPermission(NodeMapType.TRANSIENT, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE) == Tristate.UNDEFINED) {
            return DataMutateResult.LACKS;
        }

        this.transientNodes.remove(node);
        invalidateCache();
        return DataMutateResult.SUCCESS;
    }

    /**
     * Clear all of the holders permission nodes
     */
    public boolean clearNodes() {
        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        this.enduringNodes.clear();
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

        if (before.size() == after.size()) {
            return false;
        }

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearNodes(ContextSet contextSet) {
        ImmutableCollection<? extends Node> before = enduringData().immutable().values();
        this.enduringNodes.clear(contextSet);
        invalidateCache();
        ImmutableCollection<? extends Node> after = enduringData().immutable().values();

        if (before.size() == after.size()) {
            return false;
        }

        this.plugin.getEventFactory().handleNodeClear(this, before, after);
        return true;
    }

    public boolean clearPermissions() {
        return removeIf(node -> !node.hasTypeData());
    }

    public boolean clearPermissions(ContextSet contextSet) {
        return removeIf(contextSet, node -> !node.hasTypeData());
    }

    public boolean clearParents(boolean giveDefault) {
        return removeIf(Node::isGroupNode, () -> {
            if (this.getType().isUser() && giveDefault) {
                this.plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
            }
        });
    }

    public boolean clearParents(ContextSet contextSet, boolean giveDefault) {
        return removeIf(contextSet, Node::isGroupNode, () -> {
            if (this.getType().isUser() && giveDefault) {
                this.plugin.getUserManager().giveDefaultIfNeeded((User) this, false);
            }
        });
    }

    public boolean clearMeta(MetaType type) {
        return removeIf(type::matches);
    }

    public boolean clearMeta(MetaType type, ContextSet contextSet) {
        return removeIf(contextSet, type::matches);
    }

    public boolean clearMetaKeys(String key, boolean temp) {
        return removeIf(n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key));
    }

    public boolean clearMetaKeys(String key, ContextSet contextSet, boolean temp) {
        return removeIf(contextSet, n -> n.isMeta() && (n.isTemporary() == temp) && n.getMeta().getKey().equalsIgnoreCase(key));
    }

    public boolean clearTransientNodes() {
        this.transientNodes.clear();
        invalidateCache();
        return true;
    }

    public OptionalInt getWeight() {
        return OptionalInt.empty();
    }
}
