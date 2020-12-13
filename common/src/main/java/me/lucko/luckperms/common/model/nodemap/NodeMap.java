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

import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;

import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A map of nodes held by a {@link PermissionHolder}.
 */
public interface NodeMap {

    boolean isEmpty();

    int size();

    default List<Node> asList() {
        List<Node> list = new ArrayList<>();
        copyTo(list);
        return list;
    }

    default LinkedHashSet<Node> asSet() {
        LinkedHashSet<Node> set = new LinkedHashSet<>();
        copyTo(set);
        return set;
    }

    default SortedSet<Node> asSortedSet() {
        SortedSet<Node> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyTo(set);
        return set;
    }

    default ImmutableSet<Node> asImmutableSet() {
        ImmutableSet.Builder<Node> builder = ImmutableSet.builder();
        copyTo(builder);
        return builder.build();
    }

    Map<ImmutableContextSet, Collection<Node>> asMap();

    default List<InheritanceNode> inheritanceAsList() {
        List<InheritanceNode> set = new ArrayList<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    default LinkedHashSet<InheritanceNode> inheritanceAsSet() {
        LinkedHashSet<InheritanceNode> set = new LinkedHashSet<>();
        copyInheritanceNodesTo(set);
        return set;
    }

    default SortedSet<InheritanceNode> inheritanceAsSortedSet() {
        SortedSet<InheritanceNode> set = new TreeSet<>(NodeWithContextComparator.reverse());
        copyInheritanceNodesTo(set);
        return set;
    }

    default ImmutableSet<InheritanceNode> inheritanceAsImmutableSet() {
        ImmutableSet.Builder<InheritanceNode> builder = ImmutableSet.builder();
        copyInheritanceNodesTo(builder);
        return builder.build();
    }

    Map<ImmutableContextSet, Collection<InheritanceNode>> inheritanceAsMap();

    void forEach(Consumer<? super Node> consumer);

    void forEach(QueryOptions filter, Consumer<? super Node> consumer);

    void copyTo(Collection<? super Node> collection);

    void copyTo(ImmutableCollection.Builder<? super Node> collection);

    void copyTo(Collection<? super Node> collection, QueryOptions filter);

    <T extends Node> void copyTo(Collection<? super T> collection, NodeType<T> type, QueryOptions filter);

    void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection);

    void copyInheritanceNodesTo(ImmutableCollection.Builder<? super InheritanceNode> collection);

    void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection, QueryOptions filter);

    Collection<Node> nodesInContext(ContextSet context);

    Collection<InheritanceNode> inheritanceNodesInContext(ContextSet context);

    // mutate methods

    MutateResult add(Node nodeWithoutInheritanceOrigin);

    MutateResult remove(Node node);

    MutateResult removeExact(Node node);

    MutateResult removeIf(Predicate<? super Node> predicate);

    MutateResult removeIf(ContextSet contextSet, Predicate<? super Node> predicate);

    MutateResult removeThenAdd(Node nodeToRemove, Node nodeToAdd);

    MutateResult clear();

    MutateResult clear(ContextSet contextSet);

    MutateResult setContent(Iterable<? extends Node> set);

    MutateResult setContent(Stream<? extends Node> stream);

    MutateResult addAll(Iterable<? extends Node> set);

    MutateResult addAll(Stream<? extends Node> stream);

}
