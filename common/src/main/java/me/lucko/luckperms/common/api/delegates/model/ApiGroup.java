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

import me.lucko.luckperms.api.caching.GroupData;
import me.lucko.luckperms.common.model.Group;

import java.util.Objects;
import java.util.OptionalInt;

import javax.annotation.Nonnull;

public final class ApiGroup extends ApiPermissionHolder implements me.lucko.luckperms.api.Group {
    public static Group cast(me.lucko.luckperms.api.Group group) {
        Objects.requireNonNull(group, "group");
        Preconditions.checkState(group instanceof ApiGroup, "Illegal instance " + group.getClass() + " cannot be handled by this implementation.");
        return ((ApiGroup) group).getHandle();
    }

    private final Group handle;

    public ApiGroup(Group handle) {
        super(handle);
        this.handle = handle;
    }

    @Override
    Group getHandle() {
        return this.handle;
    }

    @Nonnull
    @Override
    public String getName() {
        return this.handle.getName();
    }

    @Nonnull
    @Override
    public OptionalInt getWeight() {
        return this.handle.getWeight();
    }

    @Nonnull
    @Override
    public GroupData getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiGroup)) return false;
        ApiGroup that = (ApiGroup) o;
        return this.handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }
}
