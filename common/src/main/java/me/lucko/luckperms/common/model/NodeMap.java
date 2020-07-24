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

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ContextSetComparator;
import me.lucko.luckperms.common.node.comparator.NodeComparator;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;

import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A map of nodes held by a {@link PermissionHolder}.
 *
 * <p>Permissions are stored in Multimaps, with the context of the node being the key, and the actual Node object being
 * the value. The keys (context sets) are ordered according to their weight {@link ContextSetComparator}, and the values
 * are ordered according to the priority of the node, according to {@link NodeComparator}.</p>
 *
 * <p>Each holder has two of these maps, one for enduring and transient nodes.</p>
 */
public final class NodeMap {
    private static final Function<ImmutableContextSet, SortedSet<Node>> VALUE_SET_SUPPLIER = k -> new ConcurrentSkipListSet<>(NodeComparator.reverse());
    private static final Function<ImmutableContextSet, SortedSet<InheritanceNode>> INHERITANCE_VALUE_SET_SUPPLIER = k -> new ConcurrentSkipListSet<>(NodeComparator.reverse());

    /**
     * The holder which this map is for
     */
    private final PermissionHolder holder;

    /**
     * The backing data map.
     *
     * <p>Nodes are mapped by the result of {@link Node#getContexts()}, and keys are sorted by the weight of the
     * ContextSet. ContextSets are ordered first by the presence of a server key, then by the presence of a world
     * key, and finally by the overall size of the set. Nodes are ordered according to the priority rules
     * defined in {@link NodeComparator}.</p>
     */
    private final SortedMap<ImmutableContextSet, SortedSet<Node>> map = new ConcurrentSkipListMap<>(ContextSetComparator.reverse());

    /**
     * Copy of {@link #map} which only contains group nodes
     * @see InheritanceNode
     */
    private final SortedMap<ImmutableContextSet, SortedSet<InheritanceNode>> inheritanceMap = new ConcurrentSkipListMap<>(ContextSetComparator.reverse());

    NodeMap(PermissionHolder holder) {
        this.holder = holder;
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public int size() {
        int size = 0;
        for (SortedSet<Node> values : this.map.values()) {
            size += values.size();
        }
        return size;
    }

    public List<Node> asList() {
        List<Node> list = new ArrayList<>();
        copyTo(list);
        return list;
    }

    public LinkedHashSet<Node> asSet() {
        LinkedHashSet<Node> set = new LinkedHashSet<>();
        copyTo(set);
        return set;
    }

    public SortedSet<Node> asSortedSet() {
        SortedSet<Node> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyTo(set);
        return set;
    }

    public ImmutableSet<Node> asImmutableSet() {
        ImmutableSet.Builder<Node> builder = ImmutableSet.builder();
        for (SortedSet<Node> values : this.map.values()) {
            builder.addAll(values);
        }
        return builder.build();
    }

    public Map<ImmutableContextSet, Collection<Node>> asMap() {
        Map<ImmutableContextSet, Collection<Node>> map = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : this.map.entrySet()) {
            map.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return map;
    }

    public List<InheritanceNode> inheritanceAsList() {
        List<InheritanceNode> set = new ArrayList<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    public LinkedHashSet<InheritanceNode> inheritanceAsSet() {
        LinkedHashSet<InheritanceNode> set = new LinkedHashSet<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    public SortedSet<InheritanceNode> inheritanceAsSortedSet() {
        SortedSet<InheritanceNode> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyInheritanceNodesTo(set);
        return set;
    }

    public Map<ImmutableContextSet, Collection<InheritanceNode>> inheritanceAsMap() {
        Map<ImmutableContextSet, Collection<InheritanceNode>> map = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, SortedSet<InheritanceNode>> e : this.inheritanceMap.entrySet()) {
            map.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return map;
    }

    private static boolean flagExcludeTest(Flag flag, String contextKey, QueryOptions filter, ImmutableContextSet contextSet) {
        // return true (negative result) if the explicit *include* flag is not set, and if the context set doesn't contain the required context key.
        return !filter.flag(flag) && !contextSet.containsKey(contextKey);
    }

    private static boolean normalNodesExcludeTest(QueryOptions filter, ImmutableContextSet contextSet) {
        // return true (negative result) if normal nodes should not be included due to the lack of a server/world context.
        return flagExcludeTest(Flag.INCLUDE_NODES_WITHOUT_SERVER_CONTEXT, DefaultContextKeys.SERVER_KEY, filter, contextSet) ||
                flagExcludeTest(Flag.INCLUDE_NODES_WITHOUT_WORLD_CONTEXT, DefaultContextKeys.WORLD_KEY, filter, contextSet);
    }

    private static boolean inheritanceNodesIncludeTest(QueryOptions filter, ImmutableContextSet contextSet) {
        // return true (positive result) if inheritance nodes should be included, due to the lack of any flags preventing their inclusion.
        return !flagExcludeTest(Flag.APPLY_INHERITANCE_NODES_WITHOUT_SERVER_CONTEXT, DefaultContextKeys.SERVER_KEY, filter, contextSet) &&
                !flagExcludeTest(Flag.APPLY_INHERITANCE_NODES_WITHOUT_WORLD_CONTEXT, DefaultContextKeys.WORLD_KEY, filter, contextSet);
    }

    private ContextSatisfyMode defaultSatisfyMode() {
        return this.holder.getPlugin().getConfiguration().get(ConfigKeys.CONTEXT_SATISFY_MODE);
    }

    public void forEach(Consumer<? super Node> consumer) {
        for (SortedSet<Node> values : this.map.values()) {
            values.forEach(consumer);
        }
    }

    public void forEach(QueryOptions filter, Consumer<? super Node> consumer) {
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : this.map.entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                    // only copy inheritance nodes.
                    SortedSet<InheritanceNode> inheritanceNodes = this.inheritanceMap.get(e.getKey());
                    if (inheritanceNodes != null) {
                        inheritanceNodes.forEach(consumer);
                    }
                }
            } else {
                e.getValue().forEach(consumer);
            }
        }
    }

