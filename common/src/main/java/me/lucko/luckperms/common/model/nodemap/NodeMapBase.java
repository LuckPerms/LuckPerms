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

import net.luckperms.api.context.ContextSatisfyMode;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.query.Flag;
import net.luckperms.api.query.QueryOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.function.Consumer;

/**
 * Base implementation of {@link NodeMap} query methods.
 */
abstract class NodeMapBase implements NodeMap {
   
    NodeMapBase() {

    }
    
    protected abstract SortedMap<ImmutableContextSet, SortedSet<Node>> map();

    protected abstract SortedMap<ImmutableContextSet, SortedSet<InheritanceNode>> inheritanceMap();

    protected abstract ContextSatisfyMode defaultSatisfyMode();

    @Override
    public boolean isEmpty() {
        return map().isEmpty();
    }

    @Override
    public int size() {
        int size = 0;
        for (SortedSet<Node> values : map().values()) {
            size += values.size();
        }
        return size;
    }

    @Override
    public Map<ImmutableContextSet, Collection<Node>> asMap() {
        Map<ImmutableContextSet, Collection<Node>> map = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : map().entrySet()) {
            map.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return map;
    }

    @Override
    public Map<ImmutableContextSet, Collection<InheritanceNode>> inheritanceAsMap() {
        Map<ImmutableContextSet, Collection<InheritanceNode>> map = new HashMap<>();
        for (Map.Entry<ImmutableContextSet, SortedSet<InheritanceNode>> e : inheritanceMap().entrySet()) {
            map.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return map;
    }

    @Override
    public void forEach(Consumer<? super Node> consumer) {
        for (SortedSet<Node> values : map().values()) {
            values.forEach(consumer);
        }
    }

    @Override
    public void forEach(QueryOptions filter, Consumer<? super Node> consumer) {
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : map().entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                    SortedSet<InheritanceNode> inheritanceNodes = inheritanceMap().get(e.getKey());
                    if (inheritanceNodes != null) {
                        inheritanceNodes.forEach(consumer);
                    }
                }
            } else {
                e.getValue().forEach(consumer);
            }
        }
    }

    @Override
    public void copyTo(Collection<? super Node> collection) {
        for (SortedSet<Node> values : map().values()) {
            collection.addAll(values);
        }
    }

    @Override
    public void copyTo(ImmutableCollection.Builder<? super Node> collection) {
        for (SortedSet<Node> values : map().values()) {
            collection.addAll(values);
        }
    }

    @Override
    public void copyTo(Collection<? super Node> collection, QueryOptions filter) {
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : map().entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                    SortedSet<InheritanceNode> inheritanceNodes = inheritanceMap().get(e.getKey());
                    if (inheritanceNodes != null) {
                        collection.addAll(inheritanceNodes);
                    }
                }
            } else {
                collection.addAll(e.getValue());
            }
        }
    }

    @Override
    public <T extends Node> void copyTo(Collection<? super T> collection, NodeType<T> type, QueryOptions filter) {
        if (type == NodeType.INHERITANCE) {
            //noinspection unchecked
            copyInheritanceNodesTo((Collection<? super InheritanceNode>) collection, filter);
            return;
        }
        
        for (Map.Entry<ImmutableContextSet, SortedSet<Node>> e : map().entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (normalNodesExcludeTest(filter, e.getKey())) {
                continue;
            }

            for (Node node : e.getValue()) {
                if (type.matches(node)) {
                    collection.add(type.cast(node));
                }
            }
        }
    }

    @Override
    public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection) {
        for (SortedSet<InheritanceNode> values : inheritanceMap().values()) {
            collection.addAll(values);
        }
    }

    @Override
    public void copyInheritanceNodesTo(ImmutableCollection.Builder<? super InheritanceNode> collection) {
        for (SortedSet<InheritanceNode> values : inheritanceMap().values()) {
            collection.addAll(values);
        }
    }

    @Override
    public void copyInheritanceNodesTo(Collection<? super InheritanceNode> collection, QueryOptions filter) {
        for (Map.Entry<ImmutableContextSet, SortedSet<InheritanceNode>> e : inheritanceMap().entrySet()) {
            if (!filter.satisfies(e.getKey(), defaultSatisfyMode())) {
                continue;
            }

            if (inheritanceNodesIncludeTest(filter, e.getKey())) {
                collection.addAll(e.getValue());
            }
        }
    }

    @Override
    public Collection<Node> nodesInContext(ContextSet context) {
        return copy(map().get(context.immutableCopy()));
    }

    @Override
    public Collection<InheritanceNode> inheritanceNodesInContext(ContextSet context) {
        return copy(inheritanceMap().get(context.immutableCopy()));
    }

    private static <T> Collection<T> copy(Collection<T> collection) {
        if (collection == null) {
            return Collections.emptySet();
        }
        return new ArrayList<>(collection);
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

}
