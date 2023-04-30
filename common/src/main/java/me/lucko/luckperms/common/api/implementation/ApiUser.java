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

package me.lucko.luckperms.common.api.implementation;

import com.google.common.base.Preconditions;
import me.lucko.luckperms.common.cacheddata.UserCachedDataManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeEqualityPredicate;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class ApiUser extends ApiPermissionHolder implements net.luckperms.api.model.user.User {
    public static User cast(net.luckperms.api.model.user.User u) {
        Preconditions.checkState(u instanceof ApiUser, "Illegal instance " + u.getClass() + " cannot be handled by this implementation.");
        return ((ApiUser) u).getHandle();
    }

    private final User handle;

    public ApiUser(User handle) {
        super(handle);
        this.handle = handle;
    }

    @Override
    User getHandle() {
        return this.handle;
    }

    @Override
    public @NonNull UUID getUniqueId() {
        return this.handle.getUniqueId();
    }

    @Override
    public String getUsername() {
        return this.handle.getUsername().orElse(null);
    }

    @Override
    public @NonNull String getPrimaryGroup() {
        String value = this.handle.getCachedData().getMetaData(this.handle.getQueryOptions()).getPrimaryGroup(CheckOrigin.LUCKPERMS_API);
        Objects.requireNonNull(value, "value"); // assert nonnull
        return value;
    }

    @Override
    public @NonNull DataMutateResult setPrimaryGroup(@NonNull String group) {
        Objects.requireNonNull(group, "group");
        if (group.equalsIgnoreCase(this.handle.getPrimaryGroup().getStoredValue().orElse(null))) {
            return DataMutateResult.FAIL_ALREADY_HAS;
        }

        if (!this.handle.hasNode(DataType.NORMAL, Inheritance.builder(group.toLowerCase(Locale.ROOT)).build(), NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE).asBoolean()) {
            return DataMutateResult.FAIL;
        }

        this.handle.getPrimaryGroup().setStoredValue(group.toLowerCase(Locale.ROOT));
        return DataMutateResult.SUCCESS;
    }

    @Override
    public @NonNull UserCachedDataManager getCachedData() {
        return this.handle.getCachedData();
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
