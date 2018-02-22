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
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.MetaType;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

public class ApiPermissionHolder implements me.lucko.luckperms.api.PermissionHolder {
    private final PermissionHolder handle;

    ApiPermissionHolder(PermissionHolder handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    PermissionHolder getHandle() {
        return this.handle;
    }

    @Nonnull
    @Override
    public String getObjectName() {
        return this.handle.getObjectName();
    }

    @Nonnull
    @Override
    public String getFriendlyName() {
        if (this.handle.getType().isGroup()) {
            Group group = (Group) this.handle;
            return group.getDisplayName().orElse(group.getName());
        }
        return this.handle.getFriendlyName();
    }

    @Nonnull
    @Override
    public CachedData getCachedData() {
        return this.handle.getCachedData();
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> refreshCachedData() {
        return this.handle.getRefreshBuffer().request();
    }

    @Nonnull
    @Override
    public ImmutableSetMultimap<ImmutableContextSet, Node> getNodes() {
        return this.handle.getEnduringNodes();
    }

    @Nonnull
    @Override
    public ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes() {
        return this.handle.getTransientNodes();
    }

    @Nonnull
    @Override
    public List<Node> getOwnNodes() {
        return ImmutableList.copyOf(this.handle.getOwnNodes());
    }

    @Nonnull
    @Override
    public SortedSet<? extends Node> getPermissions() {
        return ImmutableSortedSet.copyOfSorted(this.handle.getOwnNodesSorted());
    }

    @Nonnull
    @Override
    public Set<Node> getEnduringPermissions() {
        return ImmutableSet.copyOf(this.handle.getEnduringNodes().values());
    }

    @Nonnull
    @Override
    public Set<Node> getTransientPermissions() {
        return ImmutableSet.copyOf(this.handle.getTransientNodes().values());
    }

    @Nonnull
    @Override
    public SortedSet<LocalizedNode> getAllNodes(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableSortedSet.copyOfSorted(this.handle.resolveInheritancesAlmostEqual(contexts));
    }

    @Nonnull
    @Override
    public SortedSet<LocalizedNode> getAllNodes() {
        return ImmutableSortedSet.copyOfSorted(this.handle.resolveInheritancesAlmostEqual());
    }

    @Nonnull
    @Override
    public Set<LocalizedNode> getAllNodesFiltered(@Nonnull Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableSet.copyOf(this.handle.getAllNodes(contexts));
    }

    @Nonnull
    @Override
    public Map<String, Boolean> exportNodes(@Nonnull Contexts contexts, boolean lowerCase) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableMap.copyOf(this.handle.exportNodesAndShorthand(contexts, lowerCase));
    }

    @Nonnull
    @Override
    public Tristate hasPermission(@Nonnull Node node, @Nonnull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.hasPermission(NodeMapType.ENDURING, node, equalityPredicate);
    }

    @Nonnull
    @Override
    public Tristate hasTransientPermission(@Nonnull Node node, @Nonnull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.hasPermission(NodeMapType.TRANSIENT, node, equalityPredicate);
    }

    @Nonnull
    @Override
    public Tristate inheritsPermission(@Nonnull Node node, @Nonnull NodeEqualityPredicate equalityPredicate) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(equalityPredicate, "equalityPredicate");
        return this.handle.inheritsPermission(node, equalityPredicate);
    }

    @Nonnull
    @Override
    public Tristate hasPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.hasPermission(NodeMapType.ENDURING, node);
    }

    @Nonnull
    @Override
    public Tristate hasTransientPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.hasPermission(NodeMapType.TRANSIENT, node);
    }

    @Nonnull
    @Override
    public Tristate inheritsPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.inheritsPermission(node);
    }

    @Override
    public boolean inheritsGroup(@Nonnull me.lucko.luckperms.api.Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.inheritsGroup(ApiGroup.cast(group));
    }

    @Override
    public boolean inheritsGroup(@Nonnull me.lucko.luckperms.api.Group group, @Nonnull ContextSet contextSet) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(contextSet, "contextSet");
        return this.handle.inheritsGroup(ApiGroup.cast(group), contextSet);
    }

    @Nonnull
    @Override
    public DataMutateResult setPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.setPermission(node);
    }

    @Nonnull
    @Override
    public DataMutateResult setTransientPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.setTransientPermission(node);
    }

    @Nonnull
    @Override
    public DataMutateResult unsetPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.unsetPermission(node);
    }

    @Nonnull
    @Override
    public DataMutateResult unsetTransientPermission(@Nonnull Node node) {
        Objects.requireNonNull(node, "node");
        return this.handle.unsetTransientPermission(node);
    }

    @Override
    public void clearMatching(@Nonnull Predicate<Node> test) {
        Objects.requireNonNull(test, "test");
        this.handle.removeIf(test);
        if (this.handle.getType().isUser()) {
            this.handle.getPlugin().getUserManager().giveDefaultIfNeeded((User) this.handle, false);
        }
    }

    @Override
    public void clearMatchingTransient(@Nonnull Predicate<Node> test) {
        Objects.requireNonNull(test, "test");
        this.handle.removeIfTransient(test);
    }

    @Override
    public void clearNodes() {
        this.handle.clearNodes();
    }

    @Override
    public void clearNodes(@Nonnull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearNodes(contextSet);
    }

    @Override
    public void clearParents() {
        this.handle.clearParents(true);
    }

    @Override
    public void clearParents(@Nonnull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearParents(contextSet, true);
    }

    @Override
    public void clearMeta() {
        this.handle.clearMeta(MetaType.ANY);
    }

    @Override
    public void clearMeta(@Nonnull ContextSet contextSet) {
        Objects.requireNonNull(contextSet, "contextSet");
        this.handle.clearMeta(MetaType.ANY, contextSet);
    }

    @Override
    public void clearTransientNodes() {
        this.handle.clearTransientNodes();
    }

    @Nonnull
    @Override
    public List<LocalizedNode> resolveInheritances(Contexts contexts) {
        Objects.requireNonNull(contexts, "contexts");
        return ImmutableList.copyOf(this.handle.resolveInheritances(contexts));
    }

    @Nonnull
    @Override
    public List<LocalizedNode> resolveInheritances() {
        return ImmutableList.copyOf(this.handle.resolveInheritances());
    }

    @Nonnull
    @Override
    public Set<Node> getPermanentPermissionNodes() {
        return this.handle.getOwnNodes().stream().filter(Node::isPermanent).collect(ImmutableCollectors.toSet());
    }

    @Nonnull
    @Override
    public Set<Node> getTemporaryPermissionNodes() {
        return this.handle.getOwnNodes().stream().filter(Node::isPrefix).collect(ImmutableCollectors.toSet());
    }

    @Override
    public void auditTemporaryPermissions() {
        this.handle.auditTemporaryPermissions();
    }

}
