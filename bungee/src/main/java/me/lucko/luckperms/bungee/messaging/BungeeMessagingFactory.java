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

import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BungeeMessagingFactory extends MessagingFactory<LPBungeePlugin> {
    public BungeeMessagingFactory(LPBungeePlugin plugin) {
        super(plugin);
    }

    @Override
    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("pluginmsg") || messagingType.equals("bungee")) {
            try {
                return new LuckPermsMessagingService(getPlugin(), new PluginMessageMessengerProvider());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (messagingType.equals("redisbungee")) {
            if (getPlugin().getBootstrap().getProxy().getPluginManager().getPlugin("RedisBungee") == null) {
                getPlugin().getLogger().warn("RedisBungee plugin not present.");
            } else {
                try {
                    return new LuckPermsMessagingService(getPlugin(), new RedisBungeeMessengerProvider());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return super.getServiceFor(messagingType);
    }

    private class PluginMessageMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "PluginMessage";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            PluginMessageMessenger messenger = new PluginMessageMessenger(getPlugin(), incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }

    private class RedisBungeeMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "RedisBungee";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            RedisBungeeMessenger messenger = new RedisBungeeMessenger(getPlugin(), incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }
}
