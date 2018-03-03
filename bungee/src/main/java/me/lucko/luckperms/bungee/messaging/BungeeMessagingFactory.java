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

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.MessengerProvider;
import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;

import javax.annotation.Nonnull;

public class BungeeMessagingFactory extends MessagingFactory<LPBungeePlugin> {
    public BungeeMessagingFactory(LPBungeePlugin plugin) {
        super(plugin);
    }

    @Override
    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("bungee")) {
            try {
                return new LuckPermsMessagingService(getPlugin(), new BungeeMessengerProvider());
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

    private class BungeeMessengerProvider implements MessengerProvider {

        @Nonnull
        @Override
        public String getName() {
            return "Bungee";
        }

        @Nonnull
        @Override
        public Messenger obtain(@Nonnull IncomingMessageConsumer incomingMessageConsumer) {
            BungeeMessenger bungeeMessaging = new BungeeMessenger(getPlugin(), incomingMessageConsumer);
            bungeeMessaging.init();
            return bungeeMessaging;
        }
    }

    private class RedisBungeeMessengerProvider implements MessengerProvider {

        @Nonnull
        @Override
        public String getName() {
            return "RedisBungee";
        }

        @Nonnull
        @Override
        public Messenger obtain(@Nonnull IncomingMessageConsumer incomingMessageConsumer) {
            RedisBungeeMessenger redisBungeeMessaging = new RedisBungeeMessenger(getPlugin(), incomingMessageConsumer);
            redisBungeeMessaging.init();
            return redisBungeeMessaging;
        }
    }
}
