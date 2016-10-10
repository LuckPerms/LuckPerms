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

package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import me.lucko.luckperms.api.*;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.*;

import static me.lucko.luckperms.api.implementation.internal.Utils.*;
import static me.lucko.luckperms.core.PermissionHolder.exportToLegacy;

/**
 * Provides a link between {@link PermissionHolder} and {@link me.lucko.luckperms.core.PermissionHolder}
 */
@SuppressWarnings("unused")
@AllArgsConstructor
public class PermissionHolderLink implements PermissionHolder {

    @NonNull
    private final me.lucko.luckperms.core.PermissionHolder master;

    @Override
    public String getObjectName() {
        return master.getObjectName();
    }

    @Override
    public SortedSet<? extends Node> getPermissions() {
        return master.getPermissions(false);
    }

    @Override
    public Set<Node> getEnduringPermissions() {
        return master.getNodes();
    }

    @Override
    public Set<Node> getTransientPermissions() {
        return master.getTransientNodes();
    }

    @Override
    public Set<Node> getAllNodes() {
        return Collections.unmodifiableSet(master.getAllNodes(new ArrayList<>(), Contexts.allowAll()));
    }

    @Override
    public SortedSet<LocalizedNode> getAllNodes(@NonNull Contexts contexts) {
        return master.getAllNodes(new ArrayList<>(), contexts);
    }

    @Override
    public Set<LocalizedNode> getAllNodesFiltered(@NonNull Contexts contexts) {
        return master.getAllNodesFiltered(contexts);
    }

    @Override
    public Map<String, Boolean> getNodes() {
        return exportToLegacy(master.getNodes());
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
        return master.hasPermission(node, b, checkServer(server));
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.hasPermission(node, b, checkServer(server), world);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.hasPermission(node, b, temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.hasPermission(node, b, checkServer(server), temporary);
    }

    @Override
    public boolean hasPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.hasPermission(node, b, checkServer(server), world, temporary);
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
        return master.inheritsPermission(node, b, checkServer(server));
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world) {
        return master.inheritsPermission(node, b, checkServer(server), world);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, checkServer(server), temporary);
    }

    @Override
    public boolean inheritsPermission(@NonNull String node, @NonNull boolean b, @NonNull String server, @NonNull String world, @NonNull boolean temporary) {
        return master.inheritsPermission(node, b, checkServer(server), world, temporary);
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
        master.setPermission(checkNode(node), value);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), world);
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkTime(expireAt));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), checkTime(expireAt));
    }

    @Override
    public void setPermission(@NonNull String node, @NonNull boolean value, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        master.setPermission(checkNode(node), value, checkServer(server), world, checkTime(expireAt));
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
        master.unsetPermission(checkNode(node), temporary);
    }

    @Override
    public void unsetPermission(@NonNull String node) throws ObjectLacksException {
        master.unsetPermission(checkNode(node));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server));
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), world);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), temporary);
    }

    @Override
    public void unsetPermission(@NonNull String node, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        master.unsetPermission(checkNode(node), checkServer(server), world, temporary);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups, List<String> possibleNodes) {
        Map<String, String> context = new HashMap<>();
        if (server != null && !server.equals("")) {
            context.put("server", server);
        }
        if (world != null && !world.equals("")) {
            context.put("world", world);
        }
        return master.exportNodes(new Contexts(context, true, true, true, true, true), Collections.emptyList(), false);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, String world, List<String> excludedGroups) {
        Map<String, String> context = new HashMap<>();
        if (server != null && !server.equals("")) {
            context.put("server", server);
        }
        if (world != null && !world.equals("")) {
            context.put("world", world);
        }
        return master.exportNodes(new Contexts(context, true, true, true, true, true), Collections.emptyList(), false);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups, List<String> possibleNodes) {
        return getLocalPermissions(server, null, excludedGroups, possibleNodes);
    }

    @Override
    public Map<String, Boolean> getLocalPermissions(String server, List<String> excludedGroups) {
        return getLocalPermissions(server, null, excludedGroups, null);
    }

    @Override
    public Map<String, Boolean> getPermissions(String server, String world, Map<String, String> extraContext, boolean includeGlobal, List<String> possibleNodes, boolean applyGroups) {
        if (extraContext == null) {
            extraContext = new HashMap<>();
        }
        if (server != null && !server.equals("")) {
            extraContext.put("server", server);
        }
        if (world != null && !world.equals("")) {
            extraContext.put("world", world);
        }
        return master.exportNodes(new Contexts(extraContext, includeGlobal, includeGlobal, applyGroups, true, true), possibleNodes, false);
    }

    @Override
    public Map<Map.Entry<String, Boolean>, Long> getTemporaryNodes() {
        Map<Map.Entry<String, Boolean>, Long> m = new HashMap<>();

        for (Node node : master.getTemporaryNodes()) {
            m.put(new AbstractMap.SimpleEntry<>(node.getKey(), node.getValue()), node.getExpiryUnixTime());
        }

        return m;
    }

    @Override
    public Set<Node> getTemporaryPermissionNodes() {
        return master.getTemporaryNodes();
    }

    @Override
    public Map<String, Boolean> getPermanentNodes() {
        return exportToLegacy(master.getPermanentNodes());
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
