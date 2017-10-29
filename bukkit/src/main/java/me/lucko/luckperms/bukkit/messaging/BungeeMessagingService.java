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

package me.lucko.luckperms.bukkit.messaging;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.messaging.AbstractMessagingService;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * An implementation of {@link ExtendedMessagingService} using the plugin messaging channels.
 */
public class BungeeMessagingService extends AbstractMessagingService implements PluginMessageListener {
    private final LPBukkitPlugin plugin;

    public BungeeMessagingService(LPBukkitPlugin plugin) {
        super(plugin, "Bungee");
        this.plugin = plugin;
    }

    public void init() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
    }

    @Override
    public void close() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL);
    }

    @Override
    protected void sendMessage(String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
                Player p = Iterables.getFirst(players, null);
                if (p == null) {
                    return;
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF(message);

                byte[] data = out.toByteArray();

                p.sendPluginMessage(plugin, CHANNEL, data);
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 100L);
    }

    @Override
    public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
        if (!s.equals(CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String msg = in.readUTF();

        onMessage(msg, null);
    }
}
