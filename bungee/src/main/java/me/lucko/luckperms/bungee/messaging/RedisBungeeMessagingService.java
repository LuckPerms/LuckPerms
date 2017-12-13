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

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;

import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.messaging.AbstractMessagingService;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;

import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * An implementation of {@link ExtendedMessagingService} using Redis, via RedisBungee's API.
 */
public class RedisBungeeMessagingService extends AbstractMessagingService implements Listener {
    private final LPBungeePlugin plugin;
    private RedisBungeeAPI redisBungee;

    public RedisBungeeMessagingService(LPBungeePlugin plugin) {
        super(plugin, "RedisBungee");
        this.plugin = plugin;
    }

    public void init() {
        this.redisBungee = RedisBungee.getApi();
        redisBungee.registerPubSubChannels(CHANNEL);

        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void close() {
        redisBungee.unregisterPubSubChannels(CHANNEL);
        redisBungee = null;

        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @Override
    protected void sendMessage(String message) {
        redisBungee.sendChannelMessage(CHANNEL, message);
    }

    @EventHandler
    public void onMessage(PubSubMessageEvent e) {
        if (!e.getChannel().equals(CHANNEL)) {
            return;
        }

        onMessage(e.getMessage(), null);
    }
}
