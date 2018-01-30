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
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.common.model.Track;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

public final class ApiTrack implements me.lucko.luckperms.api.Track {
    public static Track cast(me.lucko.luckperms.api.Track track) {
        Objects.requireNonNull(track, "track");
        Preconditions.checkState(track instanceof ApiTrack, "Illegal instance " + track.getClass() + " cannot be handled by this implementation.");
        return ((ApiTrack) track).getHandle();
    }

    private final Track handle;
    
    public ApiTrack(Track handle) {
        this.handle = handle;
    }

    Track getHandle() {
        return this.handle;
    }

    @Nonnull
    @Override
    public String getName() {
        return this.handle.getName();
    }

    @Nonnull
    @Override
    public List<String> getGroups() {
        return this.handle.getGroups();
    }

    @Override
    public int getSize() {
        return this.handle.getSize();
    }

    @Override
    public String getNext(@Nonnull Group current) {
        Objects.requireNonNull(current, "current");
        try {
            return this.handle.getNext(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getPrevious(@Nonnull Group current) {
        Objects.requireNonNull(current, "current");
        try {
            return this.handle.getPrevious(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public DataMutateResult appendGroup(@Nonnull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.appendGroup(ApiGroup.cast(group));
    }

    @Override
    public DataMutateResult insertGroup(@Nonnull Group group, int position) throws IndexOutOfBoundsException {
        Objects.requireNonNull(group, "group");
        return this.handle.insertGroup(ApiGroup.cast(group), position);
    }

    @Override
    public DataMutateResult removeGroup(@Nonnull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.removeGroup(ApiGroup.cast(group));
    }

    @Override
    public DataMutateResult removeGroup(@Nonnull String group) {
        Objects.requireNonNull(group, "group");
        return this.handle.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@Nonnull Group group) {
        Objects.requireNonNull(group, "group");
        return this.handle.containsGroup(ApiGroup.cast(group));
    }

    @Override
    public boolean containsGroup(@Nonnull String group) {
        Objects.requireNonNull(group, "group");
        return this.handle.containsGroup(group);
    }

    @Override
    public void clearGroups() {
        this.handle.clearGroups();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiTrack)) return false;
        ApiTrack that = (ApiTrack) o;
        return this.handle.equals(that.handle);
    }

    @Override
    public int hashCode() {
        return this.handle.hashCode();
    }
}
