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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;

import java.util.List;

@AllArgsConstructor
public final class ApiTrack implements Track {
    public static me.lucko.luckperms.common.model.Track cast(Track g) {
        Preconditions.checkState(g instanceof ApiTrack, "Illegal instance " + g.getClass() + " cannot be handled by this implementation.");
        return ((ApiTrack) g).getHandle();
    }

    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.common.model.Track handle;

    @Override
    public String getName() {
        return handle.getName();
    }

    @Override
    public List<String> getGroups() {
        return handle.getGroups();
    }

    @Override
    public int getSize() {
        return handle.getSize();
    }

    @Override
    public String getNext(@NonNull Group current) {
        try {
            return handle.getNext(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getPrevious(@NonNull Group current) {
        try {
            return handle.getPrevious(ApiGroup.cast(current));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public DataMutateResult appendGroup(@NonNull Group group) {
        return handle.appendGroup(ApiGroup.cast(group));
    }

    @Override
    public DataMutateResult insertGroup(@NonNull Group group, @NonNull int position) throws IndexOutOfBoundsException {
        return handle.insertGroup(ApiGroup.cast(group), position);
    }

    @Override
    public DataMutateResult removeGroup(@NonNull Group group) {
        return handle.removeGroup(ApiGroup.cast(group));
    }

    @Override
    public DataMutateResult removeGroup(@NonNull String group) {
        return handle.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@NonNull Group group) {
        return handle.containsGroup(ApiGroup.cast(group));
    }

    @Override
    public boolean containsGroup(@NonNull String group) {
        return handle.containsGroup(group);
    }

    @Override
    public void clearGroups() {
        handle.clearGroups();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiTrack)) return false;

        ApiTrack other = (ApiTrack) o;
        return handle.equals(other.handle);
    }

    public int hashCode() {
        return handle.hashCode();
    }
}
