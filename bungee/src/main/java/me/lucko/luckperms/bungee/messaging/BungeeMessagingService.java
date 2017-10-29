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
import me.lucko.luckperms.common.messaging.AbstractMessagingService;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;

import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * An implementation of {@link ExtendedMessagingService} using the plugin messaging channels.
 */
public class BungeeMessagingService extends AbstractMessagingService implements Listener {
    private final LPBungeePlugin plugin;

    public BungeeMessagingService(LPBungeePlugin plugin) {
        super(plugin, "Bungee");
        this.plugin = plugin;
    }

    public void init() {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
        plugin.getProxy().registerChannel(CHANNEL);
    }

    @Override
    public void close() {
        plugin.getProxy().unregisterChannel(CHANNEL);
    }

    @Override
    protected void sendMessage(String message) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);

        byte[] data = out.toByteArray();

        for (ServerInfo server : plugin.getProxy().getServers().values()) {
            server.sendData(CHANNEL, data, true);
        }
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

        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String msg = in.readUTF();

        onMessage(msg, u -> {
            // Forward to other servers
            plugin.getScheduler().doAsync(() -> sendMessage(u));
        });
    }
}
