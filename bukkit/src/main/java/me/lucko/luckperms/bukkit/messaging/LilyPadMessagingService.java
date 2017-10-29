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

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.messaging.AbstractMessagingService;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;

import lilypad.client.connect.api.Connect;
import lilypad.client.connect.api.event.EventListener;
import lilypad.client.connect.api.event.MessageEvent;
import lilypad.client.connect.api.request.RequestException;
import lilypad.client.connect.api.request.impl.MessageRequest;

import java.io.UnsupportedEncodingException;
import java.util.Collections;

/**
 * An implementation of {@link ExtendedMessagingService} using LilyPad.
 */
public class LilyPadMessagingService extends AbstractMessagingService {
    private final LPBukkitPlugin plugin;
    private Connect connect;

    public LilyPadMessagingService(LPBukkitPlugin plugin) {
        super(plugin, "LilyPad");
        this.plugin = plugin;
    }

    public void init() {
        connect = plugin.getServer().getServicesManager().getRegistration(Connect.class).getProvider();
        connect.registerEvents(this);
    }

    @Override
    public void close() {
        connect.unregisterEvents(this);
    }

    @Override
    protected void sendMessage(String message) {
        MessageRequest request;

        try {
            request = new MessageRequest(Collections.emptyList(), CHANNEL, message);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }

        try {
            connect.request(request);
        } catch (RequestException e) {
            e.printStackTrace();
        }
    }

    @EventListener
    public void onMessage(MessageEvent event) {
        plugin.getScheduler().doAsync(() -> {
            try {
                String channel = event.getChannel();

                if (!channel.equals(CHANNEL)) {
                    return;
                }

                String message = event.getMessageAsString();

                onMessage(message, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
