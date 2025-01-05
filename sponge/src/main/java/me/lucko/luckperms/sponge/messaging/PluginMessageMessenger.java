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
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.EngineConnectionSide;
import org.spongepowered.api.network.EngineConnectionState;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger extends AbstractPluginMessageMessenger implements RawPlayDataHandler<ServerConnectionState.Game> {
    private static final ResourceKey CHANNEL = ResourceKey.resolve(AbstractPluginMessageMessenger.CHANNEL);

    private final LPSpongePlugin plugin;

    private RawDataChannel channel = null;

    public PluginMessageMessenger(LPSpongePlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        this.channel = this.plugin.getBootstrap().getGame().channelManager().ofType(CHANNEL, RawDataChannel.class);
        this.channel.play().addHandler(ServerConnectionState.Game.class, this);
    }

    @Override
    public void close() {
        if (this.channel != null) {
            this.channel.play().removeHandler(this);
        }
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        if (!this.plugin.getBootstrap().getGame().isServerAvailable()) {
            return;
        }

        Task task = Task.builder()
                .interval(10, TimeUnit.SECONDS)
                .execute(t -> sendOutgoingMessage(buf, t))
                .plugin(this.plugin.getBootstrap().getPluginContainer())
                .build();

        this.plugin.getBootstrap().getScheduler().getSyncScheduler().submit(task);
    }

    private void sendOutgoingMessage(byte[] buf, ScheduledTask scheduledTask) {
        if (!this.plugin.getBootstrap().getGame().isServerAvailable()) {
            scheduledTask.cancel();
            return;
        }

        Collection<ServerPlayer> players = this.plugin.getBootstrap().getGame().server().onlinePlayers();
        ServerPlayer p = Iterables.getFirst(players, null);
        if (p == null) {
            return;
        }

        this.channel.play().sendTo(p, channelBuf -> channelBuf.writeBytes(buf));
        scheduledTask.cancel();
    }

    @Override
    public void handlePayload(ChannelBuf channelBuf, ServerConnectionState.Game state) {
        byte[] buf = channelBuf.readBytes(channelBuf.available());
        handleIncomingMessage(buf);
    }
}
