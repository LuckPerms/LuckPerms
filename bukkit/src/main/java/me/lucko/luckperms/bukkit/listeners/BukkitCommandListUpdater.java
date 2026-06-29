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

package me.lucko.luckperms.bukkit.listeners;

import me.lucko.luckperms.bukkit.LPBukkitBootstrap;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.event.listeners.AbstractCommandListUpdater;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Calls {@link Player#updateCommands()} when a players permissions change.
 */
public class BukkitCommandListUpdater extends AbstractCommandListUpdater<LPBukkitPlugin, Player> {

    public static boolean isSupported() {
        try {
            Player.class.getMethod("updateCommands");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public BukkitCommandListUpdater(LPBukkitPlugin plugin) {
        super(plugin, Player.class);
    }

    @Override
    protected boolean isServerAvailable() {
        return !this.plugin.getBootstrap().isServerStopping();
    }

    @Override
    protected UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    protected void sendCommandListUpdate(UUID uniqueId) {
        LPBukkitBootstrap bootstrap = this.plugin.getBootstrap();
        if (bootstrap.isServerStopping()) {
            return;
        }

        Player player = bootstrap.getPlayer(uniqueId).orElse(null);
        if (player != null) {
            bootstrap.getScheduler().executeSync(player, player::updateCommands);
        }
    }
}
