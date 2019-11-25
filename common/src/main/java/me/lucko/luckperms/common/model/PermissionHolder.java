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
import com.google.common.collect.Multimap;

import me.lucko.luckperms.common.cacheddata.HolderCachedDataManager;
import me.lucko.luckperms.common.cacheddata.type.MetaAccumulator;
import me.lucko.luckperms.common.inheritance.InheritanceComparator;
import me.lucko.luckperms.common.inheritance.InheritanceGraph;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.dataorder.DataQueryOrder;
import net.luckperms.api.query.dataorder.DataQueryOrderFunction;
import net.luckperms.api.util.Tristate;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * to remove duplicate elements, the lists are flattened. This is significantly
 * faster than trying to prevent duplicates throughout the process of accumulation,
 * and reduces the need for too much caching.</p>
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
     * The holders identifier
     */
    private @MonotonicNonNull PermissionHolderIdentifier identifier;

    /**
     * The holders persistent nodes.
     *
     * <p>These (unlike transient nodes) are saved to the storage backing.</p>
     *
     * @see #normalData()
     */
    private final NodeMap normalNodes = new NodeMap(this);

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
    private final Comparator<? super PermissionHolder> inheritanceComparator = InheritanceComparator.getFor(this);

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

    public Comparator<? super PermissionHolder> getInheritanceComparator() {
        return this.inheritanceComparator;
    }

    public NodeMap getData(DataType type) {
        switch (type) {
            case NORMAL:
                return this.normalNodes;
            case TRANSIENT:
                return this.transientNodes;
            default:
                throw new AssertionError();
        }
    }

    public NodeMap normalData() {
        return this.normalNodes;
    }

    public NodeMap transientData() {
        return this.transientNodes;
    }

    public PermissionHolderIdentifier getIdentifier() {
        if (this.identifier == null) {
            this.identifier = new PermissionHolderIdentifier(getType(), getObjectName());
        }
        return this.identifier;
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
     * Gets the formatted display name of this permission holder
     * (for use in commands, etc)
     *
     * @return the holders formatted display name
     */
    public abstract String getFormattedDisplayName();

    /**
     * Gets a display name for this permission holder, without any formatting.
     *
     * @return the display name
     */
    public abstract String getPlainDisplayName();

    /**
     * Gets the holders cached data
     *
     * @return the holders cached data
     */
    public abstract HolderCachedDataManager<?> getCachedData();

    /**
     * Returns the type of this PermissionHolder.
     *
     * @return this holders type
     */
    public abstract HolderType getType();

    protected void invalidateCache() {
        this.normalNodes.invalidate();
        this.transientNodes.invalidate();

        getCachedData().invalidate();
        getPlugin().getEventFactory().handleDataRecalculate(this);
    }

    public void setNodes(DataType type, Collection<? extends Node> set) {
        getData(type).setContent(set);
        invalidateCache();
    }

    public void replaceNodes(DataType type, Multimap<ImmutableContextSet, ? extends Node> multimap) {
        getData(type).setContent(multimap);
        invalidateCache();
    }

    public List<Node> getOwnNodes(QueryOptions queryOptions) {
        List<Node> ret = new ArrayList<>();

        Comparator<DataType> comparator = queryOptions.option(DataQueryOrderFunction.KEY)
                .map(func -> func.getOrderComparator(getIdentifier()))
                .orElse(DataQueryOrder.TRANSIENT_FIRST);

        for (DataType dataType : DataQueryOrder.order(comparator)) {
            getData(dataType).copyTo(ret, queryOptions);
        }

        return ret;
    }

    public SortedSet<Node> getOwnNodesSorted(QueryOptions queryOptions) {
        SortedSet<Node> ret = new TreeSet<>(NodeWithContextComparator.reverse());

        Comparator<DataType> comparator = queryOptions.option(DataQueryOrderFunction.KEY)
                .map(func -> func.getOrderComparator(getIdentifier()))
                .orElse(DataQueryOrder.TRANSIENT_FIRST);

        for (DataType dataType : DataQueryOrder.order(comparator)) {
            getData(dataType).copyTo(ret, queryOptions);
        }

        return ret;
    }

    public List<InheritanceNode> getOwnInheritanceNodes(QueryOptions queryOptions) {
        List<InheritanceNode> ret = new ArrayList<>();

        Comparator<DataType> comparator = queryOptions.option(DataQueryOrderFunction.KEY)
                .map(func -> func.getOrderComparator(getIdentifier()))
                .orElse(DataQueryOrder.TRANSIENT_FIRST);

        for (DataType dataType : DataQueryOrder.order(comparator)) {
            getData(dataType).copyInheritanceNodesTo(ret, queryOptions);
        }

        return ret;
    }

    private void accumulateInheritedNodesTo(Collection<Node> accumulator, QueryOptions queryOptions) {
        if (queryOptions.flag(Flag.RESOLVE_INHERITANCE)) {
            InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph(queryOptions);
            Iterable<PermissionHolder> traversal = graph.traverse(this);
            for (PermissionHolder holder : traversal) {
                List<? extends Node> nodes = holder.getOwnNodes(queryOptions);
                accumulator.addAll(nodes);
            }
        } else {
            accumulator.addAll(getOwnNodes(queryOptions));
        }
    }

    public List<Node> resolveInheritedNodes(QueryOptions queryOptions) {
        List<Node> ret = new ArrayList<>();
        accumulateInheritedNodesTo(ret, queryOptions);
        return ret;
    }

    public SortedSet<Node> resolveInheritedNodesSorted(QueryOptions queryOptions) {
        SortedSet<Node> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        accumulateInheritedNodesTo(ret, queryOptions);
        return ret;
    }

    public Map<String, Boolean> exportPermissions(QueryOptions queryOptions, boolean convertToLowercase, boolean resolveShorthand) {
        List<Node> entries = resolveInheritedNodes(queryOptions);
        return processExportedPermissions(entries, convertToLowercase, resolveShorthand);
    }

    private static ImmutableMap<String, Boolean> processExportedPermissions(List<Node> entries, boolean convertToLowercase, boolean resolveShorthand) {
        Map<String, Boolean> perms = new HashMap<>(entries.size());
        for (Node node : entries) {
            if (convertToLowercase) {
                perms.putIfAbsent(node.getKey().toLowerCase(), node.getValue());
            } else {
                perms.putIfAbsent(node.getKey(), node.getValue());
            }
        }

        if (resolveShorthand) {
            for (Node node : entries) {
                Collection<String> shorthand = node.resolveShorthand();
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

    public MetaAccumulator accumulateMeta(MetaAccumulator accumulator, QueryOptions queryOptions) {
        if (accumulator == null) {
            accumulator = MetaAccumulator.makeFromConfig(this.plugin);
        }

        InheritanceGraph graph = this.plugin.getInheritanceHandler().getGraph(queryOptions);
        Iterable<PermissionHolder> traversal = graph.traverse(this);
        for (PermissionHolder holder : traversal) {
            List<? extends Node> nodes = holder.getOwnNodes(queryOptions);
            for (Node node : nodes) {
                if (!node.getValue()) continue;
                if (!NodeType.META_OR_CHAT_META.matches(node)) continue;

                accumulator.accumulateNode(node);
            }

            OptionalInt w = holder.getWeight();
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
    public boolean auditTemporaryNodes() {
        boolean transientWork = auditTemporaryNodes(DataType.TRANSIENT);
        boolean normalWork = auditTemporaryNodes(DataType.NORMAL);

        return transientWork || normalWork;
    }

    private boolean auditTemporaryNodes(DataType dataType) {
        ImmutableCollection<? extends Node> before = getData(dataType).immutable().values();
        Set<Node> removed = new HashSet<>();

        boolean work = getData(dataType).auditTemporaryNodes(removed);
        if (work) {
            // invalidate
            invalidateCache();

            // call event
            ImmutableCollection<? extends Node> after = getData(dataType).immutable().values();
            for (Node r : removed) {
                this.plugin.getEventFactory().handleNodeRemove(r, this, dataType, before, after);
            }
        }
        return work;
    }

    public Tristate hasNode(DataType type, Node node, NodeEqualityPredicate equalityPredicate) {
        if (this.getType() == HolderType.GROUP && node instanceof InheritanceNode && ((InheritanceNode) node).getGroupName().equalsIgnoreCase(getObjectName())) {
            return Tristate.TRUE;
        }

        return getData(type).immutable().values().stream()
                .filter(equalityPredicate.equalTo(node))
                .findFirst()
                .map(n -> Tristate.of(n.getValue())).orElse(Tristate.UNDEFINED);
    }

    public DataMutateResult setNode(DataType dataType, Node node, boolean callEvent) {
        if (hasNode(dataType, node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME) != Tristate.UNDEFINED) {
            return DataMutateResult.FAIL_ALREADY_HAS;
        }

        NodeMap data = getData(dataType);

        ImmutableCollection<? extends Node> before = data.immutable().values();

        data.add(node);
        invalidateCache();

        ImmutableCollection<? extends Node> after = data.immutable().values();
        if (callEvent) {
            this.plugin.getEventFactory().handleNodeAdd(node, this, dataType, before, after);
        }

        return DataMutateResult.SUCCESS;
    }

    public DataMutateResult.WithMergedNode setNode(DataType dataType, Node node, TemporaryNodeMergeStrategy mergeStrategy) {
        if (node.hasExpiry() && mergeStrategy != TemporaryNodeMergeStrategy.NONE) {
            Node otherMatch = getData(dataType).immutable().values().stream()
                    .filter(NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE.equalTo(node))
                    .findFirst().orElse(null);
            if (otherMatch != null) {
                NodeMap data = getData(dataType);

                Node newNode = null;
                switch (mergeStrategy) {
                    case ADD_NEW_DURATION_TO_EXISTING: {
                        // Create a new Node with the same properties, but add the expiry dates together
                        long newExpiry = otherMatch.getExpiry().plus(Duration.between(Instant.now(), node.getExpiry())).getEpochSecond();
                        newNode = node.toBuilder().expiry(newExpiry).build();
                    }
                    case REPLACE_EXISTING_IF_DURATION_LONGER: {
                        // Only replace if the new expiry time is greater than the old one.
                        if (node.getExpiry().getEpochSecond() <= otherMatch.getExpiry().getEpochSecond()) {
                            break;
                        }
                        newNode = node;
                    }
                }

                if (newNode != null) {
                    // Remove the old Node & add the new one.
                    ImmutableCollection<? extends Node> before = data.immutable().values();

                    data.replace(newNode, otherMatch);
                    invalidateCache();

                    ImmutableCollection<? extends Node> after = data.immutable().values();
                    this.plugin.getEventFactory().handleNodeAdd(newNode, this, dataType, before, after);

                    return new MergedNodeResult(DataMutateResult.SUCCESS, newNode);
                }
            }
        }

        // Fallback to the normal handling.
        return new MergedNodeResult(setNode(dataType, node, true), node);
    }

    public DataMutateResult unsetNode(DataType dataType, Node node) {
        if (hasNode(dataType, node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE) == Tristate.UNDEFINED) {
            return DataMutateResult.FAIL_LACKS;
        }

        ImmutableCollection<? extends Node> before = getData(dataType).immutable().values();

        getData(dataType).remove(node);
        invalidateCache();

        ImmutableCollection<? extends Node> after = getData(dataType).immutable().values();
        this.plugin.getEventFactory().handleNodeRemove(node, this, dataType, before, after);

        return DataMutateResult.SUCCESS;
    }

    public boolean removeIf(DataType dataType, @Nullable ContextSet contextSet, Predicate<? super Node> predicate, boolean giveDefault) {
        NodeMap data = getData(dataType);
        ImmutableCollection<? extends Node> before = data.immutable().values();

        if (contextSet == null) {
            if (!data.removeIf(predicate)) {
                return false;
            }
        } else {
            if (!data.removeIf(contextSet, predicate)) {
                return false;
            }
        }

        if (getType() == HolderType.USER && giveDefault) {
            getPlugin().getUserManager().giveDefaultIfNeeded((User) this, false);
        }

        invalidateCache();

        ImmutableCollection<? extends Node> after = data.immutable().values();
        this.plugin.getEventFactory().handleNodeClear(this, dataType, before, after);

        return true;
    }

    public boolean clearNodes(DataType dataType, ContextSet contextSet, boolean giveDefault) {
        NodeMap data = getData(dataType);
        ImmutableCollection<? extends Node> before = data.immutable().values();

        if (contextSet == null) {
            data.clear();
        } else {
            data.clear(contextSet);
        }

        if (getType() == HolderType.USER && giveDefault) {
            getPlugin().getUserManager().giveDefaultIfNeeded((User) this, false);
        }

        invalidateCache();

        ImmutableCollection<? extends Node> after = data.immutable().values();

        if (before.size() == after.size()) {
            return false;
        }

        this.plugin.getEventFactory().handleNodeClear(this, dataType, before, after);
        return true;
    }

    public OptionalInt getWeight() {
        return OptionalInt.empty();
    }

    private static final class MergedNodeResult implements DataMutateResult.WithMergedNode {
        private final DataMutateResult result;
        private final Node mergedNode;

        private MergedNodeResult(DataMutateResult result, Node mergedNode) {
            this.result = result;
            this.mergedNode = mergedNode;
        }

        @Override
        public @NonNull DataMutateResult getResult() {
            return this.result;
        }

        @Override
        public @NonNull Node getMergedNode() {
            return this.mergedNode;
        }
    }
}
