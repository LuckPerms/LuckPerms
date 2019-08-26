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

import me.lucko.luckperms.sponge.LPSpongePlugin;

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Platform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger implements Messenger, RawDataListener {
    private static final String CHANNEL = "luckperms:update";

    private final LPSpongePlugin plugin;
    private final IncomingMessageConsumer consumer;

    private ChannelBinding.RawDataChannel channel = null;

    public PluginMessageMessenger(LPSpongePlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        this.channel = this.plugin.getBootstrap().getGame().getChannelRegistrar().createRawChannel(this.plugin.getBootstrap(), CHANNEL);
        this.channel.addListener(Platform.Type.SERVER, this);
    }

    @Override
    public void close() {
        if (this.channel != null) {
            this.plugin.getBootstrap().getGame().getChannelRegistrar().unbindChannel(this.channel);
        }
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        this.plugin.getBootstrap().getSpongeScheduler().createTaskBuilder().interval(10, TimeUnit.SECONDS).execute(task -> {
            if (!this.plugin.getBootstrap().getGame().isServerAvailable()) {
                return;
            }

            Collection<Player> players = this.plugin.getBootstrap().getGame().getServer().getOnlinePlayers();
            Player p = Iterables.getFirst(players, null);
            if (p == null) {
                return;
            }

            this.channel.sendTo(p, buf -> buf.writeUTF(outgoingMessage.asEncodedString()));
            task.cancel();
        }).submit(this.plugin.getBootstrap());
    }

    @Override
    public void handlePayload(@NonNull ChannelBuf buf, @NonNull RemoteConnection connection, Platform.@NonNull Type type) {
        String msg = buf.readUTF();
        this.consumer.consumeIncomingMessageAsString(msg);
    }
}
