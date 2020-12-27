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

package me.lucko.luckperms.fabric.listeners;

import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import me.lucko.luckperms.common.model.User;

import net.fabricmc.fabric.api.util.TriState;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Listener to route permission checks made via fabric-permissions-api to LuckPerms.
 */
public class PermissionCheckListener {

    public void registerListeners() {
        PermissionCheckEvent.EVENT.register(this::onPermissionCheck);
    }

    private TriState onPermissionCheck(ServerPlayerEntity player, String permission) {
        switch (((MixinSubject) player).hasPermission(permission)) {
            case TRUE:
                return TriState.TRUE;
            case FALSE:
                return TriState.FALSE;
            case UNDEFINED:
                return TriState.DEFAULT;
            default:
                throw new AssertionError();
        }
    }

    public interface MixinSubject {
        void initializePermissions(User user);
        Tristate hasPermission(String permission);
        Tristate hasPermission(String permission, QueryOptions queryOptions);
    }
}
