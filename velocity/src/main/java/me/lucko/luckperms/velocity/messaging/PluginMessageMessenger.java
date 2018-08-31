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

package me.lucko.luckperms.velocity.messaging;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.ChannelSide;
import com.velocitypowered.api.proxy.messages.MessageHandler;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.message.OutgoingMessage;
import me.lucko.luckperms.velocity.LPVelocityPlugin;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger implements Messenger, MessageHandler {
    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.create("luckperms", "update");

    private final LPVelocityPlugin plugin;
    private final IncomingMessageConsumer consumer;

    public PluginMessageMessenger(LPVelocityPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        ChannelRegistrar channelRegistrar = this.plugin.getBootstrap().getProxy().getChannelRegistrar();
        channelRegistrar.register(this, CHANNEL);
    }

    @Override
    public void close() {
        // TODO: no way to unregister an individual MessageHandler?
    }

    private void dispatchMessage(byte[] data) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            this.plugin.getBootstrap().getProxy().getAllPlayers().stream()
                    .map(Player::getCurrentServer)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .distinct()
                    .forEach(server -> server.sendPluginMessage(CHANNEL, data));
        });
    }

    @Override
    public void sendOutgoingMessage(@Nonnull OutgoingMessage outgoingMessage) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(outgoingMessage.asEncodedString());

        byte[] data = out.toByteArray();
        dispatchMessage(data);
    }

    @Override
    public ForwardStatus handle(ChannelMessageSource source, ChannelSide side, ChannelIdentifier channel, byte[] data) {
        if (side == ChannelSide.FROM_CLIENT) {
            return ForwardStatus.HANDLED;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String msg = in.readUTF();

        if (this.consumer.consumeIncomingMessageAsString(msg)) {
            // Forward to other servers
            this.plugin.getBootstrap().getScheduler().executeAsync(() -> dispatchMessage(data));
        }

        return ForwardStatus.HANDLED;
    }

}
