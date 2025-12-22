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

import me.lucko.luckperms.fabric.event.SetupPlayerPermissionsEvent;
import me.lucko.luckperms.fabric.model.MixinUser;
import net.luckperms.api.util.Tristate;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;

/**
 * Listener to route permission checks made via Minecraft's native permission predicate to LuckPerms.
 */
public class FabricPermissionsListener {

    public void registerListeners() {
        SetupPlayerPermissionsEvent.EVENT.register(this::onSetupPlayerPermissions);
    }

    private PermissionSet onSetupPlayerPermissions(ServerPlayer entity, PermissionSet defaults) {
        return new LuckPermsPermissionSet(entity, defaults);
    }

    private static final class LuckPermsPermissionSet implements PermissionSet {
        private final ServerPlayer player;
        private final PermissionSet delegate;

        LuckPermsPermissionSet(ServerPlayer player, PermissionSet delegate) {
            this.player = player;
            this.delegate = delegate;
        }

        @Override
        public boolean hasPermission(Permission permission) {
            if (permission instanceof Permission.Atom) {
                Identifier permissionId = ((Permission.Atom) permission).id();
                String permissionString = permissionId.getNamespace() + '.' + permissionId.getPath();

                Tristate result = ((MixinUser) this.player).luckperms$hasPermission(permissionString);
                if (result != Tristate.UNDEFINED) {
                    return result.asBoolean();
                }
            }

            return this.delegate.hasPermission(permission);
        }
    }
}