    public void copyTo(Collection<? super Node> collection) {
        for (SortedSet<Node> values : this.map.values()) {
            collection.addAll(values);
        }
    }

    public void copyTo(Collection<? super Node> collection, QueryOptions filter) {
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : this.map.entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                    // only copy inheritance nodes.
                    SortedSet<InheritanceNode> inheritanceNodes = this.inheritanceMap.get(e.getKey());
                    if (inheritanceNodes != null) {
                        collection.addAll(inheritanceNodes);
                    }
                }
            } else {
                collection.addAll(e.getValue());
            }
        }
    }

    public <T extends Node> void copyTo(Collection<? super T> collection, NodeType<T> type, QueryOptions filter) {
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : this.map.entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                    // only copy inheritance nodes.
                    if (type == NodeType.INHERITANCE) {
                        SortedSet<InheritanceNode> inheritanceNodes = this.inheritanceMap.get(e.getKey());
                        if (inheritanceNodes != null) {
                            for (InheritanceNode node : inheritanceNodes) {
                                collection.add(type.cast(node));
                            }
                        }
                    }
                }
            } else {
                for (Node node : e.getValue()) {
                    if (type.matches(node)) {
                        collection.add(type.cast(node));
                    }
                }
            }
        }
    }

    public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection) {
        for (SortedSet<InheritanceNode> values : this.inheritanceMap.values()) {
            collection.addAll(values);
        }
    }

    public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection, QueryOptions filter) {
        for (Map.Entry<ImmutableContextSet, SortedSet<InheritanceNode>> e : this.inheritanceMap.entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                collection.addAll(e.getValue());
            }
        }
    }

    public Collection<Node> nodesInContext(ContextSet context) {
        final SortedSet<Node> values = this.map.get(context.immutableCopy());
        if (values == null) {
            return ImmutableSet.of();
        }
        return new ArrayList<>(values);
    }

    public Collection<InheritanceNode> inheritanceNodesInContext(ContextSet context) {
        final SortedSet<InheritanceNode> values = this.inheritanceMap.get(context.immutableCopy());
        if (values == null) {
            return ImmutableSet.of();
        }
        return new ArrayList<>(values);
    }

    private Node localise(Node node) {
        Optional<InheritanceOriginMetadata> metadata = node.getMetadata(InheritanceOriginMetadata.KEY);
        if (metadata.isPresent() && metadata.get().getOrigin().equals(this.holder.getIdentifier())) {
            return node;
        }

        return node.toBuilder().withMetadata(InheritanceOriginMetadata.KEY, new InheritanceOrigin(this.holder.getIdentifier())).build();
    }

    void add(Node node) {
        ImmutableContextSet context = node.getContexts();
        Node n = localise(node);

        SortedSet<Node> nodesInContext = this.map.computeIfAbsent(context, VALUE_SET_SUPPLIER);
        nodesInContext.removeIf(e -> e.equals(node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
        nodesInContext.add(n);

        if (n instanceof InheritanceNode) {
            SortedSet<InheritanceNode> inheritanceNodesInContext = this.inheritanceMap.computeIfAbsent(context, INHERITANCE_VALUE_SET_SUPPLIER);
            inheritanceNodesInContext.removeIf(e -> e.equals(node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
            if (n.getValue()) {
                inheritanceNodesInContext.add((InheritanceNode) n);
            }
        }
    }

    void remove(Node node) {
        ImmutableContextSet context = node.getContexts();
        SortedSet<Node> nodesInContext = this.map.get(context);
        if (nodesInContext != null) {
            nodesInContext.removeIf(e -> e.equals(node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
        }

        if (node instanceof InheritanceNode) {
            SortedSet<InheritanceNode> inheritanceNodesInContext = this.inheritanceMap.get(context);
            if (inheritanceNodesInContext != null) {
                inheritanceNodesInContext.removeIf(e -> e.equals(node, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
            }
        }
    }

    private void removeExact(Node node) {
        ImmutableContextSet context = node.getContexts();
        SortedSet<Node> nodesInContext = this.map.get(context);
        if (nodesInContext != null) {
            nodesInContext.remove(node);
        }

        if (node instanceof InheritanceNode && node.getValue()) {
            SortedSet<InheritanceNode> inheritanceNodesInContext = this.inheritanceMap.get(context);
            if (inheritanceNodesInContext != null) {
                inheritanceNodesInContext.remove(node);
            }
        }
    }

    void replace(Node node, Node previous) {
        removeExact(previous);
        add(node);
    }

    void clear() {
        this.map.clear();
        this.inheritanceMap.clear();
    }

    void clear(ContextSet contextSet) {
        ImmutableContextSet context = contextSet.immutableCopy();
        this.map.remove(context);
        this.inheritanceMap.remove(context);
    }

    void setContent(Iterable<? extends Node> set) {
        this.map.clear();
        this.inheritanceMap.clear();
        mergeContent(set);
    }

    void setContent(Stream<? extends Node> stream) {
        this.map.clear();
        this.inheritanceMap.clear();
        mergeContent(stream);
    }

    void mergeContent(Iterable<? extends Node> set) {
        for (Node n : set) {
            add(n);
        }
    }

    void mergeContent(Stream<? extends Node> stream) {
        stream.forEach(this::add);
    }

    boolean removeIf(Predicate<? super Node> predicate) {
        boolean success = false;
        for (SortedSet<Node> valueSet : this.map.values()) {
            if (valueSet.removeIf(predicate)) {
                success = true;
            }
        }
        for (SortedSet<InheritanceNode> valueSet : this.inheritanceMap.values()) {
            valueSet.removeIf(predicate);
        }
        return success;
    }

    boolean removeIf(ContextSet contextSet, Predicate<? super Node> predicate) {
        ImmutableContextSet context = contextSet.immutableCopy();

        boolean success = false;

        SortedSet<Node> nodesInContext = this.map.get(context);
        if (nodesInContext != null) {
            success = nodesInContext.removeIf(predicate);
        }

        SortedSet<InheritanceNode> inheritanceNodesInContext = this.inheritanceMap.get(context);
        if (inheritanceNodesInContext != null) {
            inheritanceNodesInContext.removeIf(predicate);
        }

        return success;
    }

    boolean auditTemporaryNodes(@Nullable Set<? super Node> removed) {
        boolean work = false;

        for (SortedSet<Node> valueSet : this.map.values()) {
            Iterator<Node> it = valueSet.iterator();
            while (it.hasNext()) {
                Node entry = it.next();
                if (!entry.hasExpired()) {
                    continue;
                }

                // remove
                if (removed != null) {
                    removed.add(entry);
                }
                if (entry instanceof InheritanceNode && entry.getValue()) {
                    SortedSet<InheritanceNode> inheritanceNodesInContext = this.inheritanceMap.get(entry.getContexts());
                    if (inheritanceNodesInContext != null) {
                        inheritanceNodesInContext.remove(entry);
                    }
                }
                it.remove();
                work = true;
            }
        }

        return work;
    }

}
