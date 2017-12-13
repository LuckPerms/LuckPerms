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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.caching.GroupData;

import java.util.OptionalInt;

public final class ApiGroup extends ApiPermissionHolder implements Group {
    public static me.lucko.luckperms.common.model.Group cast(Group g) {
        Preconditions.checkState(g instanceof ApiGroup, "Illegal instance " + g.getClass() + " cannot be handled by this implementation.");
        return ((ApiGroup) g).getHandle();
    }

    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.common.model.Group handle;

    public ApiGroup(@NonNull me.lucko.luckperms.common.model.Group handle) {
        super(handle);
        this.handle = handle;
    }

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public OptionalInt getWeight() {
        return handle.getWeight();
    }

    @Override
    public GroupData getCachedData() {
        return handle.getCachedData();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiGroup)) return false;

        ApiGroup other = (ApiGroup) o;
        return handle.equals(other.handle);
    }

    public int hashCode() {
        return handle.hashCode();
    }
}
