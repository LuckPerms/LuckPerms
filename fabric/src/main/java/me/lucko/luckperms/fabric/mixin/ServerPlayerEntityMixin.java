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

package me.lucko.luckperms.fabric.mixin;

import me.lucko.luckperms.common.cacheddata.type.MetaCache;
import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.manager.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;
import me.lucko.luckperms.fabric.context.FabricContextManager;
import me.lucko.luckperms.fabric.model.MixinUser;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link ServerPlayerEntity} to store LP caches and implement {@link MixinUser}.
 *
 * <p>This mixin is also temporarily used to implement our internal PlayerChangeWorldCallback,
 * until a similar event is added to Fabric itself.</p>
 */
@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements MixinUser {

    /** Cache a reference to the LP {@link User} instance loaded for this player */
    @Unique
    private User luckperms$user;

    /**
     * Hold a QueryOptionsCache instance on the player itself, so we can just cast instead of
     * having to maintain a map of Player->Cache.
     */
    @Unique
    private QueryOptionsCache<ServerPlayerEntity> luckperms$queryOptions;

    // Used by PlayerChangeWorldCallback hook below.
    @Shadow public abstract ServerWorld getServerWorld();

    @Override
    public User luckperms$getUser() {
        return this.luckperms$user;
    }

    @Override
    public QueryOptionsCache<ServerPlayerEntity> luckperms$getQueryOptionsCache(FabricContextManager contextManager) {
        if (this.luckperms$queryOptions == null) {
            this.luckperms$queryOptions = contextManager.newQueryOptionsCache((ServerPlayerEntity) (Object) this);
        }
        return this.luckperms$queryOptions;
    }

    @Override
    public void luckperms$initializePermissions(User user) {
        if (user == null) {
            return;
        }

        this.luckperms$user = user;

        // ensure query options cache is initialised too.
        if (this.luckperms$queryOptions == null) {
            this.luckperms$getQueryOptionsCache((FabricContextManager) user.getPlugin().getContextManager());
        }
    }

    @Override
    public Tristate luckperms$hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        if (this.luckperms$user == null || this.luckperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return Tristate.UNDEFINED;
        }
        return luckperms$hasPermission(permission, this.luckperms$queryOptions.getQueryOptions());
    }

    @Override
    public Tristate luckperms$hasPermission(String permission, QueryOptions queryOptions) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final User user = this.luckperms$user;
        if (user == null || this.luckperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return Tristate.UNDEFINED;
        }

        PermissionCache data = user.getCachedData().getPermissionData(queryOptions);
        return data.checkPermission(permission, CheckOrigin.PLATFORM_API_HAS_PERMISSION).result();
    }

    @Override
    public String luckperms$getOption(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (this.luckperms$user == null || this.luckperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return null;
        }
        return luckperms$getOption(key, this.luckperms$queryOptions.getQueryOptions());
    }

    @Override
    public String luckperms$getOption(String key, QueryOptions queryOptions) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final User user = this.luckperms$user;
        if (user == null || this.luckperms$queryOptions == null) {
            // "fake" players will have our mixin, but won't have been initialised.
            return null;
        }

        MetaCache cache = user.getCachedData().getMetaData(queryOptions);
        return cache.getMetaOrChatMetaValue(key, CheckOrigin.PLATFORM_API);
    }

    @Inject(at = @At("TAIL"), method = "copyFrom")
    private void luckperms$copyFrom(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        MixinUser oldMixin = (MixinUser) oldPlayer;
        luckperms$initializePermissions(oldMixin.luckperms$getUser());
    }
}
