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

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.common.node.NodeFactory;

import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nonnull;

public final class ApiUser extends ApiPermissionHolder implements User {
    public static me.lucko.luckperms.common.model.User cast(User u) {
        Preconditions.checkState(u instanceof ApiUser, "Illegal instance " + u.getClass() + " cannot be handled by this implementation.");
        return ((ApiUser) u).getHandle();
    }

    private final me.lucko.luckperms.common.model.User handle;

    @Override
    me.lucko.luckperms.common.model.User getHandle() {
        return this.handle;
    }

    public ApiUser(me.lucko.luckperms.common.model.User handle) {
        super(handle);
        this.handle = handle;
    }

    @Nonnull
    @Override
    public UUID getUuid() {
        return this.handle.getUuid();
    }

    @Override
    public String getName() {
        return this.handle.getName().orElse(null);
    }

    @Nonnull
    @Override
    public String getPrimaryGroup() {
        return this.handle.getPrimaryGroup().getValue();
    }

    @Override
    public DataMutateResult setPrimaryGroup(@Nonnull String group) {
        Objects.requireNonNull(group, "group");
        if (getPrimaryGroup().equalsIgnoreCase(group)) {
            return DataMutateResult.ALREADY_HAS;
        }

        if (!this.handle.hasPermission(NodeFactory.buildGroupNode(group.toLowerCase()).build()).asBoolean()) {
            return DataMutateResult.FAIL;
        }

        this.handle.getPrimaryGroup().setStoredValue(group.toLowerCase());
        return DataMutateResult.SUCCESS;
    }

    @Nonnull
    @Override
    public UserData getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    @Deprecated
    public void refreshPermissions() {
        this.handle.getRefreshBuffer().requestDirectly();
    }

    @Override
    @Deprecated
    public void setupDataCache() {
        this.handle.preCalculateData();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiUser)) return false;
        ApiUser that = (ApiUser) o;
        return this.handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }
}
