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
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger extends AbstractPluginMessageMessenger implements PluginMessageListener {
    private final LPBukkitPlugin plugin;

    public PluginMessageMessenger(LPBukkitPlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        this.plugin.getBootstrap().getServer().getMessenger().registerOutgoingPluginChannel(this.plugin.getLoader(), CHANNEL);
        this.plugin.getBootstrap().getServer().getMessenger().registerIncomingPluginChannel(this.plugin.getLoader(), CHANNEL, this);
    }

    @Override
    public void close() {
        this.plugin.getBootstrap().getServer().getMessenger().unregisterIncomingPluginChannel(this.plugin.getLoader(), CHANNEL);
        this.plugin.getBootstrap().getServer().getMessenger().unregisterOutgoingPluginChannel(this.plugin.getLoader(), CHANNEL);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<? extends Player> players = PluginMessageMessenger.this.plugin.getBootstrap().getServer().getOnlinePlayers();
                Player p = Iterables.getFirst(players, null);
                if (p == null) {
                    return;
                }

                p.sendPluginMessage(PluginMessageMessenger.this.plugin.getLoader(), CHANNEL, buf);
                cancel();
            }
        }.runTaskTimer(this.plugin.getLoader(), 1L, 100L);
    }

    @Override
    public void onPluginMessageReceived(String channel, @NonNull Player player, byte @NonNull [] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        handleIncomingMessage(message);
    }
}
