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

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent.ForwardResult;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.lucko.luckperms.common.messaging.pluginmsg.AbstractPluginMessageMessenger;
import me.lucko.luckperms.velocity.LPVelocityPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;

/**
 * An implementation of {@link Messenger} using the plugin messaging channels.
 */
public class PluginMessageMessenger extends AbstractPluginMessageMessenger {
    private static final ChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(AbstractPluginMessageMessenger.CHANNEL);

    private final LPVelocityPlugin plugin;

    public PluginMessageMessenger(LPVelocityPlugin plugin, IncomingMessageConsumer consumer) {
        super(consumer);
        this.plugin = plugin;
    }

    public void init() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getChannelRegistrar().register(CHANNEL);
        proxy.getEventManager().register(this.plugin.getBootstrap(), this);
    }

    @Override
    public void close() {
        ProxyServer proxy = this.plugin.getBootstrap().getProxy();
        proxy.getChannelRegistrar().unregister(CHANNEL);
        proxy.getEventManager().unregisterListener(this.plugin.getBootstrap(), this);
    }

    @Override
    protected void sendOutgoingMessage(byte[] buf) {
        for (RegisteredServer server : this.plugin.getBootstrap().getProxy().getAllServers()) {
            server.sendPluginMessage(CHANNEL, buf);
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        // compare the underlying text representation of the channel
        // the namespaced representation is used by legacy servers too, so we
        // are able to support both. :)
        if (!e.getIdentifier().getId().equals(CHANNEL.getId())) {
            return;
        }

        e.setResult(ForwardResult.handled());

        if (e.getSource() instanceof Player) {
            return;
        }

        byte[] buf = e.getData();

        if (handleIncomingMessage(buf)) {
            // Forward to other servers
            this.plugin.getBootstrap().getScheduler().async(() -> sendOutgoingMessage(buf));
        }
    }
}
