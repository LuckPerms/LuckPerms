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
import me.lucko.luckperms.common.cacheddata.GroupCachedDataManager;
import me.lucko.luckperms.common.model.Group;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.OptionalInt;

public class ApiGroup extends ApiPermissionHolder implements net.luckperms.api.model.group.Group {
    public static Group cast(net.luckperms.api.model.group.Group group) {
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

    @Override
    public @NonNull String getName() {
        return this.handle.getName();
    }

    @Override
    public @Nullable String getDisplayName() {
        return this.handle.getDisplayName().orElse(null);
    }

    @Override
    public @Nullable String getDisplayName(@NonNull QueryOptions queryOptions) {
        return this.handle.calculateDisplayName(queryOptions).orElse(null);
    }

    @Override
    public @NonNull OptionalInt getWeight() {
        return this.handle.getWeight();
    }

    @Override
    public @NonNull GroupCachedDataManager getCachedData() {
        return this.handle.getCachedData();
    }

    @Override
    protected void onNodeChange() {
        // invalidate caches - they have potentially been affected by
        // this change.
        this.handle.getPlugin().getGroupManager().invalidateAllGroupCaches();
        this.handle.getPlugin().getUserManager().invalidateAllUserCaches();
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
