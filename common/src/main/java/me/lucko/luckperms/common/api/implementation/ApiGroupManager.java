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

import com.google.common.collect.ImmutableListMultimap;
import me.lucko.luckperms.common.api.ApiUtils;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiGroupManager extends ApiAbstractManager<Group, net.luckperms.api.model.group.Group, GroupManager<?>> implements net.luckperms.api.model.group.GroupManager {
    public ApiGroupManager(LuckPermsPlugin plugin, GroupManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected net.luckperms.api.model.group.Group proxy(me.lucko.luckperms.common.model.Group internal) {
        return internal == null ? null : internal.getApiProxy();
    }

    @Override
    public @NonNull CompletableFuture<net.luckperms.api.model.group.Group> createAndLoadGroup(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().createAndLoadGroup(name, CreationCause.API)
                .thenApply(this::proxy);
    }

    @Override
    public @NonNull CompletableFuture<Optional<net.luckperms.api.model.group.Group>> loadGroup(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().loadGroup(name).thenApply(opt -> opt.map(this::proxy));
    }

    @Override
    public @NonNull CompletableFuture<Void> saveGroup(net.luckperms.api.model.group.@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        return this.plugin.getStorage().saveGroup(ApiGroup.cast(group));
    }

    @Override
    public @NonNull CompletableFuture<Void> deleteGroup(net.luckperms.api.model.group.@NonNull Group group) {
        Objects.requireNonNull(group, "group");
        if (group.getName().equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
            throw new IllegalArgumentException("Cannot delete the default group.");
        }

        return this.plugin.getStorage().deleteGroup(ApiGroup.cast(group), DeletionCause.API);
    }

    @Override
    public @NonNull CompletableFuture<Void> modifyGroup(@NonNull String name, @NonNull Consumer<? super net.luckperms.api.model.group.Group> action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");

        return this.plugin.getStorage().createAndLoadGroup(name, CreationCause.API)
                .thenApplyAsync(group -> {
                    action.accept(group.getApiProxy());
                    return group;
                }, this.plugin.getBootstrap().getScheduler().async())
                .thenCompose(group -> this.plugin.getStorage().saveGroup(group));
    }

    @Override
    public @NonNull CompletableFuture<Void> loadAllGroups() {
        return this.plugin.getStorage().loadAllGroups();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Deprecated
    public @NonNull CompletableFuture<List<HeldNode<String>>> getWithPermission(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return (CompletableFuture) this.plugin.getStorage().searchGroupNodes(StandardNodeMatchers.key(permission));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> @NonNull CompletableFuture<Map<String, Collection<T>>> searchAll(@NonNull NodeMatcher<? extends T> matcher) {
        Objects.requireNonNull(matcher, "matcher");
        ConstraintNodeMatcher<? extends T> constraint = (ConstraintNodeMatcher<? extends T>) matcher;
        return this.plugin.getStorage().searchGroupNodes(constraint).thenApply(list -> {
            ImmutableListMultimap.Builder<String, T> builder = ImmutableListMultimap.builder();
            for (NodeEntry<String, ? extends T> row : list) {
                builder.put(row.getHolder(), row.getNode());
            }
            return builder.build().asMap();
        });
    }

    @Override
    public net.luckperms.api.model.group.Group getGroup(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return proxy(this.handle.getIfLoaded(name));
    }

    @Override
    public @NonNull Set<net.luckperms.api.model.group.Group> getLoadedGroups() {
        return this.handle.getAll().values().stream()
                .map(this::proxy)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
