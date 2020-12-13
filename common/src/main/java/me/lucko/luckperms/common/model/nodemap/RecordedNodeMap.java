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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A forwarding {@link NodeMap} that records all mutations and keeps them in a log.
 */
public class RecordedNodeMap implements NodeMap {

    private final NodeMap delegate;
    private final Lock lock = new ReentrantLock();
    private MutateResult changes = new MutateResult();

    public RecordedNodeMap(NodeMap delegate) {
        this.delegate = delegate;
    }

    public NodeMap bypass() {
        return this.delegate;
    }

    public void discardChanges() {
        this.lock.lock();
        try {
            this.changes.clear();
        } finally {
            this.lock.unlock();
        }
    }

    public MutateResult exportChanges(Predicate<MutateResult> onlyIf) {
        this.lock.lock();
        try {
            MutateResult existing = this.changes;
            if (onlyIf.test(existing)) {
                this.changes = new MutateResult();
                return existing;
            }
            return null;
        } finally {
            this.lock.unlock();
        }
    }

    private MutateResult record(MutateResult result) {
        this.lock.lock();
        try {
            this.changes.mergeFrom(result);
        } finally {
            this.lock.unlock();
        }
        return result;
    }

    // delegate, but pass the result through #record(MutateResult)
    
    @Override
    public MutateResult add(Node nodeWithoutInheritanceOrigin) {
        return record(this.delegate.add(nodeWithoutInheritanceOrigin));
    }

    @Override
    public MutateResult remove(Node node) {
        return record(this.delegate.remove(node));
    }

    @Override
    public MutateResult removeExact(Node node) {
        return record(this.delegate.removeExact(node));
    }

    @Override
    public MutateResult removeIf(Predicate<? super Node> predicate) {
        return record(this.delegate.removeIf(predicate));
    }

    @Override
    public MutateResult removeIf(ContextSet contextSet, Predicate<? super Node> predicate) {
        return record(this.delegate.removeIf(contextSet, predicate));
    }

    @Override
    public MutateResult removeThenAdd(Node nodeToRemove, Node nodeToAdd) {
        return record(this.delegate.removeThenAdd(nodeToRemove, nodeToAdd));
    }

    @Override
    public MutateResult clear() {
        return record(this.delegate.clear());
    }

    @Override
    public MutateResult clear(ContextSet contextSet) {
        return record(this.delegate.clear(contextSet));
    }

    @Override
    public MutateResult setContent(Iterable<? extends Node> set) {
        return record(this.delegate.setContent(set));
    }

    @Override
    public MutateResult setContent(Stream<? extends Node> stream) {
        return record(this.delegate.setContent(stream));
    }

    @Override
    public MutateResult addAll(Iterable<? extends Node> set) {
        return record(this.delegate.addAll(set));
    }

    @Override
    public MutateResult addAll(Stream<? extends Node> stream) {
        return record(this.delegate.addAll(stream));
    }

    // just plain delegation

    @Override public boolean isEmpty() { return this.delegate.isEmpty(); }
    @Override public int size() { return this.delegate.size(); }
    @Override public List<Node> asList() { return this.delegate.asList(); }
    @Override public LinkedHashSet<Node> asSet() { return this.delegate.asSet(); }
    @Override public SortedSet<Node> asSortedSet() { return this.delegate.asSortedSet(); }
    @Override public ImmutableSet<Node> asImmutableSet() { return this.delegate.asImmutableSet(); }
    @Override public Map<ImmutableContextSet, Collection<Node>> asMap() { return this.delegate.asMap(); }
    @Override public List<InheritanceNode> inheritanceAsList() { return this.delegate.inheritanceAsList(); }
    @Override public LinkedHashSet<InheritanceNode> inheritanceAsSet() { return this.delegate.inheritanceAsSet(); }
    @Override public SortedSet<InheritanceNode> inheritanceAsSortedSet() { return this.delegate.inheritanceAsSortedSet(); }
    @Override public ImmutableSet<InheritanceNode> inheritanceAsImmutableSet() { return this.delegate.inheritanceAsImmutableSet(); }
    @Override public Map<ImmutableContextSet, Collection<InheritanceNode>> inheritanceAsMap() { return this.delegate.inheritanceAsMap(); }
    @Override public void forEach(Consumer<? super Node> consumer) { this.delegate.forEach(consumer); }
    @Override public void forEach(QueryOptions filter, Consumer<? super Node> consumer) { this.delegate.forEach(filter, consumer); }
    @Override public void copyTo(Collection<? super Node> collection) { this.delegate.copyTo(collection); }
    @Override public void copyTo(ImmutableCollection.Builder<? super Node> collection) { this.delegate.copyTo(collection); }
    @Override public void copyTo(Collection<? super Node> collection, QueryOptions filter) { this.delegate.copyTo(collection, filter); }
    @Override public <T extends Node> void copyTo(Collection<? super T> collection, NodeType<T> type, QueryOptions filter) { this.delegate.copyTo(collection, type, filter); }
    @Override public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection) { this.delegate.copyInheritanceNodesTo(collection); }
    @Override public void copyInheritanceNodesTo(ImmutableCollection.Builder<? super InheritanceNode> collection) { this.delegate.copyInheritanceNodesTo(collection); }
    @Override public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection, QueryOptions filter) { this.delegate.copyInheritanceNodesTo(collection, filter); }
    @Override public Collection<Node> nodesInContext(ContextSet context) { return this.delegate.nodesInContext(context); }
    @Override public Collection<InheritanceNode> inheritanceNodesInContext(ContextSet context) { return this.delegate.inheritanceNodesInContext(context); }
    
}
