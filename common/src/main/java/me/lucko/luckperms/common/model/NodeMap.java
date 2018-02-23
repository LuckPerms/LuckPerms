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

import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;

import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.contexts.ContextSetComparator;
import me.lucko.luckperms.common.node.ImmutableLocalizedNode;
import me.lucko.luckperms.common.node.NodeComparator;
import me.lucko.luckperms.common.node.NodeWithContextComparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

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
    private final SortedSetMultimap<ImmutableContextSet, Node> map = MultimapBuilder
            .treeKeys(ContextSetComparator.reverse())
            .treeSetValues(NodeComparator.reverse())
            .build();

    /**
     * Copy of {@link #map} which only contains group nodes
     * @see Node#isGroupNode()
     */
    private final SortedSetMultimap<ImmutableContextSet, Node> inheritanceMap = MultimapBuilder
            .treeKeys(ContextSetComparator.reverse())
            .treeSetValues(NodeComparator.reverse())
            .build();

    /**
     * The lock which synchronizes the instance
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * A cache which holds an immutable copy of the backing map
     */
    private final NodeMapCache cache = new NodeMapCache(this);

    NodeMap(PermissionHolder holder) {
        this.holder = holder;
    }

    public List<Node> asList() {
        this.lock.lock();
        try {
            return new ArrayList<>(this.map.values());
        } finally {
            this.lock.unlock();
        }
    }

    public LinkedHashSet<Node> asSet() {
        this.lock.lock();
        try {
            return new LinkedHashSet<>(this.map.values());
        } finally {
            this.lock.unlock();
        }
    }

    public SortedSet<LocalizedNode> asSortedSet() {
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        copyToLocalized(ret);
        return ret;
    }

    public void copyTo(Collection<? super Node> collection) {
        this.lock.lock();
        try {
            collection.addAll(this.map.values());
        } finally {
            this.lock.unlock();
        }
    }

    public void copyTo(Collection<? super Node> collection, ContextSet filter) {
        this.lock.lock();
        try {
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : this.map.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    collection.addAll(e.getValue());
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void copyGroupNodesTo(Collection<? super Node> collection) {
        this.lock.lock();
        try {
            collection.addAll(this.inheritanceMap.values());
        } finally {
            this.lock.unlock();
        }
    }

    public void copyGroupNodesTo(Collection<? super Node> collection, ContextSet filter) {
        this.lock.lock();
        try {
            for (Map.Entry<ImmutableContextSet, Collection<Node>> e : this.inheritanceMap.asMap().entrySet()) {
                if (e.getKey().isSatisfiedBy(filter)) {
                    collection.addAll(e.getValue());
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    public void copyToLocalized(Collection<LocalizedNode> collection) {
        this.lock.lock();
        try {
            for (Node node : this.map.values()) {
                collection.add(ImmutableLocalizedNode.of(node, this.holder.getObjectName()));
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns an immutable representation of the maps current state.
     *
     * @return an immutable copy
     */
    public ImmutableSetMultimap<ImmutableContextSet, Node> immutable() {
        return this.cache.get();
    }

    /**
     * Invalidates the cache
     */
    void invalidate() {
        this.cache.invalidate();
    }

    void add(Node node) {
        this.lock.lock();
        try {
            ImmutableContextSet context = node.getFullContexts().makeImmutable();
            this.map.put(context, node);
            if (node.isGroupNode() && node.getValuePrimitive()) {
                this.inheritanceMap.put(context, node);
            }
        } finally {
            this.lock.unlock();
        }
    }

    void remove(Node node) {
        this.lock.lock();
        try {
            ImmutableContextSet context = node.getFullContexts().makeImmutable();
            this.map.get(context).removeIf(e -> e.equals(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE));
            if (node.isGroupNode()) {
                this.inheritanceMap.get(context).removeIf(e -> e.equals(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE));
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void removeExact(Node node) {
        this.lock.lock();
        try {
            ImmutableContextSet context = node.getFullContexts().makeImmutable();
            this.map.remove(context, node);
            if (node.isGroupNode() && node.getValuePrimitive()) {
                this.inheritanceMap.remove(context, node);
            }
        } finally {
            this.lock.unlock();
        }
    }

    void replace(Node node, Node previous) {
        this.lock.lock();
        try {
            removeExact(previous);
            add(node);
        } finally {
            this.lock.unlock();
        }
    }

    void clear() {
        this.lock.lock();
        try {
            this.map.clear();
            this.inheritanceMap.clear();
        } finally {
            this.lock.unlock();
        }
    }

    void clear(ContextSet contextSet) {
        this.lock.lock();
        try {
            ImmutableContextSet context = contextSet.makeImmutable();
            this.map.removeAll(context);
            this.inheritanceMap.removeAll(context);
        } finally {
            this.lock.unlock();
        }
    }

    void setContent(Set<Node> set) {
        this.lock.lock();
        try {
            this.map.clear();
            this.inheritanceMap.clear();
            for (Node n : set) {
                add(n);
            }
        } finally {
            this.lock.unlock();
        }
    }

    void setContent(Multimap<ImmutableContextSet, Node> multimap) {
        this.lock.lock();
        try {
            this.map.clear();
            this.inheritanceMap.clear();

            this.map.putAll(multimap);
            for (Map.Entry<ImmutableContextSet, Node> entry : this.map.entries()) {
                if (entry.getValue().isGroupNode() && entry.getValue().getValuePrimitive()) {
                    this.inheritanceMap.put(entry.getKey(), entry.getValue());
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    boolean removeIf(Predicate<? super Node> predicate) {
        this.lock.lock();
        try {
            boolean ret = this.map.values().removeIf(predicate);
            if (ret) {
                this.inheritanceMap.values().removeIf(predicate);
            }
            return ret;
        } finally {
            this.lock.unlock();
        }
    }

    boolean removeIf(ContextSet contextSet, Predicate<? super Node> predicate) {
        this.lock.lock();
        try {
            ImmutableContextSet context = contextSet.makeImmutable();
            SortedSet<Node> nodes = this.map.get(context);
            boolean ret = nodes.removeIf(predicate);
            if (ret) {
                this.inheritanceMap.get(context).removeIf(predicate);
            }
            return ret;
        } finally {
            this.lock.unlock();
        }
    }

    boolean auditTemporaryNodes(@Nullable Set<Node> removed) {
        boolean work = false;

        this.lock.lock();
        try {
            Iterator<Node> it = this.map.values().iterator();
            while (it.hasNext()) {
                Node entry = it.next();
                if (entry.hasExpired()) {
                    if (removed != null) {
                        removed.add(entry);
                    }
                    if (entry.isGroupNode() && entry.getValuePrimitive()) {
                        this.inheritanceMap.remove(entry.getFullContexts().makeImmutable(), entry);
                    }
                    work = true;
                    it.remove();
                }
            }
        } finally {
            this.lock.unlock();
        }

        return work;
    }

    private static final class NodeMapCache extends Cache<ImmutableSetMultimap<ImmutableContextSet, Node>> {
        private final NodeMap handle;

        private NodeMapCache(NodeMap handle) {
            this.handle = handle;
        }

        @Override
        protected ImmutableSetMultimap<ImmutableContextSet, Node> supply() {
            this.handle.lock.lock();
            try {
                return ImmutableSetMultimap.copyOf(this.handle.map);
            } finally {
                this.handle.lock.unlock();
            }
        }
    }

}
