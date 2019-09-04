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

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger implements Messenger, PluginMessageListener {
    private static final String CHANNEL = "luckperms:update";

    private final LPBukkitPlugin plugin;
    private final IncomingMessageConsumer consumer;

    public PluginMessageMessenger(LPBukkitPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        this.plugin.getBootstrap().getServer().getMessenger().registerOutgoingPluginChannel(this.plugin.getBootstrap(), CHANNEL);
        this.plugin.getBootstrap().getServer().getMessenger().registerIncomingPluginChannel(this.plugin.getBootstrap(), CHANNEL, this);
    }

    @Override
    public void close() {
        this.plugin.getBootstrap().getServer().getMessenger().unregisterIncomingPluginChannel(this.plugin.getBootstrap(), CHANNEL);
        this.plugin.getBootstrap().getServer().getMessenger().unregisterOutgoingPluginChannel(this.plugin.getBootstrap(), CHANNEL);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(outgoingMessage.asEncodedString());
        byte[] data = out.toByteArray();

        new BukkitRunnable() {
            @Override
            public void run() {
                Collection<? extends Player> players = PluginMessageMessenger.this.plugin.getBootstrap().getServer().getOnlinePlayers();
                Player p = Iterables.getFirst(players, null);
                if (p == null) {
                    return;
                }

                p.sendPluginMessage(PluginMessageMessenger.this.plugin.getBootstrap(), CHANNEL, data);
                cancel();
            }
        }.runTaskTimer(this.plugin.getBootstrap(), 1L, 100L);
    }

    @Override
    public void onPluginMessageReceived(String s, @NonNull Player player, @NonNull byte[] bytes) {
        if (!s.equals(CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        String msg = in.readUTF();

        this.consumer.consumeIncomingMessageAsString(msg);
    }
}
