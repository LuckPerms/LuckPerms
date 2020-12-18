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
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.user.UserManager;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.misc.NodeEntry;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiUserManager extends ApiAbstractManager<User, net.luckperms.api.model.user.User, UserManager<?>> implements net.luckperms.api.model.user.UserManager {
    public ApiUserManager(LuckPermsPlugin plugin, UserManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected net.luckperms.api.model.user.User proxy(User internal) {
        return internal == null ? null : internal.getApiProxy();
    }

    @Override
    public @NonNull CompletableFuture<net.luckperms.api.model.user.User> loadUser(@NonNull UUID uniqueId, @Nullable String username) {
        Objects.requireNonNull(uniqueId, "uuid");
        ApiUtils.checkUsername(username, this.plugin);

        if (this.plugin.getUserManager().getIfLoaded(uniqueId) == null) {
            this.plugin.getUserManager().getHouseKeeper().registerApiUsage(uniqueId);
        }

        return this.plugin.getStorage().loadUser(uniqueId, username)
                .thenApply(this::proxy);
    }

    @Override
    public @NonNull CompletableFuture<UUID> lookupUniqueId(@NonNull String username) {
        Objects.requireNonNull(username, "username");
        return this.plugin.getStorage().getPlayerUniqueId(username);
    }

    @Override
    public @NonNull CompletableFuture<String> lookupUsername(@NonNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uuid");
        return this.plugin.getStorage().getPlayerName(uniqueId);
    }

    @Override
    public @NonNull CompletableFuture<Void> saveUser(net.luckperms.api.model.user.@NonNull User user) {
        User internal = ApiUser.cast(Objects.requireNonNull(user, "user"));
        this.plugin.getUserManager().giveDefaultIfNeeded(internal);
        return this.plugin.getStorage().saveUser(internal);
    }

    @Override
    public @NonNull CompletableFuture<Void> modifyUser(@NonNull UUID uniqueId, @NonNull Consumer<? super net.luckperms.api.model.user.User> action) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(action, "action");

        return this.plugin.getStorage().loadUser(uniqueId, null)
                .thenApplyAsync(user -> {
                    action.accept(user.getApiProxy());
                    return user;
                }, this.plugin.getBootstrap().getScheduler().async())
                .thenCompose(user -> this.plugin.getStorage().saveUser(user));
    }

    @Override
    public @NonNull CompletableFuture<PlayerSaveResult> savePlayerData(@NonNull UUID uniqueId, @NonNull String username) {
        Objects.requireNonNull(uniqueId, "uuid");
        Objects.requireNonNull(username, "username");
        return this.plugin.getStorage().savePlayerData(uniqueId, username);
    }

    @Override
    public @NonNull CompletableFuture<Void> deletePlayerData(@NonNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return this.plugin.getStorage().deletePlayerData(uniqueId);
    }

    @Override
    public @NonNull CompletableFuture<Set<UUID>> getUniqueUsers() {
        return this.plugin.getStorage().getUniqueUsers();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    @Deprecated
    public @NonNull CompletableFuture<List<HeldNode<UUID>>> getWithPermission(@NonNull String permission) {
        Objects.requireNonNull(permission, "permission");
        return (CompletableFuture) this.plugin.getStorage().searchUserNodes(StandardNodeMatchers.key(permission));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Node> @NonNull CompletableFuture<Map<UUID, Collection<T>>> searchAll(@NonNull NodeMatcher<? extends T> matcher) {
        Objects.requireNonNull(matcher, "matcher");
        ConstraintNodeMatcher<? extends T> constraint = (ConstraintNodeMatcher<? extends T>) matcher;
        return this.plugin.getStorage().searchUserNodes(constraint).thenApply(list -> {
            ImmutableListMultimap.Builder<UUID, T> builder = ImmutableListMultimap.builder();
            for (NodeEntry<UUID, ? extends T> row : list) {
                builder.put(row.getHolder(), row.getNode());
            }
            return builder.build().asMap();
        });
    }

    @Override
    public net.luckperms.api.model.user.User getUser(@NonNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uuid");
        return proxy(this.handle.getIfLoaded(uniqueId));
    }

    @Override
    public net.luckperms.api.model.user.User getUser(@NonNull String username) {
        Objects.requireNonNull(username, "name");
        return proxy(this.handle.getByUsername(username));
    }

    @Override
    public @NonNull Set<net.luckperms.api.model.user.User> getLoadedUsers() {
        return this.handle.getAll().values().stream()
                .map(this::proxy)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uuid");
        return this.handle.isLoaded(uniqueId);
    }

    @Override
    public void cleanupUser(net.luckperms.api.model.user.@NonNull User user) {
        Objects.requireNonNull(user, "user");
        this.handle.getHouseKeeper().clearApiUsage(ApiUser.cast(user).getUniqueId());
    }
}
