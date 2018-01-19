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

package me.lucko.luckperms.common.api.delegates.manager;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.manager.GroupManager;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class ApiGroupManager implements GroupManager {
    private final me.lucko.luckperms.common.managers.group.GroupManager<?> handle;

    public ApiGroupManager(me.lucko.luckperms.common.managers.group.GroupManager<?> handle) {
        this.handle = handle;
    }

    @Override
    public Group getGroup(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        me.lucko.luckperms.common.model.Group group = this.handle.getIfLoaded(name);
        return group == null ? null : group.getDelegate();
    }

    @Nonnull
    @Override
    public Set<Group> getLoadedGroups() {
        return this.handle.getAll().values().stream().map(me.lucko.luckperms.common.model.Group::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isLoaded(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
