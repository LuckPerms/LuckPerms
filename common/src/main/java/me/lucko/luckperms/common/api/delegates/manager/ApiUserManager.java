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

import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.manager.UserManager;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public class ApiUserManager implements UserManager {
    private final me.lucko.luckperms.common.managers.user.UserManager<?> handle;

    public ApiUserManager(me.lucko.luckperms.common.managers.user.UserManager<?> handle) {
        this.handle = handle;
    }

    @Override
    public User getUser(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        me.lucko.luckperms.common.model.User user = this.handle.getIfLoaded(uuid);
        return user == null ? null : new ApiUser(user);
    }

    @Override
    public User getUser(@Nonnull String name) {
        Objects.requireNonNull(name, "name");
        me.lucko.luckperms.common.model.User user = this.handle.getByUsername(name);
        return user == null ? null : new ApiUser(user);
    }

    @Nonnull
    @Override
    public Set<User> getLoadedUsers() {
        return this.handle.getAll().values().stream().map(ApiUser::new).collect(Collectors.toSet());
    }

    @Override
    public boolean isLoaded(@Nonnull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return this.handle.isLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public void cleanupUser(@Nonnull User user) {
        Objects.requireNonNull(user, "user");
        this.handle.getHouseKeeper().clearApiUsage(ApiUser.cast(user).getUuid());
    }
}
