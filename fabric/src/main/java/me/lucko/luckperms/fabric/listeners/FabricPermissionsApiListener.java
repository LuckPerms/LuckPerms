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

import me.lucko.fabric.api.permissions.v0.OptionRequestEvent;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.query.QueryOptionsImpl;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import me.lucko.luckperms.fabric.model.MixinUser;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

/**
 * Listener to route permission checks made via fabric-permissions-api to LuckPerms.
 */
public class FabricPermissionsApiListener {
    private final LPFabricPlugin plugin;

    public FabricPermissionsApiListener(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PermissionCheckEvent.EVENT.register(this::onPermissionCheck);
        OptionRequestEvent.EVENT.register(this::onOptionRequest);
    }

    private @NonNull TriState onPermissionCheck(CommandSource source, String permission) {
        if (source instanceof ServerCommandSource) {
            Entity entity = ((ServerCommandSource) source).getEntity();
            if (entity instanceof ServerPlayerEntity) {
                return playerPermissionCheck((ServerPlayerEntity) entity, permission);
            }
        }
        return otherPermissionCheck(source, permission);
    }

    private @NonNull Optional<String> onOptionRequest(CommandSource source, String key) {
        if (source instanceof ServerCommandSource) {
            Entity entity = ((ServerCommandSource) source).getEntity();
            if (entity instanceof ServerPlayerEntity) {
                return playerGetOption((ServerPlayerEntity) entity, key);
            }
        }
        return otherGetOption(source, key);
    }

    private TriState playerPermissionCheck(ServerPlayerEntity player, String permission) {
        switch (((MixinUser) player).hasPermission(permission)) {
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

    private TriState otherPermissionCheck(CommandSource source, String permission) {
        if (source instanceof ServerCommandSource) {
            String name = ((ServerCommandSource) source).getName();
            VerboseCheckTarget target = VerboseCheckTarget.internal(name);

            this.plugin.getVerboseHandler().offerPermissionCheckEvent(CheckOrigin.PLATFORM_API_HAS_PERMISSION, target, QueryOptionsImpl.DEFAULT_CONTEXTUAL, permission, TristateResult.UNDEFINED);
            this.plugin.getPermissionRegistry().offer(permission);
        }

        return TriState.DEFAULT;
    }

    private Optional<String> playerGetOption(ServerPlayerEntity player, String key) {
        return Optional.ofNullable(((MixinUser) player).getOption(key));
    }

    private Optional<String> otherGetOption(CommandSource source, String key) {
        if (source instanceof ServerCommandSource) {
            String name = ((ServerCommandSource) source).getName();
            VerboseCheckTarget target = VerboseCheckTarget.internal(name);

            this.plugin.getVerboseHandler().offerMetaCheckEvent(CheckOrigin.PLATFORM_API, target, QueryOptionsImpl.DEFAULT_CONTEXTUAL, key, StringResult.nullResult());
        }

        return Optional.empty();
    }

}
