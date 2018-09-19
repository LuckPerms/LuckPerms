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

package me.lucko.luckperms.common.api.delegates.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.NodeEqualityPredicate;
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.TemporaryDataMutateResult;
import me.lucko.luckperms.api.TemporaryMergeBehaviour;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.comparator.NodeWithContextComparator;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.utils.MetaType;
import me.lucko.luckperms.common.node.utils.NodeTools;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class ApiPermissionHolder implements me.lucko.luckperms.api.PermissionHolder {
    private final PermissionHolder handle;

    ApiPermissionHolder(PermissionHolder handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    PermissionHolder getHandle() {
        return this.handle;
    }

    @Override
    public @NonNull String getObjectName() {
        return this.handle.getObjectName();
    }

    @Override
    public @NonNull String getFriendlyName() {
        return this.handle.getPlainDisplayName();
    }

    @Override
    public @NonNull CachedData getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    public @NonNull CompletableFuture<Void> refreshCachedData() {
        return CompletableFuture.runAsync(() -> this.handle.getCachedData().invalidate());
    }

    @Override
    public @NonNull ImmutableSetMultimap<ImmutableContextSet, Node> getNodes() {
        //noinspection unchecked
        return (ImmutableSetMultimap) this.handle.enduringData().immutable();
    }

    @Override
    public @NonNull ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes() {
        //noinspection unchecked
        return (ImmutableSetMultimap) this.handle.transientData().immutable();
    }

    @Override
    public @NonNull List<Node> getOwnNodes() {
        return ImmutableList.copyOf(this.handle.getOwnNodes());
    }

    @Override
    public @NonNull SortedSet<? extends Node> getPermissions() {
        return ImmutableSortedSet.copyOfSorted(this.handle.getOwnNodesSorted());
    }

    @Override
    public @NonNull Set<Node> getEnduringPermissions() {
        return ImmutableSet.copyOf(this.handle.enduringData().immutable().values());
    }

    @Override
    public @NonNull Set<Node> getTransientPermissions() {
        return ImmutableSet.copyOf(this.handle.transientData().immutable().values());
    }

    @Override
    public @NonNull SortedSet<LocalizedNode> getAllNodes(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        List<LocalizedNode> nodes = new LinkedList<>();
        this.handle.accumulateInheritancesTo(nodes, contexts);
        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);

        return ImmutableSortedSet.copyOfSorted(ret);
    }

    @Override
    public @NonNull SortedSet<LocalizedNode> getAllNodes() {
        List<LocalizedNode> nodes = new LinkedList<>();
        this.handle.accumulateInheritancesTo(nodes);
        NodeTools.removeEqual(nodes.iterator(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);

        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(nodes);

        return ImmutableSortedSet.copyOfSorted(ret);
    }

    @Override
    public @NonNull Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");

        List<LocalizedNode> entries = this.handle.getAllEntries(contexts);

        NodeTools.removeSamePermission(entries.iterator());
        SortedSet<LocalizedNode> ret = new TreeSet<>(NodeWithContextComparator.reverse());
        ret.addAll(entries);

        return ImmutableSet.copyOf(ret);
    }

    @Override
    public @NonNull Map<String, Boolean> exportNodes(@NonNull Contexts contexts, boolean lowerCase) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableMap.copyOf(this.handle.exportPermissions(contexts, lowerCase, true));
    }

    @Override
    public @NonNull Tristate hasPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.hasPermission(NodeMapType.ENDURING, node, equalityPredicate);
    }

    @Override
    public @NonNull Tristate hasTransientPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.hasPermission(NodeMapType.TRANSIENT, node, equalityPredicate);
    }

    @Override
    public @NonNull Tristate inheritsPermission(@NonNull Node node, @NonNull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.inheritsPermission(node, equalityPredicate);
    }

    @Override
    public @NonNull Tristate hasPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.hasPermission(NodeMapType.ENDURING, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    @Override
    public @NonNull Tristate hasTransientPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.hasPermission(NodeMapType.TRANSIENT, node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    @Override
    public @NonNull Tristate inheritsPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.inheritsPermission(node, StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE);
    }

    @Override
    public boolean inheritsGroup(me.lucko.luckperms.api.@NonNull Group group) {
        Objects.requireNonNull(group, "group");

        Group g = ApiGroup.cast(group);
        if (this.handle.getType().isGroup() && g.getName().equals(this.handle.getObjectName())) {
            return true;
        }

        return this.handle.hasPermission(NodeMapType.ENDURING, NodeFactory.buildGroupNode(g.getName()).build(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE).asBoolean();
    }

    @Override
    public boolean inheritsGroup(me.lucko.luckperms.api.@NonNull Group group, @NonNull ContextSet contextSet) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(contextSet, "contextSet");

        Group g = ApiGroup.cast(group);
        if (this.handle.getType().isGroup() && g.getName().equals(this.handle.getObjectName())) {
            return true;
        }

        return this.handle.hasPermission(NodeMapType.ENDURING, NodeFactory.buildGroupNode(g.getName()).withExtraContext(contextSet).build(), StandardNodeEquality.IGNORE_EXPIRY_TIME_AND_VALUE).asBoolean();
    }

    @Override
    public @NonNull DataMutateResult setPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.setPermission(node);
    }

    @Override
    public @NonNull TemporaryDataMutateResult setPermission(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(temporaryMergeBehaviour, "temporaryMergeBehaviour");
        return this.handle.setPermission(node, temporaryMergeBehaviour);
    }

    @Override
    public @NonNull DataMutateResult setTransientPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.setTransientPermission(node);
    }

    @Override
    public @NonNull TemporaryDataMutateResult setTransientPermission(@NonNull Node node, @NonNull TemporaryMergeBehaviour temporaryMergeBehaviour) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(temporaryMergeBehaviour, "temporaryMergeBehaviour");
        return this.handle.setTransientPermission(node, temporaryMergeBehaviour);
    }

    @Override
    public @NonNull DataMutateResult unsetPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.unsetPermission(node);
    }

    @Override
    public @NonNull DataMutateResult unsetTransientPermission(@NonNull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.unsetTransientPermission(node);
    }

    @Override
    public void clearMatching(@NonNull Predicate<Node> test) {
        Objects.requireNonNull(test, "test");
        this.handle.removeIf(test);
        if (this.handle.getType().isUser()) {
            this.handle.getPlugin().getUserManager().giveDefaultIfNeeded((User) this.handle, false);
        }
    }

    @Override
    public void clearMatchingTransient(@NonNull Predicate<Node> test) {
        Objects.requireNonNull(test, "test");
        this.handle.removeIfTransient(test);
    }

    @Override
    public void clearNodes() {
        this.handle.clearNodes();
    }

    @Override
    public void clearNodes(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearNodes(contextSet);
    }

    @Override
    public void clearParents() {
        this.handle.clearParents(true);
    }

    @Override
    public void clearParents(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearParents(contextSet, true);
    }

    @Override
    public void clearMeta() {
        this.handle.clearMeta(MetaType.ANY);
    }

    @Override
    public void clearMeta(@NonNull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearMeta(MetaType.ANY, contextSet);
    }

    @Override
    public void clearTransientNodes() {
        this.handle.clearTransientNodes();
    }

    @Override
    public @NonNull List<LocalizedNode> resolveInheritances(@NonNull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableList.copyOf(this.handle.resolveInheritances(contexts));
    }

    @Override
    public @NonNull List<LocalizedNode> resolveInheritances() {
        return ImmutableList.copyOf(this.handle.resolveInheritances());
    }

    @Override
    public @NonNull Set<Node> getPermanentPermissionNodes() {
        return this.handle.getOwnNodes().stream().filter(Node::isPermanent).collect(ImmutableCollectors.toSet());
    }

    @Override
    public @NonNull Set<Node> getTemporaryPermissionNodes() {
        return this.handle.getOwnNodes().stream().filter(Node::isPrefix).collect(ImmutableCollectors.toSet());
    }

    @Override
    public void auditTemporaryPermissions() {
        this.handle.auditTemporaryPermissions();
    }

}
