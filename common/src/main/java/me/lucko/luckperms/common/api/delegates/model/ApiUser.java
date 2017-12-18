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

import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.common.node.NodeFactory;

import java.util.UUID;

public final class ApiUser extends ApiPermissionHolder implements User {
    public static me.lucko.luckperms.common.model.User cast(User u) {
        Preconditions.checkState(u instanceof ApiUser, "Illegal instance " + u.getClass() + " cannot be handled by this implementation.");
        return ((ApiUser) u).getHandle();
    }

    @Getter
    private final me.lucko.luckperms.common.model.User handle;

    public ApiUser(@NonNull me.lucko.luckperms.common.model.User handle) {
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
    public DataMutateResult setPrimaryGroup(@NonNull String s) {
        if (getPrimaryGroup().equalsIgnoreCase(s)) {
            return DataMutateResult.ALREADY_HAS;
        }

        if (!handle.hasPermission(NodeFactory.buildGroupNode(s.toLowerCase()).build()).asBoolean()) {
            return DataMutateResult.FAIL;
        }

        handle.getPrimaryGroup().setStoredValue(s.toLowerCase());
        return DataMutateResult.SUCCESS;
    }

    @Override
    public UserData getCachedData() {
        return handle.getCachedData();
    }

    @Override
    @Deprecated
    public void refreshPermissions() {
        handle.getRefreshBuffer().requestDirectly();
    }

    @Override
    @Deprecated
    public void setupDataCache() {
        handle.preCalculateData();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiUser)) return false;

        ApiUser other = (ApiUser) o;
        return handle.equals(other.handle);
    }

    public int hashCode() {
        return handle.hashCode();
    }
}
