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

import lombok.Getter;
import lombok.NonNull;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static me.lucko.luckperms.common.api.ApiUtils.checkGroup;
import static me.lucko.luckperms.common.api.ApiUtils.checkTime;

/**
 * Provides a link between {@link User} and {@link me.lucko.luckperms.common.core.model.User}
 */
public final class UserDelegate extends PermissionHolderDelegate implements User {

    @Getter
    private final me.lucko.luckperms.common.core.model.User master;

    public UserDelegate(@NonNull me.lucko.luckperms.common.core.model.User master) {
        super(master);
        this.master = master;
    }

    @Override
    public UUID getUuid() {
        return master.getUuid();
    }

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public String getPrimaryGroup() {
        return master.getPrimaryGroup().getValue();
    }

    @Override
    public void setPrimaryGroup(@NonNull String s) throws ObjectAlreadyHasException {
        if (getPrimaryGroup().equalsIgnoreCase(s)) {
            throw new ObjectAlreadyHasException();
        }

        if (!getGroupNames().contains(s.toLowerCase())) {
            throw new IllegalStateException("User is not a member of that group.");
        }

        master.getPrimaryGroup().setStoredValue(s.toLowerCase());
    }

    @Override
    public void refreshPermissions() {
        master.getRefreshBuffer().requestDirectly();
    }

    @Override
    public Optional<UserData> getUserDataCache() {
        return Optional.ofNullable(master.getUserData());
    }

    @Override
    public void setupDataCache() {
        master.setupData(false);
    }

    @Override
    public boolean isInGroup(@NonNull Group group) {
        checkGroup(group);
        return master.inheritsGroup(((GroupDelegate) group).getMaster());
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull String server) {
        checkGroup(group);
        return master.inheritsGroup(((GroupDelegate) group).getMaster(), server);
    }

    @Override
    public boolean isInGroup(@NonNull Group group, @NonNull String server, @NonNull String world) {
        checkGroup(group);
        return master.inheritsGroup(((GroupDelegate) group).getMaster(), server, world);
    }

    @Override
    public void addGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster());
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster(), server);
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster(), server, world);
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster(), checkTime(expireAt));
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster(), server, checkTime(expireAt));
    }

    @Override
    public void addGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupDelegate) group).getMaster(), server, world, checkTime(expireAt));
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster());
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster(), temporary);
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster(), server);
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster(), server, world);
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster(), server, temporary);
    }

    @Override
    public void removeGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupDelegate) group).getMaster(), server, world, temporary);
    }

    @Override
    public void clearNodes() {
        master.clearNodes();
    }

    @Override
    public List<String> getGroupNames() {
        return master.getGroupNames();
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server, @NonNull String world) {
        return master.getLocalGroups(server, world);
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server) {
        return master.getLocalGroups(server);
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
