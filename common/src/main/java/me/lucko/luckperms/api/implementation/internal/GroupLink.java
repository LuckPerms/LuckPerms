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
import lombok.Getter;
import lombok.NonNull;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

import static me.lucko.luckperms.api.implementation.internal.Utils.*;

/**
 * Provides a link between {@link Group} and {@link me.lucko.luckperms.groups.Group}
 */
@SuppressWarnings("unused")
public class GroupLink extends PermissionHolderLink implements Group {

    @Getter(AccessLevel.PACKAGE)
    private final me.lucko.luckperms.groups.Group master;

    public GroupLink(@NonNull me.lucko.luckperms.groups.Group master) {
        super(master);
        this.master = master;
    }

    @Override
    public String getName() {
        return master.getName();
    }

    @Override
    public boolean inheritsGroup(@NonNull Group group) {
        checkGroup(group);
        return master.inheritsGroup(((GroupLink) group).getMaster());
    }

    @Override
    public boolean inheritsGroup(@NonNull Group group, @NonNull String server) {
        checkGroup(group);
        return master.inheritsGroup(((GroupLink) group).getMaster(), server);
    }

    @Override
    public boolean inheritsGroup(@NonNull Group group, @NonNull String server, @NonNull String world) {
        checkGroup(group);
        return master.inheritsGroup(((GroupLink) group).getMaster(), server, world);
    }

    @Override
    public void setInheritGroup(@NonNull Group group) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void setInheritGroup(@NonNull Group group, @NonNull String server) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster(), checkServer(server));
    }

    @Override
    public void setInheritGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster(), checkServer(server), world);
    }

    @Override
    public void setInheritGroup(@NonNull Group group, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster(), checkTime(expireAt));
    }

    @Override
    public void setInheritGroup(@NonNull Group group, @NonNull String server, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster(), checkServer(server), checkTime(expireAt));
    }

    @Override
    public void setInheritGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull long expireAt) throws ObjectAlreadyHasException {
        checkGroup(group);
        master.setInheritGroup(((GroupLink) group).getMaster(), checkServer(server), world, checkTime(expireAt));
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster());
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster(), temporary);
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group, @NonNull String server) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster(), checkServer(server));
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group, @NonNull String server, @NonNull String world) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster(), checkServer(server), world);
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group, @NonNull String server, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster(), checkServer(server), temporary);
    }

    @Override
    public void unsetInheritGroup(@NonNull Group group, @NonNull String server, @NonNull String world, @NonNull boolean temporary) throws ObjectLacksException {
        checkGroup(group);
        master.unsetInheritGroup(((GroupLink) group).getMaster(), checkServer(server), world, temporary);
    }

    @Override
    public void clearNodes() {
        master.clearNodes();
    }

    @Override
    public List<String> getGroupNames() {
        return master.getGroupNames();
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server, @NonNull String world) {
        return master.getLocalGroups(checkServer(server), world);
    }

    @Override
    public List<String> getLocalGroups(@NonNull String server) {
        return master.getLocalGroups(checkServer(server));
    }
}
