/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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
import com.google.common.collect.ImmutableSortedSet;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LocalizedNode;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.utils.ExtractedContexts;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static me.lucko.luckperms.common.api.ApiUtils.checkTime;

/**
 * Provides a link between {@link PermissionHolder} and {@link me.lucko.luckperms.common.core.model.PermissionHolder}
 */
@AllArgsConstructor
public class PermissionHolderDelegate implements PermissionHolder {
    private final me.lucko.luckperms.common.core.model.PermissionHolder master;

    @Override
    public String getObjectName() {
        return master.getObjectName();
    }

    @Override
    public SortedSet<? extends Node> getPermissions() {
        return ImmutableSortedSet.copyOfSorted(master.mergePermissionsToSortedSet());
    }

    @Override
    public Set<Node> getEnduringPermissions() {
        return ImmutableSet.copyOf(master.getNodes().values());
    }

    @Override
    public Set<Node> getTransientPermissions() {
        return ImmutableSet.copyOf(master.getTransientNodes().values());
    }

    @Override
    public SortedSet<LocalizedNode> getAllNodes(@NonNull Contexts contexts) {
        return new TreeSet<>(master.resolveInheritancesAlmostEqual(ExtractedContexts.generate(contexts)));
    }

    @Override
    public Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts) {
        return new HashSet<>(master.getAllNodes(ExtractedContexts.generate(contexts)));
    }

    @Override
    public Map<String, Boolean> exportNodes(Contexts contexts, boolean lowerCase) {
        return new HashMap<>(master.exportNodes(ExtractedContexts.generate(contexts), lowerCase));
    }

    @Override
    public Tristate hasPermission(@NonNull Node node) {
        return master.hasPermission(node, false);
    }

    @Override
    public Tristate hasTransientPermission(@NonNull Node node) {
        return master.hasPermission(node, true);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b) {
        return master.hasPermission(node, b);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return master.hasPermission(node, b, server);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.hasPermission(node, b, server, world);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.hasPermission(node, b, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.hasPermission(node, b, server, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.hasPermission(node, b, server, world, temporary);
    }

    @Override
    public Tristate inheritsPermission(@NonNull Node node) {
        return master.inheritsPermission(node);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b) {
        return master.inheritsPermission(node, b);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server) {
        return master.inheritsPermission(node, b, server);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.inheritsPermission(node, b, server, world);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, server, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, server, world, temporary);
    }

    @Override
    public void setPermission(@NonNull Node node) throws ObjectAlreadyHasException {
        master.setPermission(node);
    }

    @Override
    public void setTransientPermission(@NonNull Node node) throws ObjectAlreadyHasException {
        master.setTransientPermission(node);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value, server));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value, server, world));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value, checkTime(expireAt)));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value, server, checkTime(expireAt)));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(NodeFactory.make(node, value, server, world, checkTime(expireAt)));
    }

    @Override
    public void unsetPermission(@NonNull Node node) throws ObjectLacksException {
        master.unsetPermission(node);
    }

    @Override
    public void unsetTransientPermission(@NonNull Node node) throws ObjectLacksException {
        master.unsetTransientPermission(node);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node, temporary));
    }

    @Override
    public void unsetPermission(@NonNull String node) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node, server));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node, server, world));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node, server, temporary));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(NodeFactory.make(node, server, world, temporary));
    }

    @Override
    public void clearNodes() {
        master.clearNodes();
    }

    @Override
    public void clearNodes(String server) {
        master.clearNodes(server);
    }

    @Override
    public void clearNodes(String server, String world) {
        master.clearNodes(server, world);
    }

    @Override
    public void clearParents() {
        master.clearParents();
    }

    @Override
    public void clearParents(String server) {
        master.clearParents(server);
    }

    @Override
    public void clearParents(String server, String world) {
        master.clearParents(server, world);
    }

    @Override
    public void clearMeta() {
        master.clearMeta();
    }

    @Override
    public void clearMeta(String server) {
        master.clearMeta(server);
    }

    @Override
    public void clearMeta(String server, String world) {
        master.clearMeta(server, world);
    }

    @Override
    public void clearMetaKeys(String key, String server, String world, boolean temporary) {
        master.clearMetaKeys(key, server, world, temporary);
    }

    @Override
    public void clearTransientNodes() {
        master.clearTransientNodes();
    }

    @Override
    public Set<Node> getTemporaryPermissionNodes() {
        return master.getTemporaryNodes();
    }

    @Override
    public Set<Node> getPermanentPermissionNodes() {
        return master.getPermanentNodes();
    }

    @Override
    public void auditTemporaryPermissions() {
        master.auditTemporaryPermissions();
    }

}
