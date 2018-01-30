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

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.api.ApiUtils;
import me.lucko.luckperms.common.api.delegates.model.ApiGroup;
import me.lucko.luckperms.common.managers.group.GroupManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

public class ApiGroupManager extends ApiAbstractManager<Group, me.lucko.luckperms.api.Group, GroupManager<?>> implements me.lucko.luckperms.api.manager.GroupManager {
    public ApiGroupManager(LuckPermsPlugin plugin, GroupManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected me.lucko.luckperms.api.Group getDelegateFor(me.lucko.luckperms.common.model.Group internal) {
        if (internal == null) {
            return null;
        }

        return internal.getApiDelegate();
    }

    @Nonnull
    @Override
    public CompletableFuture<me.lucko.luckperms.api.Group> createAndLoadGroup(@Nonnull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().noBuffer().createAndLoadGroup(name, CreationCause.API)
                .thenApply(this::getDelegateFor);
    }

    @Nonnull
    @Override
    public CompletableFuture<Optional<me.lucko.luckperms.api.Group>> loadGroup(@Nonnull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().noBuffer().loadGroup(name).thenApply(opt -> opt.map(this::getDelegateFor));
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> saveGroup(@Nonnull me.lucko.luckperms.api.Group group) {
        Objects.requireNonNull(group, "group");
        return this.plugin.getStorage().noBuffer().saveGroup(ApiGroup.cast(group));
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> deleteGroup(@Nonnull me.lucko.luckperms.api.Group group) {
        Objects.requireNonNull(group, "group");
        if (group.getName().equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }
        return this.plugin.getStorage().noBuffer().deleteGroup(ApiGroup.cast(group), DeletionCause.API);
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> loadAllGroups() {
        return this.plugin.getStorage().noBuffer().loadAllGroups();
    }

    @Override
    public me.lucko.luckperms.api.Group getGroup(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return getDelegateFor(this.handle.getIfLoaded(name));
    }

    @Nonnull
    @Override
    public Set<me.lucko.luckperms.api.Group> getLoadedGroups() {
        return this.handle.getAll().values().stream()
                .map(this::getDelegateFor)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
