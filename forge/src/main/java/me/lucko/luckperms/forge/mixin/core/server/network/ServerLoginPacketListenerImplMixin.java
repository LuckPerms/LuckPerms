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

package me.lucko.luckperms.forge.mixin.core.server.network;

import com.mojang.authlib.GameProfile;
import me.lucko.luckperms.forge.event.ConnectionEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * Mixin into {@link ServerLoginPacketListenerImpl} for posting {@link ConnectionEvent}
 */
@Mixin(value = ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow
    @Final
    public Connection connection;

    @Shadow
    @Nullable
    GameProfile gameProfile;

    @Shadow
    protected abstract GameProfile createFakeProfile(GameProfile p_10039_);

    private boolean luckperms$authenticated;

    /**
     * Mixin into {@link ServerLoginPacketListenerImpl#tick} for posting {@link ConnectionEvent.Auth},
     * this event is used for starting an asynchronous preload operation for the connecting users' data.
     */
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/network/NetworkHooks;tickNegotiation(Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)Z",
                    remap = false
            )
    )
    private void onTickNegotiation(CallbackInfo callbackInfo) {
        if (this.gameProfile == null) {
            return;
        }

        // Ensure the GameProfile is complete, this is primarily for offline servers with no UUID forwarding configured.
        if (!this.gameProfile.isComplete()) {
            this.gameProfile = this.createFakeProfile(this.gameProfile);
        }

        if (this.luckperms$authenticated) {
            return;
        }

        this.luckperms$authenticated = true;
        MinecraftForge.EVENT_BUS.post(new ConnectionEvent.Auth(this.connection, this.gameProfile));
    }

    /**
     * Mixin into {@link ServerLoginPacketListenerImpl#onDisconnect(Component)} for posting {@link ConnectionEvent.Disconnect},
     * this event is used for cleaning up failed logins.
     */
    @Inject(
            method = "onDisconnect",
            at = @At(
                    value = "HEAD"
            )
    )
    private void onDisconnect(CallbackInfo callbackInfo) {
        if (this.gameProfile == null || !this.gameProfile.isComplete()) {
            return;
        }

        MinecraftForge.EVENT_BUS.post(new ConnectionEvent.Disconnect(this.connection, this.gameProfile));
    }

}
