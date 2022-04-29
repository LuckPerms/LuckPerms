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

package me.lucko.luckperms.forge.mixin.core.server.players;

import me.lucko.luckperms.forge.event.ConnectionEvent;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin into {@link PlayerList} for posting {@link ConnectionEvent.Login}
 */
@Mixin(value = PlayerList.class)
public abstract class PlayerListMixin {

    /**
     * Mixin into {@link PlayerList#placeNewPlayer(Connection, ServerPlayer)} for posting {@link ConnectionEvent.Login},
     * this event is used for finalizing an asynchronous preload operation for the connecting users' data which was
     * started during {@link ConnectionEvent.Auth}.
     */
    @Inject(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"
            ),
            cancellable = true
    )
    private void onLogin(Connection connection, ServerPlayer player, CallbackInfo callbackInfo) {
        ConnectionEvent.Login event = new ConnectionEvent.Login(connection, player);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }

        if (event.getMessage() != null) {
            connection.send(new ClientboundDisconnectPacket(event.getMessage()));
        }

        connection.disconnect(event.getMessage());
        callbackInfo.cancel();
    }

}
