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

package me.lucko.luckperms.forge.mixin.core.server.level;

import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.forge.bridge.server.level.ServerPlayerBridge;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Mixin into {@link ServerPlayer} to store LuckPerms caches and implement {@link ServerPlayerBridge}.
 */
@Mixin(value = ServerPlayer.class)
public abstract class ServerPlayerMixin implements ServerPlayerBridge {

    /**
     * Cache player locale
     */
    private Locale luckperms$locale;

    /**
     * Hold a QueryOptionsCache instance on the player itself, so we can just cast instead of
     * having to maintain a map of Player->Cache.
     */
    private QueryOptionsCache<ServerPlayer> luckperms$queryOptionsCache;

    /**
     * Cache a reference to the LP {@link User} instance loaded for this player
     */
    private User luckperms$user;

    @Override
    public Tristate bridge$hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        if (this.luckperms$queryOptionsCache == null || this.luckperms$user == null) {
            return Tristate.UNDEFINED;
        }

        return bridge$hasPermission(permission, this.luckperms$queryOptionsCache.getQueryOptions());
    }

    @Override
    public Tristate bridge$hasPermission(String permission, QueryOptions queryOptions) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }

        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        if (this.luckperms$user == null || this.luckperms$queryOptionsCache == null) {
            return Tristate.UNDEFINED;
        }

        PermissionCache cache = this.luckperms$user.getCachedData().getPermissionData(queryOptions);
        return cache.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    @Override
    public Locale bridge$getLocale() {
        return this.luckperms$locale;
    }

    @Override
    public QueryOptionsCache<ServerPlayer> bridge$getQueryOptionsCache() {
        return this.luckperms$queryOptionsCache;
    }

    @Override
    public void bridge$setQueryOptionsCache(QueryOptionsCache<ServerPlayer> queryOptionsCache) {
        this.luckperms$queryOptionsCache = queryOptionsCache;
    }

    @Override
    public User bridge$getUser() {
        return this.luckperms$user;
    }

    @Override
    public void bridge$setUser(User user) {
        this.luckperms$user = user;
    }

    @Inject(
            method = "restoreFrom",
            at = @At(
                    value = "TAIL"
            )
    )
    private void onRestoreFrom(ServerPlayer oldPlayer, boolean alive, CallbackInfo callbackInfo) {
        ServerPlayerBridge oldPlayerBridge = (ServerPlayerBridge) oldPlayer;
        this.luckperms$locale = oldPlayerBridge.bridge$getLocale();
        this.luckperms$queryOptionsCache = oldPlayerBridge.bridge$getQueryOptionsCache();
        this.luckperms$queryOptionsCache.invalidate();
        this.luckperms$user = oldPlayerBridge.bridge$getUser();
    }

    @Inject(
            method = "updateOptions",
            at = @At(
                    value = "TAIL"
            )
    )
    private void onUpdateOptions(ServerboundClientInformationPacket packet, CallbackInfo callbackInfo) {
        this.luckperms$locale = TranslationManager.parseLocale(packet.language());
    }

}
