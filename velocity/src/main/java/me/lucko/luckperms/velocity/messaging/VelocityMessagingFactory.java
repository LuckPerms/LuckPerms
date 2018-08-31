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

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.MessengerProvider;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.LuckPermsMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;
import me.lucko.luckperms.velocity.LPVelocityPlugin;

import javax.annotation.Nonnull;

public class VelocityMessagingFactory extends MessagingFactory<LPVelocityPlugin> {
    public VelocityMessagingFactory(LPVelocityPlugin plugin) {
        super(plugin);
    }

    @Override
    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("pluginmsg") || messagingType.equals("velocity")) {
            try {
                return new LuckPermsMessagingService(getPlugin(), new PluginMessageMessengerProvider());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return super.getServiceFor(messagingType);
    }

    private class PluginMessageMessengerProvider implements MessengerProvider {

        @Nonnull
        @Override
        public String getName() {
            return "PluginMessage";
        }

        @Nonnull
        @Override
        public Messenger obtain(@Nonnull IncomingMessageConsumer incomingMessageConsumer) {
            PluginMessageMessenger messenger = new PluginMessageMessenger(getPlugin(), incomingMessageConsumer);
            messenger.init();
            return messenger;
        }
    }
}
