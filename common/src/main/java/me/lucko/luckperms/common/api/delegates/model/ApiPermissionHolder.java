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

import lombok.AllArgsConstructor;
import lombok.NonNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.caching.CachedData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.MetaType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ApiPermissionHolder implements PermissionHolder {
    private final me.lucko.luckperms.common.model.PermissionHolder handle;

    @Override
    public String getObjectName() {
        return handle.getObjectName();
    }

    @Override
    public String getFriendlyName() {
        if (handle.getType().isGroup()) {
            Group group = (Group) this.handle;
            return group.getDisplayName().orElse(group.getName());
        }
        return handle.getFriendlyName();
    }

    @Override
    public CachedData getCachedData() {
        return handle.getCachedData();
    }

    @Override
    public CompletableFuture<Void> refreshCachedData() {
        return handle.getRefreshBuffer().request();
    }

    @Override
    public ImmutableSetMultimap<ImmutableContextSet, Node> getNodes() {
        return handle.getEnduringNodes();
    }

    @Override
    public ImmutableSetMultimap<ImmutableContextSet, Node> getTransientNodes() {
        return handle.getTransientNodes();
    }

    @Override
    public List<Node> getOwnNodes() {
        return handle.getOwnNodes();
    }

    @Override
    public SortedSet<? extends Node> getPermissions() {
        return ImmutableSortedSet.copyOfSorted(handle.getOwnNodesSorted());
    }

    @Override
    public Set<Node> getEnduringPermissions() {
        return ImmutableSet.copyOf(handle.getEnduringNodes().values());
    }

    @Override
    public Set<Node> getTransientPermissions() {
        return ImmutableSet.copyOf(handle.getTransientNodes().values());
    }

    @Override
    public SortedSet<LocalizedNode> getAllNodes(@NonNull Contexts contexts) {
        return new TreeSet<>(handle.resolveInheritancesAlmostEqual(contexts));
    }

    @Override
    public SortedSet<LocalizedNode> getAllNodes() {
        return new TreeSet<>(handle.resolveInheritancesAlmostEqual());
    }

    @Override
    public Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts) {
        return new HashSet<>(handle.getAllNodes(contexts));
    }

    @Override
    public Map<String, Boolean> exportNodes(Contexts contexts, boolean lowerCase) {
        return new HashMap<>(handle.exportNodesAndShorthand(contexts, lowerCase));
    }

    @Override
    public Tristate hasPermission(@NonNull Node node) {
        return handle.hasPermission(node, false);
    }

    @Override
    public Tristate hasTransientPermission(@NonNull Node node) {
        return handle.hasPermission(node, true);
    }

    @Override
    public Tristate inheritsPermission(@NonNull Node node) {
        return handle.inheritsPermission(node);
    }

    @Override
    public boolean inheritsGroup(@NonNull me.lucko.luckperms.api.Group group) {
        return handle.inheritsGroup(ApiGroup.cast(group));
    }

    @Override
    public boolean inheritsGroup(@NonNull me.lucko.luckperms.api.Group group, @NonNull ContextSet contextSet) {
        return handle.inheritsGroup(ApiGroup.cast(group), contextSet);
    }

    @Override
    public DataMutateResult setPermission(@NonNull Node node) {
        return handle.setPermission(node);
    }

    @Override
    public DataMutateResult setTransientPermission(@NonNull Node node) {
        return handle.setTransientPermission(node);
    }

    @Override
    public DataMutateResult unsetPermission(@NonNull Node node) {
        return handle.unsetPermission(node);
    }

    @Override
    public DataMutateResult unsetTransientPermission(@NonNull Node node) {
        return handle.unsetTransientPermission(node);
    }

    @Override
    public void clearMatching(@NonNull Predicate<Node> test) {
        handle.removeIf(test);
        if (handle.getType().isUser()) {
            handle.getPlugin().getUserManager().giveDefaultIfNeeded((User) handle, false);
        }
    }

    @Override
    public void clearMatchingTransient(@NonNull Predicate<Node> test) {
        handle.removeIfTransient(test);
    }

    @Override
    public void clearNodes() {
        handle.clearNodes();
    }

    @Override
    public void clearNodes(@NonNull ContextSet contextSet) {
        handle.clearNodes(contextSet);
    }

    @Override
    public void clearParents() {
        handle.clearParents(true);
    }

    @Override
    public void clearParents(@NonNull ContextSet contextSet) {
        handle.clearParents(contextSet, true);
    }

    @Override
    public void clearMeta() {
        handle.clearMeta(MetaType.ANY);
    }

    @Override
    public void clearMeta(@NonNull ContextSet contextSet) {
        handle.clearMeta(MetaType.ANY, contextSet);
    }

    @Override
    public void clearTransientNodes() {
        handle.clearTransientNodes();
    }

    @Override
    public List<LocalizedNode> resolveInheritances(Contexts contexts) {
        return handle.resolveInheritances(contexts);
    }

    @Override
    public List<LocalizedNode> resolveInheritances() {
        return handle.resolveInheritances();
    }

    @Override
    public Set<Node> getPermanentPermissionNodes() {
        return handle.getOwnNodes().stream().filter(Node::isPermanent).collect(Collectors.toSet());
    }

    @Override
    public Set<Node> getTemporaryPermissionNodes() {
        return handle.getOwnNodes().stream().filter(Node::isPrefix).collect(Collectors.toSet());
    }

    @Override
    public void auditTemporaryPermissions() {
        handle.auditTemporaryPermissions();
    }

}
