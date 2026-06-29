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

package me.lucko.luckperms.common.minecraft.listeners;

import me.lucko.luckperms.common.event.listeners.AbstractCommandListUpdater;
import me.lucko.luckperms.common.minecraft.MinecraftLuckPermsPlugin;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.UUID;

/**
 * Calls {@link PlayerList#sendPlayerPermissionLevel(ServerPlayer)} when a players permissions change.
 */
public class MinecraftCommandListUpdater extends AbstractCommandListUpdater<MinecraftLuckPermsPlugin<?, ?>, ServerPlayer> {
    public MinecraftCommandListUpdater(MinecraftLuckPermsPlugin<?, ?> plugin) {
        super(plugin, ServerPlayer.class);
    }

    @Override
    protected boolean isServerAvailable() {
        return this.plugin.getBootstrap().getServer().isPresent();
    }

    @Override
    protected UUID getUniqueId(ServerPlayer player) {
        return player.getUUID();
    }

    @Override
    protected void sendCommandListUpdate(UUID uniqueId) {
        this.plugin.getBootstrap().getScheduler().executeSync(() -> {
            ServerPlayer player = this.plugin.getBootstrap().getPlayer(uniqueId).orElse(null);
            if (player != null) {
                MinecraftServer server = player.level().getServer();
                server.getPlayerList().sendPlayerPermissionLevel(player);
            }
        });
    }

}
