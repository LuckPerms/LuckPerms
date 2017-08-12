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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

/**
 * Provides a link between {@link Track} and {@link me.lucko.luckperms.common.model.Track}
 */
@AllArgsConstructor
public final class TrackDelegate implements Track {
    public static me.lucko.luckperms.common.model.Track cast(Track g) {
        Preconditions.checkState(g instanceof TrackDelegate, "Illegal instance " + g.getClass() + " cannot be handled by this implementation.");
        return ((TrackDelegate) g).getHandle();
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
    public String getNext(@NonNull Group current) throws ObjectLacksException {
        return handle.getNext(GroupDelegate.cast(current));
    }

    @Override
    public String getPrevious(@NonNull Group current) throws ObjectLacksException {
        return handle.getPrevious(GroupDelegate.cast(current));
    }

    @Override
    public void appendGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        handle.appendGroup(GroupDelegate.cast(group));
    }

    @Override
    public void insertGroup(@NonNull Group group, @NonNull int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException {
        handle.insertGroup(GroupDelegate.cast(group), position);
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        handle.removeGroup(GroupDelegate.cast(group));
    }

    @Override
    public void removeGroup(@NonNull String group) throws ObjectLacksException {
        handle.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@NonNull Group group) {
        return handle.containsGroup(GroupDelegate.cast(group));
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
        if (!(o instanceof TrackDelegate)) return false;

        TrackDelegate other = (TrackDelegate) o;
        return this.getName() == null ? other.getName() == null : this.getName().equals(other.getName());
    }

    public int hashCode() {
        return this.getName().hashCode();
    }
}
