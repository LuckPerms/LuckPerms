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

import me.lucko.luckperms.common.cacheddata.type.PermissionCache;
import me.lucko.luckperms.common.context.QueryOptionsCache;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.verbose.event.PermissionCheckEvent;
import me.lucko.luckperms.fabric.context.FabricContextManager;
import me.lucko.luckperms.fabric.context.PlayerQueryOptionsHolder;
import me.lucko.luckperms.fabric.event.PlayerChangeWorldCallback;
import me.lucko.luckperms.fabric.listeners.PermissionCheckListener;

import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements PlayerQueryOptionsHolder, PermissionCheckListener.MixinSubject {

    private QueryOptionsCache<ServerPlayerEntity> queryOptionsCache;
    private User user;

    @Shadow public abstract ServerWorld getServerWorld();

    @Override
    public QueryOptionsCache<ServerPlayerEntity> getQueryOptionsCache(PlayerQueryOptionsHolder.Factory factory) {
        if (this.queryOptionsCache == null) {
            this.queryOptionsCache = factory.createCache(((ServerPlayerEntity) (Object) this));
        }
        return this.queryOptionsCache;
    }

    @Override
    public void initializePermissions(User user) {
        this.user = user;

        // ensure query options cache is initialised too.
        FabricContextManager contextManager = (FabricContextManager) user.getPlugin().getContextManager();
        getQueryOptionsCache(contextManager);
    }

    @Override
    public Tristate hasPermission(String permission) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        return hasPermission(permission, this.queryOptionsCache.getQueryOptions());
    }

    @Override
    public Tristate hasPermission(String permission, QueryOptions queryOptions) {
        if (permission == null) {
            throw new NullPointerException("permission");
        }
        if (queryOptions == null) {
            throw new NullPointerException("queryOptions");
        }

        final User user = this.user;
        if (user == null) {
            throw new IllegalStateException("Permissions have not been initialised for this player yet.");
        }

        PermissionCache data = user.getCachedData().getPermissionData(queryOptions);
        return data.checkPermission(permission, PermissionCheckEvent.Origin.PLATFORM_PERMISSION_CHECK).result();
    }

    @Inject(
            at = @At("TAIL"),
            method = "worldChanged",
            locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void luckperms_onChangeDimension(ServerWorld targetWorld, CallbackInfo ci) {
        PlayerChangeWorldCallback.EVENT.invoker().onChangeWorld(this.getServerWorld(), targetWorld, (ServerPlayerEntity) (Object) this);
    }
}
