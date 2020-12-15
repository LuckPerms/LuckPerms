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

package me.lucko.luckperms.common.model.nodemap;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ContextSetComparator;
import me.lucko.luckperms.common.model.InheritanceOrigin;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.nodemap.MutateResult.ChangeType;
import me.lucko.luckperms.common.node.comparator.NodeComparator;

import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.Iterator;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class NodeMapMutable extends NodeMapBase {

    // Used in calls to Map#computeIfAbsent to make them behave like a LoadingMap/Cache
    // The key (ImmutableContextSet) isn't actually used - these are more like suppliers than functions
    private static final Function<ImmutableContextSet, SortedSet<Node>> VALUE_SET_SUPPLIER = k -> new ConcurrentSkipListSet<>(NodeComparator.reverse());
    private static final Function<ImmutableContextSet, SortedSet<InheritanceNode>> INHERITANCE_VALUE_SET_SUPPLIER = k -> new ConcurrentSkipListSet<>(NodeComparator.reverse());

    // Creates the Map instances used by this.map and this.inheritanceMap
    private static <N extends Node> SortedMap<ImmutableContextSet, SortedSet<N>> createMap() {
        return new ConcurrentSkipListMap<>(ContextSetComparator.reverse());
    }

    /*
     * Nodes are inserted into the maps using Node#getContexts() as the key.
     * The context set keys are ordered according to the rules of ContextSetComparator.
     * The node values are ordered according to the priority rules defined in NodeComparator.
     *
     * We use our own "multimap"-like implementation here because guava's is not thread safe.
     *
     * The map fields aren't final because they are replaced when large updates (e.g. clear)
     * are performed. We do this so there's no risk that the read methods will see an inconsistent
     * state in the middle of an update from the DB. (see below comment about locking - we don't
     * lock for reads!)
     */
    private SortedMap<ImmutableContextSet, SortedSet<Node>> map = createMap();
    private SortedMap<ImmutableContextSet, SortedSet<InheritanceNode>> inheritanceMap = createMap();

    /**
     * This lock is used whilst performing mutations, but *not* reads.
     *
     * The maps themselves are thread safe, so for querying, we just allow
     * the read methods to do whatever they want without any locking.
     * However, we want mutations to be atomic, so we use the lock to ensure that happens.
     */
    private final Lock lock = new ReentrantLock();

    protected final PermissionHolder holder;

    public NodeMapMutable(PermissionHolder holder) {
        this.holder = holder;
    }

    @Override
    protected SortedMap<ImmutableContextSet, SortedSet<Node>> map() {
        return this.map;
    }

    @Override
    protected SortedMap<ImmutableContextSet, SortedSet<InheritanceNode>> inheritanceMap() {
        return this.inheritanceMap;
    }

    @Override
    protected ContextSatisfyMode defaultSatisfyMode() {
        return this.holder.getPlugin().getConfiguration().get(ConfigKeys.CONTEXT_SATISFY_MODE);
    }

    private Node addInheritanceOrigin(Node node) {
        Optional<InheritanceOriginMetadata> metadata = node.getMetadata(InheritanceOriginMetadata.KEY);
        if (metadata.isPresent() && metadata.get().getOrigin().equals(this.holder.getIdentifier())) {
            return node;
        }

        return node.toBuilder().withMetadata(InheritanceOriginMetadata.KEY, new InheritanceOrigin(this.holder.getIdentifier())).build();
    }

    @Override
    public MutateResult add(Node nodeWithoutInheritanceOrigin) {
        Node node = addInheritanceOrigin(nodeWithoutInheritanceOrigin);

        ImmutableContextSet context = node.getContexts();
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            SortedSet<Node> nodes = this.map.computeIfAbsent(context, VALUE_SET_SUPPLIER);

            // add the new node to the set - if it was already there, return
            if (!nodes.add(node)) {
                return result;
            }

            // mark that we added the node in the results
            result.recordChange(ChangeType.ADD, node);

            // remove any others that were in the set already with a different value/expiry time
            removeMatchingButNotSame(nodes.iterator(), node, result);

            // update the inheritanceMap too if necessary
            if (node instanceof InheritanceNode) {
                SortedSet<InheritanceNode> inhNodes = this.inheritanceMap.computeIfAbsent(context, INHERITANCE_VALUE_SET_SUPPLIER);
                // remove existing..
                inhNodes.removeIf(el -> node.equals(el, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
                // .. & add
                if (node.getValue()) {
                    inhNodes.add((InheritanceNode) node);
                }
            }

        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult remove(Node node) {
        ImmutableContextSet context = node.getContexts();
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            SortedSet<Node> nodes = this.map.get(context);
            if (nodes == null) {
                return result;
            }

            // remove any nodes that match, record to results
            removeMatching(nodes.iterator(), node, result);

            // update inheritance map too
            if (node instanceof InheritanceNode) {
                SortedSet<InheritanceNode> inhNodes = this.inheritanceMap.get(context);
                if (inhNodes != null) {
                    inhNodes.removeIf(el -> node.equals(el, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE));
                }
            }

        } finally {
            this.lock.unlock();
        }

        return result;
    }

    private static void removeMatching(Iterator<Node> it, Node node, MutateResult result) {
        while (it.hasNext()) {
            Node el = it.next();
            if (node.equals(el, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE)) {
                it.remove();
                result.recordChange(ChangeType.REMOVE, el);
            }
        }
    }

    private static void removeMatchingButNotSame(Iterator<Node> it, Node node, MutateResult result) {
        while (it.hasNext()) {
            Node el = it.next();
            if (el != node && node.equals(el, NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE)) {
                it.remove();
                result.recordChange(ChangeType.REMOVE, el);
            }
        }
    }

    @Override
    public MutateResult removeExact(Node node) {
        ImmutableContextSet context = node.getContexts();
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            SortedSet<Node> nodes = this.map.get(context);
            if (nodes == null) {
                return result;
            }

            // try to remove an exact match
            if (nodes.remove(node)) {
                // if we removed something, record to results
                result.recordChange(ChangeType.REMOVE, node);

                // update inheritance map too if necessary
                if (node instanceof InheritanceNode && node.getValue()) {
                    SortedSet<InheritanceNode> inhNodes = this.inheritanceMap.get(context);
                    if (inhNodes != null) {
                        inhNodes.remove(node);
                    }
                }
            }

        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult removeIf(Predicate<? super Node> predicate) {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            for (SortedSet<Node> nodes : this.map.values()) {
                removeMatching(nodes.iterator(), predicate, result);
            }
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult removeIf(ContextSet contextSet, Predicate<? super Node> predicate) {
        ImmutableContextSet context = contextSet.immutableCopy();
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            SortedSet<Node> nodes = this.map.get(context);
            if (nodes == null) {
                return result;
            }
            removeMatching(nodes.iterator(), predicate, result);
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    private void removeMatching(Iterator<Node> it, Predicate<? super Node> predicate, MutateResult result) {
        while (it.hasNext()) {
            Node node = it.next();

            // if the predicate passes, remove the node from the set & record to results
            if (predicate.test(node)) {
                it.remove();
                result.recordChange(ChangeType.REMOVE, node);

                // update inheritance map too if necessary
                if (node instanceof InheritanceNode && node.getValue()) {
                    SortedSet<InheritanceNode> inhNodes = this.inheritanceMap.get(node.getContexts());
                    if (inhNodes != null) {
                        inhNodes.remove(node);
                    }
                }
            }
        }
    }

    @Override
    public MutateResult removeThenAdd(Node nodeToRemove, Node nodeToAdd) {
        if (nodeToAdd.equals(nodeToRemove)) {
            return new MutateResult();
        }

        this.lock.lock();
        try {
            return removeExact(nodeToRemove).mergeFrom(add(nodeToAdd));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public MutateResult clear() {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            // log removals
            for (SortedSet<Node> nodes : this.map.values()) {
                result.recordChanges(ChangeType.REMOVE, nodes);
            }

            // replace the map - this means any client reading async won't be affected
            // by any race conditions between this call to clear and any subsequent call to setContent
            this.map = createMap();
            this.inheritanceMap = createMap();
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult clear(ContextSet contextSet) {
        ImmutableContextSet context = contextSet.immutableCopy();
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            SortedSet<Node> removed = this.map.remove(context);
            if (removed != null) {
                result.recordChanges(ChangeType.REMOVE, removed);
                this.inheritanceMap.remove(context);
            }
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult setContent(Iterable<? extends Node> set) {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            result.mergeFrom(clear());
            result.mergeFrom(addAll(set));
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult setContent(Stream<? extends Node> stream) {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            result.mergeFrom(clear());
            result.mergeFrom(addAll(stream));
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult addAll(Iterable<? extends Node> set) {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            for (Node n : set) {
                result.mergeFrom(add(n));
            }
        } finally {
            this.lock.unlock();
        }

        return result;
    }

    @Override
    public MutateResult addAll(Stream<? extends Node> stream) {
        MutateResult result = new MutateResult();

        this.lock.lock();
        try {
            stream.forEach(n -> result.mergeFrom(add(n)));
        } finally {
            this.lock.unlock();
        }

        return result;
    }

}
