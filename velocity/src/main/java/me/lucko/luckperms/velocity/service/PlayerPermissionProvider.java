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

package me.lucko.luckperms.velocity.service;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.PermissionSubject;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.lucko.luckperms.common.context.manager.QueryOptionsSupplier;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerPermissionProvider implements PermissionProvider, PermissionFunction {
    private final Player player;
    private final User user;
    private final QueryOptionsSupplier queryOptionsSupplier;

    public PlayerPermissionProvider(Player player, User user, QueryOptionsSupplier queryOptionsSupplier) {
        this.player = player;
        this.user = user;
        this.queryOptionsSupplier = queryOptionsSupplier;
    }

    @Override
    public @NonNull PermissionFunction createFunction(@NonNull PermissionSubject subject) {
        Preconditions.checkState(subject == this.player, "createFunction called with different argument");
        return this;
    }

    @Override
    public @NonNull Tristate getPermissionValue(@NonNull String permission) {
        return CompatibilityUtil.convertTristate(this.user.getCachedData().getPermissionData(this.queryOptionsSupplier.getQueryOptions()).checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result());
    }
}
