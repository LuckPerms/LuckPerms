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

import com.mojang.serialization.Codec;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import me.lucko.luckperms.fabric.model.MixinUser;
import net.fabricmc.fabric.api.permission.v1.MutablePermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.fabric.api.permission.v1.PermissionEvents;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.impl.permission.PermissionContextKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Listener to route permission checks made via fabric-permission-api v1 to LuckPerms.
 */
@SuppressWarnings("UnstableApiUsage")
public class FabricPermissionsApiV1Listener {
    private static final PermissionContext.Key<User> USER_KEY = new PermissionContextKey<>(Identifier.fromNamespaceAndPath("luckperms", "user"));

    private final LPFabricPlugin plugin;

    public FabricPermissionsApiV1Listener(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        PermissionEvents.ON_REQUEST.register(this::onPermissionRequest);
        PermissionEvents.PREPARE_OFFLINE_PLAYER.register(this::onPrepareOfflinePlayer);
    }

    private <T> T onPermissionRequest(PermissionContext ctx, PermissionNode<T> node) {
        if (node.codec() == Codec.BOOL) {
            // permission check
            String permissionString = node.key().getNamespace() + '.' + node.key().getPath();
            return (T) permissionCheck(ctx, permissionString);
        }

        if (node.codec() == Codec.STRING) {
            // option lookup
            String optionKey = node.key().toString();
            return (T) optionCheck(ctx, optionKey);
        }

        if (node.codec() == Codec.INT) {
            // option lookup - parse to int
            String optionKey = node.key().toString();
            String optionValue = optionCheck(ctx, optionKey);
            if (optionValue != null) {
                try {
                    return (T) Integer.valueOf(Integer.parseInt(optionValue));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        return null;
    }

    private CompletableFuture<Consumer<MutablePermissionContext>> onPrepareOfflinePlayer(PermissionContext ctx, MinecraftServer server) {
        UUID uniqueId = ctx.uuid();
        String username = ctx.get(PermissionContextKey.NAME);

        return this.plugin.getStorage().loadUser(uniqueId, username).thenApply(user -> {
            if (user == null) {
                return null;
            }
            return mutableCtx -> mutableCtx.set(USER_KEY, user);
        });
    }

    private Boolean permissionCheck(PermissionContext ctx, String permission) {
        Entity entity = ctx.get(PermissionContextKey.ENTITY);
        if (entity instanceof MixinUser user) {
            return user.luckperms$hasPermission(permission).asBoolean();
        }

        User user = ctx.get(USER_KEY);
        if (user != null) {
            return user.getCachedData().getPermissionData().checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result().asBoolean();
        }

        return null;
    }

    private String optionCheck(PermissionContext ctx, String optionKey) {
        Entity entity = ctx.get(PermissionContextKey.ENTITY);
        if (entity instanceof MixinUser user) {
            return user.luckperms$getOption(optionKey);
        }

        User user = ctx.get(USER_KEY);
        if (user != null) {
            return user.getCachedData().getMetaData().getMetaOrChatMetaValue(optionKey, CheckOrigin.PLATFORM_API);
        }

        return null;
    }

}
