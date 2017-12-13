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

package me.lucko.luckperms.sponge.messaging;

import com.google.common.collect.Iterables;

import me.lucko.luckperms.common.messaging.AbstractMessagingService;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.sponge.LPSpongePlugin;

import org.spongepowered.api.Platform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link ExtendedMessagingService} using the plugin messaging channels.
 */
public class BungeeMessagingService extends AbstractMessagingService implements RawDataListener {
    private final LPSpongePlugin plugin;
    private ChannelBinding.RawDataChannel channel = null;

    public BungeeMessagingService(LPSpongePlugin plugin) {
        super(plugin, "Bungee");
        this.plugin = plugin;
    }

    public void init() {
        channel = plugin.getGame().getChannelRegistrar().createRawChannel(plugin, CHANNEL);
        channel.addListener(Platform.Type.SERVER, this);
    }

    @Override
    public void close() {
        if (channel != null) {
            plugin.getGame().getChannelRegistrar().unbindChannel(channel);
        }
    }

    @Override
    protected void sendMessage(String message) {
        plugin.getSpongeScheduler().createTaskBuilder().interval(10, TimeUnit.SECONDS).execute(task -> {
            if (!plugin.getGame().isServerAvailable()) {
                return;
            }

            Collection<Player> players = plugin.getGame().getServer().getOnlinePlayers();
            Player p = Iterables.getFirst(players, null);
            if (p == null) {
                return;
            }

            this.channel.sendTo(p, buf -> buf.writeUTF(message));

            task.cancel();
        }).submit(plugin);
    }

    @Override
    public void handlePayload(ChannelBuf buf, RemoteConnection connection, Platform.Type type) {
        String msg = buf.readUTF();
        onMessage(msg, null);
    }
}
