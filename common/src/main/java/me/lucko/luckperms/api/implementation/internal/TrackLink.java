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

package me.lucko.luckperms.api.implementation.internal;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.Track;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

import static me.lucko.luckperms.api.implementation.internal.Utils.checkGroup;

/**
 * Provides a link between {@link Track} and {@link me.lucko.luckperms.tracks.Track}
 */
@SuppressWarnings("unused")
@AllArgsConstructor
public class TrackLink implements Track {

    @NonNull
    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.tracks.Track master;

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public List<String> getGroups() {
        return master.getGroups();
    }

    @Override
    public int getSize() {
        return master.getSize();
    }

    @Override
    public String getNext(@NonNull Group current) throws ObjectLacksException {
        checkGroup(current);
        return master.getNext(((GroupLink) current).getMaster());
    }

    @Override
    public String getPrevious(@NonNull Group current) throws ObjectLacksException {
        checkGroup(current);
        return master.getPrevious(((GroupLink) current).getMaster());
    }

    @Override
    public void appendGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.appendGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void insertGroup(@NonNull Group group, @NonNull int position) throws ObjectAlreadyHasException, IndexOutOfBoundsException {
        checkGroup(group);
        master.insertGroup(((GroupLink) group).getMaster(), position);
    }

    @Override
    public void removeGroup(@NonNull Group group) throws ObjectLacksException {
        checkGroup(group);
        master.removeGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void removeGroup(@NonNull String group) throws ObjectLacksException {
        master.removeGroup(group);
    }

    @Override
    public boolean containsGroup(@NonNull Group group) {
        checkGroup(group);
        return master.containsGroup(((GroupLink) group).getMaster());
    }

    @Override
    public boolean containsGroup(@NonNull String group) {
        return master.containsGroup(group);
    }

    @Override
    public void clearGroups() {
        master.clearGroups();
    }
}
