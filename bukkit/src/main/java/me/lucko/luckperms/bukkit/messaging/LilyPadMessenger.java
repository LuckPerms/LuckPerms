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

import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.event.EventListener;
import lilypad.client.connect.api.event.MessageEvent;
import lilypad.client.connect.api.request.RequestException;
import lilypad.client.connect.api.request.impl.MessageRequest;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * An implementation of {@link Messenger} using LilyPad.
 */
public class LilyPadMessenger implements Messenger {
    private static final String CHANNEL = "luckperms:update";

    private final LPBukkitPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private Connect connect;

    public LilyPadMessenger(LPBukkitPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init() {
        this.connect = this.plugin.getBootstrap().getServer().getServicesManager().getRegistration(Connect.class).getProvider();
        this.connect.registerEvents(this);
    }

    @Override
    public void close() {
        this.connect.unregisterEvents(this);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        MessageRequest request = new MessageRequest(Collections.emptyList(), CHANNEL, outgoingMessage.asEncodedString().getBytes(StandardCharsets.UTF_8));
        try {
            this.connect.request(request);
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onMessage(MessageEvent event) {
        this.plugin.getBootstrap().getScheduler().async(() -> {
            String channel = event.getChannel();
            if (!channel.equals(CHANNEL)) {
                return;
            }
            String message = new String(event.getMessage(), StandardCharsets.UTF_8);
            this.consumer.consumeIncomingMessageAsString(message);
        });
    }
}
