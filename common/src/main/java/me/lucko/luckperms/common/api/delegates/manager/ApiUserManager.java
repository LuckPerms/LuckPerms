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

import lombok.AllArgsConstructor;
import lombok.NonNull;

import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.manager.UserManager;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ApiUserManager implements UserManager {
    private final LuckPermsPlugin plugin;
    private final me.lucko.luckperms.common.managers.UserManager handle;

    @Override
    public User getUser(@NonNull UUID uuid) {
        me.lucko.luckperms.common.model.User user = handle.getIfLoaded(uuid);
        return user == null ? null : user.getDelegate();
    }

    @Override
    public User getUser(@NonNull String name) {
        me.lucko.luckperms.common.model.User user = handle.getByUsername(name);
        return user == null ? null : user.getDelegate();
    }

    @Override
    public Set<User> getLoadedUsers() {
        return handle.getAll().values().stream().map(me.lucko.luckperms.common.model.User::getDelegate).collect(Collectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull UUID uuid) {
        return handle.isLoaded(UserIdentifier.of(uuid, null));
    }

    @Override
    public void cleanupUser(@NonNull User user) {
        handle.scheduleUnload(plugin.getUuidCache().getExternalUUID(ApiUser.cast(user).getUuid()));
    }
}
