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

package me.lucko.luckperms.bungee.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import me.lucko.luckperms.bungee.LPBungeePlugin;

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger implements Messenger, Listener {
    private static final String CHANNEL = "luckperms:update";

    private final LPBungeePlugin plugin;
    private final IncomingMessageConsumer consumer;

    public PluginMessageMessenger(LPBungeePlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getPluginManager().registerListener(this.plugin.getLoader(), this);
        proxy.registerChannel(CHANNEL);
    }

    @Override
    public void close() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.unregisterChannel(CHANNEL);
        proxy.getPluginManager().unregisterListener(this);
    }

    private void dispatchMessage(byte[] message) {
        for (ServerInfo server : this.plugin.getBootstrap().getProxy().getServers().values()) {
            server.sendData(CHANNEL, message, false);
        }
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(outgoingMessage.asEncodedString());

        byte[] message = out.toByteArray();
        dispatchMessage(message);
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals(CHANNEL)) {
            return;
        }

        e.setCancelled(true);

        if (e.getSender() instanceof ProxiedPlayer) {
            return;
        }

        byte[] data = e.getData();

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String msg = in.readUTF();

        if (this.consumer.consumeIncomingMessageAsString(msg)) {
            // Forward to other servers
            this.plugin.getBootstrap().getScheduler().executeAsync(() -> dispatchMessage(data));
        }
    }
}
