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

package me.lucko.luckperms.common.api.delegates;

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
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.contexts.ExtractedContexts;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

import static me.lucko.luckperms.common.api.ApiUtils.checkTime;

/**
 * Provides a link between {@link PermissionHolder} and {@link me.lucko.luckperms.common.model.PermissionHolder}
 */
@AllArgsConstructor
public class PermissionHolderDelegate implements PermissionHolder {
    private final me.lucko.luckperms.common.model.PermissionHolder handle;

    @Override
    public String getObjectName() {
        return handle.getObjectName();
    }

    @Override
    public String getFriendlyName() {
        return handle.getFriendlyName();
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
        return new TreeSet<>(handle.resolveInheritancesAlmostEqual(ExtractedContexts.generate(contexts)));
    }

    @Override
    public SortedSet<LocalizedNode> getAllNodes() {
        return new TreeSet<>(handle.resolveInheritancesAlmostEqual());
    }

    @Override
    public Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts) {
        return new HashSet<>(handle.getAllNodes(ExtractedContexts.generate(contexts)));
    }

    @Override
    public Map<String, Boolean> exportNodes(Contexts contexts, boolean lowerCase) {
        return new HashMap<>(handle.exportNodesAndShorthand(ExtractedContexts.generate(contexts), lowerCase));
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
    public boolean hasPermission(@NonNull String node, @NonNull boolean b) {
        return handle.hasPermission(node, b);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return handle.hasPermission(node, b, server);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return handle.hasPermission(node, b, server, world);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return handle.hasPermission(node, b, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return handle.hasPermission(node, b, server, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return handle.hasPermission(node, b, server, world, temporary);
    }

    @Override
    public Tristate inheritsPermission(@NonNull Node node) {
        return handle.inheritsPermission(node);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b) {
        return handle.inheritsPermission(node, b);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return handle.inheritsPermission(node, b, server);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return handle.inheritsPermission(node, b, server, world);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return handle.inheritsPermission(node, b, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return handle.inheritsPermission(node, b, server, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return handle.inheritsPermission(node, b, server, world, temporary);
    }

    @Override
    public void setPermission(@NonNull Node node) throws ObjectAlreadyHasException {
        handle.setPermission(node).throwException();
    }

    @Override
    public DataMutateResult setPermissionUnchecked(Node node) {
        return handle.setPermission(node);
    }

    @Override
    public void setTransientPermission(@NonNull Node node) throws ObjectAlreadyHasException {
        handle.setTransientPermission(node).throwException();
    }

    @Override
    public DataMutateResult setTransientPermissionUnchecked(Node node) {
        return handle.setTransientPermission(node);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value)).throwException();
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value, server)).throwException();
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value, server, world)).throwException();
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value, checkTime(expireAt))).throwException();
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value, server, checkTime(expireAt))).throwException();
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(node, value, server, world, checkTime(expireAt))).throwException();
    }

    @Override
    public void unsetPermission(@NonNull Node node) throws ObjectLacksException {
        handle.unsetPermission(node).throwException();
    }

    @Override
    public DataMutateResult unsetPermissionUnchecked(Node node) {
        return handle.unsetPermission(node);
    }

    @Override
    public void unsetTransientPermission(@NonNull Node node) throws ObjectLacksException {
        handle.unsetTransientPermission(node).throwException();
    }

    @Override
    public DataMutateResult unsetTransientPermissionUnchecked(Node node) {
        return handle.unsetTransientPermission(node);
    }

    @Override
    public void clearMatching(Predicate<Node> test) {
        handle.removeIf(test);
        if (handle instanceof User) {
            handle.getPlugin().getUserManager().giveDefaultIfNeeded((User) handle, false);
        }
    }

    @Override
    public void clearMatchingTransient(Predicate<Node> test) {
        handle.removeIfTransient(test);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node, temporary)).throwException();
    }

    @Override
    public void unsetPermission(@NonNull String node) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node)).throwException();
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node, server)).throwException();
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node, server, world)).throwException();
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node, server, temporary)).throwException();
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(node, server, world, temporary)).throwException();
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
    public void clearNodes(String server) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }

        handle.clearNodes(set);
    }

    @Override
    public void clearNodes(String server, String world) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }
        if (world != null) {
            set.add("world", world);
        }

        handle.clearNodes(set);
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
    public void clearParents(String server) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }

        handle.clearParents(set, true);
    }

    @Override
    public void clearParents(String server, String world) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }
        if (world != null) {
            set.add("world", world);
        }

        handle.clearParents(set, true);
    }

    @Override
    public void clearMeta() {
        handle.clearMeta();
    }

    @Override
    public void clearMeta(@NonNull ContextSet contextSet) {
        handle.clearMeta(contextSet);
    }

    @Override
    public void clearMeta(String server) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }

        handle.clearMeta(set);
    }

    @Override
    public void clearMeta(String server, String world) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }
        if (world != null) {
            set.add("world", world);
        }

        handle.clearMeta(set);
    }

    @Override
    public void clearMetaKeys(String key, String server, String world, boolean temporary) {
        MutableContextSet set = new MutableContextSet();
        if (server != null) {
            set.add("server", server);
        }
        if (world != null) {
            set.add("world", world);
        }

        handle.clearMetaKeys(key, set, temporary);
    }

    @Override
    public void clearTransientNodes() {
        handle.clearTransientNodes();
    }

    @Override
    public Set<Node> getTemporaryPermissionNodes() {
        return handle.getTemporaryNodes();
    }

    @Override
    public List<LocalizedNode> resolveInheritances(Contexts contexts) {
        return null;
    }

    @Override
    public List<LocalizedNode> resolveInheritances() {
        return null;
    }

    @Override
    public Set<Node> getPermanentPermissionNodes() {
        return handle.getPermanentNodes();
    }

    @Override
    public void auditTemporaryPermissions() {
        handle.auditTemporaryPermissions();
    }

}
