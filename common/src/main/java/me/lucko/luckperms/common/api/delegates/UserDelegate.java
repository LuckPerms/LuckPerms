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

import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.api.ApiUtils.checkTime;

/**
 * Provides a link between {@link User} and {@link me.lucko.luckperms.common.model.User}
 */
public final class UserDelegate extends PermissionHolderDelegate implements User {
    public static me.lucko.luckperms.common.model.User cast(User u) {
        Preconditions.checkState(u instanceof UserDelegate, "Illegal instance " + u.getClass() + " cannot be handled by this implementation.");
        return ((UserDelegate) u).getHandle();
    }

    @Getter
    private final me.lucko.luckperms.common.model.User handle;

    public UserDelegate(@NonNull me.lucko.luckperms.common.model.User handle) {
        super(handle);
        this.handle = handle;
    }

    @Override
    public UUID getUuid() {
        return handle.getUuid();
    }

    @Override
    public String getName() {
        return handle.getName().orElse(null);
    }

    @Override
    public String getPrimaryGroup() {
        return handle.getPrimaryGroup().getValue();
    }

    @Override
    public void setPrimaryGroup(@NonNull String s) throws ObjectAlreadyHasException {
        if (getPrimaryGroup().equalsIgnoreCase(s)) {
            throw new ObjectAlreadyHasException();
        }

        if (!getGroupNames().contains(s.toLowerCase())) {
            throw new IllegalStateException("User is not a member of that group.");
        }

        handle.getPrimaryGroup().setStoredValue(s.toLowerCase());
    }

    @Override
    public void refreshPermissions() {
        handle.getRefreshBuffer().requestDirectly();
    }

    @Override
    public UserData getCachedData() {
        return handle.getUserData();
    }

    @Override
    public Optional<UserData> getUserDataCache() {
        return Optional.of(handle.getUserData());
    }

    @Override
    public void setupDataCache() {
        handle.preCalculateData(false);
    }

    @Override
    public boolean isInGroup(@NonNull Group group) {
        return handle.inheritsGroup(GroupDelegate.cast(group));
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull ContextSet contextSet) {
        return handle.inheritsGroup(GroupDelegate.cast(group), contextSet);
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull String server) {
        return handle.inheritsGroup(GroupDelegate.cast(group), server);
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull String server, @NonNull String world) {
        return handle.inheritsGroup(GroupDelegate.cast(group), server, world);
    }

    @Override
    public void addGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group))).throwException();
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group), server)).throwException();
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group), server, world)).throwException();
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group), checkTime(expireAt))).throwException();
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group), server, checkTime(expireAt))).throwException();
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        handle.setPermission(NodeFactory.make(GroupDelegate.cast(group), server, world, checkTime(expireAt))).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group))).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group), temporary)).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group), server)).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group), server, world)).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group), server, temporary)).throwException();
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        handle.unsetPermission(NodeFactory.make(GroupDelegate.cast(group), server, world, temporary)).throwException();
    }

    @Override
    public void clearNodes() {
        handle.clearNodes();
    }

    @Override
    public List<String> getGroupNames() {
        return handle.getOwnNodes().stream()
                .filter(Node::isGroupNode)
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server, @NonNull String world) {
        return handle.getOwnNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnWorld(world, false, true))
                .filter(n -> n.shouldApplyOnServer(server, false, true))
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server) {
        return handle.getOwnNodes().stream()
                .filter(Node::isGroupNode)
                .filter(n -> n.shouldApplyOnServer(server, false, true))
                .map(Node::getGroupName)
                .collect(Collectors.toList());
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof UserDelegate)) return false;

        UserDelegate other = (UserDelegate) o;
        return this.getUuid() == null ? other.getUuid() == null : this.getUuid().equals(other.getUuid());
    }

    public int hashCode() {
        return this.getUuid().hashCode();
    }
}
