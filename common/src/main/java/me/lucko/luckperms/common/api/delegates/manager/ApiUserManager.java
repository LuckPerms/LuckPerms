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

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.common.api.ApiUtils;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.bulkupdate.comparisons.Constraint;
import me.lucko.luckperms.common.bulkupdate.comparisons.StandardComparison;
import me.lucko.luckperms.common.managers.user.UserManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.UserIdentifier;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ApiUserManager extends ApiAbstractManager<User, me.lucko.luckperms.api.User, UserManager<?>> implements me.lucko.luckperms.api.manager.UserManager {
    public ApiUserManager(LuckPermsPlugin plugin, UserManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected me.lucko.luckperms.api.User getDelegateFor(User internal) {
        if (internal == null) {
            return null;
        }
        return new ApiUser(internal);
    }

    @Nonnull
    @Override
    public CompletableFuture<me.lucko.luckperms.api.User> loadUser(@Nonnull UUID uuid, @Nullable String username) {
        Objects.requireNonNull(uuid, "uuid");
        ApiUtils.checkUsername(username);

        if (this.plugin.getUserManager().getIfLoaded(uuid) == null) {
            this.plugin.getUserManager().getHouseKeeper().registerApiUsage(uuid);
        }

        return this.plugin.getStorage().loadUser(uuid, username)
                .thenApply(this::getDelegateFor);
    }

    @Nonnull
    @Override
    public CompletableFuture<UUID> lookupUuid(@Nonnull String username) {
        Objects.requireNonNull(username, "username");
        return this.plugin.getStorage().getPlayerUuid(username);
    }

    @Nonnull
    @Override
    public CompletableFuture<String> lookupUsername(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.plugin.getStorage().getPlayerName(uuid);
    }

    @Nonnull
    @Override
    public CompletableFuture<Void> saveUser(@Nonnull me.lucko.luckperms.api.User user) {
        Objects.requireNonNull(user, "user");
        return this.plugin.getStorage().saveUser(ApiUser.cast(user));
    }

    @Nonnull
    @Override
    public CompletableFuture<PlayerSaveResult> savePlayerData(@Nonnull UUID uuid, @Nonnull String username) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(username, "username");
        return this.plugin.getStorage().savePlayerData(uuid, username);
    }

    @Nonnull
    @Override
    public CompletableFuture<Set<UUID>> getUniqueUsers() {
        return this.plugin.getStorage().getUniqueUsers();
    }

    @Nonnull
    @Override
    public CompletableFuture<List<HeldPermission<UUID>>> getWithPermission(@Nonnull String permission) {
        Objects.requireNonNull(permission, "permission");
        return this.plugin.getStorage().getUsersWithPermission(Constraint.of(StandardComparison.EQUAL, permission));
    }

    @Override
    public me.lucko.luckperms.api.User getUser(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return getDelegateFor(this.handle.getIfLoaded(uuid));
    }

    @Override
    public me.lucko.luckperms.api.User getUser(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        return getDelegateFor(this.handle.getByUsername(name));
    }

    @Nonnull
    @Override
    public Set<me.lucko.luckperms.api.User> getLoadedUsers() {
        return this.handle.getAll().values().stream()
                .map(this::getDelegateFor)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.isLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public void cleanupUser(@Nonnull me.lucko.luckperms.api.User user) {
        Objects.requireNonNull(user, "user");
        this.handle.getHouseKeeper().clearApiUsage(ApiUser.cast(user).getUuid());
    }
}
