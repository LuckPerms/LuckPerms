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

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.contexts.ContextSetComparator;
import me.lucko.luckperms.common.node.comparator.NodeComparator;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.model.ImmutableLocalizedNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    @SuppressWarnings("Guava")
    private static final Supplier<SortedSet<LocalizedNode>> VALUE_SET_SUPPLIER = () -> new ConcurrentSkipListSet<>(NodeComparator.reverse());

    /**
     * The holder which this map is for
     */
    private final PermissionHolder holder;

    /**
     * The backing data map.
     *
     * <p>Nodes are mapped by the result of {@link Node#getFullContexts()}, and keys are sorted by the weight of the
     * ContextSet. ContextSets are ordered first by the presence of a server key, then by the presence of a world
     * key, and finally by the overall size of the set. Nodes are ordered according to the priority rules
     * defined in {@link NodeComparator}.</p>
     */
    private final SortedSetMultimap<ImmutableContextSet, LocalizedNode> map = Multimaps.newSortedSetMultimap(
            new ConcurrentSkipListMap<>(ContextSetComparator.reverse()),
            VALUE_SET_SUPPLIER
    );

    /**
     * Copy of {@link #map} which only contains group nodes
     * @see Node#isGroupNode()
     */
    private final SortedSetMultimap<ImmutableContextSet, LocalizedNode> inheritanceMap = Multimaps.newSortedSetMultimap(
            new ConcurrentSkipListMap<>(ContextSetComparator.reverse()),
            VALUE_SET_SUPPLIER
    );

    /**
     * A cache which holds an immutable copy of the backing map
     */
    private final NodeMapCache cache = new NodeMapCache(this);

    NodeMap(PermissionHolder holder) {
        this.holder = holder;
    }

    public List<LocalizedNode> asList() {
        return new ArrayList<>(this.map.values());
    }

    public LinkedHashSet<LocalizedNode> asSet() {
        return new LinkedHashSet<>(this.map.values());
    }

    public SortedSet<LocalizedNode> asSortedSet() {
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        copyTo(ret);
        return ret;
    }

    public void copyTo(Collection<? super LocalizedNode> collection) {
        collection.addAll(this.map.values());
    }

    public void copyTo(Collection<? super LocalizedNode> collection, ContextSet filter) {
        for (Map.Entry<ImmutableContextSet, Collection<LocalizedNode>> e : this.map.asMap().entrySet()) {
            if (e.getKey().isSatisfiedBy(filter)) {
                collection.addAll(e.getValue());
            }
        }
    }

    public void copyGroupNodesTo(Collection<? super LocalizedNode> collection) {
        collection.addAll(this.inheritanceMap.values());
    }

    public void copyGroupNodesTo(Collection<? super LocalizedNode> collection, ContextSet filter) {
        for (Map.Entry<ImmutableContextSet, Collection<LocalizedNode>> e : this.inheritanceMap.asMap().entrySet()) {
            if (e.getKey().isSatisfiedBy(filter)) {
                collection.addAll(e.getValue());
            }
        }
    }

    /**
     * Returns an immutable representation of the maps current state.
     *
     * @return an immutable copy
     */
    public ImmutableSetMultimap<ImmutableContextSet, LocalizedNode> immutable() {
        return this.cache.get();
    }

    /**
     * Invalidates the cache
     */
    void invalidate() {
        this.cache.invalidate();
    }

    private LocalizedNode localise(Node node) {
        if (node instanceof LocalizedNode) {
            LocalizedNode localizedNode = (LocalizedNode) node;
            if (this.holder.getObjectName().equals(localizedNode.getLocation())) {
                return localizedNode;
            }
        }

        // localise
        return ImmutableLocalizedNode.of(node, this.holder.getObjectName());
    }

    void add(Node node) {
        ImmutableContextSet context = node.getFullContexts().makeImmutable();
        LocalizedNode n = localise(node);

        this.map.put(context, n);
        if (node.isGroupNode() && node.getValue()) {
            this.inheritanceMap.put(context, n);
        }
    }

    void remove(Node node) {
        ImmutableContextSet context = node.getFullContexts().makeImmutable();
        this.map.get(context).removeIf(e -> e.equals(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE));
        if (node.isGroupNode()) {
            this.inheritanceMap.get(context).removeIf(e -> e.equals(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE));
        }
    }

    private void removeExact(Node node) {
        ImmutableContextSet context = node.getFullContexts().makeImmutable();
        this.map.remove(context, node);
        if (node.isGroupNode() && node.getValue()) {
            this.inheritanceMap.remove(context, node);
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
        ImmutableContextSet context = contextSet.makeImmutable();
        this.map.removeAll(context);
        this.inheritanceMap.removeAll(context);
    }

    void setContent(Collection<? extends Node> set) {
        this.map.clear();
        this.inheritanceMap.clear();
        for (Node n : set) {
            add(n);
        }
    }

    void setContent(Multimap<ImmutableContextSet, ? extends Node> multimap) {
        setContent(multimap.values());
    }

    boolean removeIf(Predicate<? super LocalizedNode> predicate) {
        boolean ret = this.map.values().removeIf(predicate);
        this.inheritanceMap.values().removeIf(predicate);
        return ret;
    }

    boolean removeIf(ContextSet contextSet, Predicate<? super LocalizedNode> predicate) {
        ImmutableContextSet context = contextSet.makeImmutable();
        SortedSet<LocalizedNode> nodes = this.map.get(context);
        boolean ret = nodes.removeIf(predicate);
        this.inheritanceMap.get(context).removeIf(predicate);
        return ret;
    }

    boolean auditTemporaryNodes(@Nullable Set<? super LocalizedNode> removed) {
        boolean work = false;

        Iterator<? extends LocalizedNode> it = this.map.values().iterator();
        while (it.hasNext()) {
            LocalizedNode entry = it.next();
            if (entry.hasExpired()) {
                if (removed != null) {
                    removed.add(entry);
                }
                if (entry.isGroupNode() && entry.getValue()) {
                    this.inheritanceMap.remove(entry.getFullContexts().makeImmutable(), entry);
                }
                work = true;
                it.remove();
            }
        }

        return work;
    }

    private static final class NodeMapCache extends Cache<ImmutableSetMultimap<ImmutableContextSet, LocalizedNode>> {
        private final NodeMap handle;

        private NodeMapCache(NodeMap handle) {
            this.handle = handle;
        }

        @Nonnull
        @Override
        protected ImmutableSetMultimap<ImmutableContextSet, LocalizedNode> supply() {
            return ImmutableSetMultimap.copyOf(this.handle.map);
        }
    }

}
