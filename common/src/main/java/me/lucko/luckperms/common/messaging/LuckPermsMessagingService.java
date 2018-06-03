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

package me.lucko.luckperms.common.messaging;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.MessengerProvider;
import me.lucko.luckperms.api.messenger.message.Message;
import me.lucko.luckperms.api.messenger.message.type.LogMessage;
import me.lucko.luckperms.api.messenger.message.type.UpdateMessage;
import me.lucko.luckperms.api.messenger.message.type.UserUpdateMessage;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.messaging.message.LogMessageImpl;
import me.lucko.luckperms.common.messaging.message.UpdateMessageImpl;
import me.lucko.luckperms.common.messaging.message.UserUpdateMessageImpl;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

public class LuckPermsMessagingService implements InternalMessagingService, IncomingMessageConsumer {
    private final LuckPermsPlugin plugin;
    private final Set<UUID> receivedMessages;
    private final PushUpdateBuffer updateBuffer;

    private final MessengerProvider messengerProvider;
    private final Messenger messenger;

    public LuckPermsMessagingService(LuckPermsPlugin plugin, MessengerProvider messengerProvider) {
        this.plugin = plugin;

        this.messengerProvider = messengerProvider;
        this.messenger = messengerProvider.obtain(this);
        Objects.requireNonNull(this.messenger, "messenger");

        this.receivedMessages = Collections.synchronizedSet(new HashSet<>());
        this.updateBuffer = new PushUpdateBuffer(plugin);
    }

    @Override
    public String getName() {
        return this.messengerProvider.getName();
    }

    @Override
    public Messenger getMessenger() {
        return this.messenger;
    }

    @Override
    public MessengerProvider getMessengerProvider() {
        return this.messengerProvider;
    }

    @Override
    public void close() {
        this.messenger.close();
    }

    @Override
    public BufferedRequest<Void> getUpdateBuffer() {
        return this.updateBuffer;
    }

    private UUID generatePingId() {
        UUID uuid = UUID.randomUUID();
        this.receivedMessages.add(uuid);
        return uuid;
    }

    @Override
    public void pushUpdate() {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            UUID requestId = generatePingId();
            this.plugin.getLogger().info("[" + getName() + " Messaging] Sending ping with id: " + requestId);
            this.messenger.sendOutgoingMessage(new UpdateMessageImpl(requestId));
        });
    }

    @Override
    public void pushUserUpdate(User user) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            UUID requestId = generatePingId();
            this.plugin.getLogger().info("[" + getName() + " Messaging] Sending user ping for '" + user.getFriendlyName() + "' with id: " + requestId);
            this.messenger.sendOutgoingMessage(new UserUpdateMessageImpl(requestId, user.getUuid()));
        });
    }

    @Override
    public void pushLog(LogEntry logEntry) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            UUID requestId = generatePingId();

            if (this.plugin.getEventFactory().handleLogNetworkPublish(!this.plugin.getConfiguration().get(ConfigKeys.PUSH_LOG_ENTRIES), requestId, logEntry)) {
                return;
            }

            this.plugin.getLogger().info("[" + getName() + " Messaging] Sending log with id: " + requestId);
            this.messenger.sendOutgoingMessage(new LogMessageImpl(requestId, logEntry));
        });
    }

    @Override
    public boolean consumeIncomingMessage(@Nonnull Message message) {
        Objects.requireNonNull(message, "message");

        if (message instanceof UpdateMessage) {
            UpdateMessage msg = (UpdateMessage) message;
            if (!this.receivedMessages.add(msg.getId())) {
                return false;
            }

            this.plugin.getLogger().info("[" + getName() + " Messaging] Received update ping with id: " + msg.getId());

            if (this.plugin.getEventFactory().handleNetworkPreSync(false, msg.getId())) {
                return true;
            }

            this.plugin.getUpdateTaskBuffer().request();
            return true;

        } else if (message instanceof UserUpdateMessage) {
            UserUpdateMessage msg = (UserUpdateMessage) message;
            if (!this.receivedMessages.add(msg.getId())) {
                return false;
            }

            User user = this.plugin.getUserManager().getIfLoaded(msg.getUser());
            if (user == null) {
                return true;
            }

            this.plugin.getLogger().info("[" + getName() + " Messaging] Received user update ping for '" + user.getFriendlyName() + "' with id: " + msg.getId());

            if (this.plugin.getEventFactory().handleNetworkPreSync(false, msg.getId())) {
                return true;
            }

            this.plugin.getStorage().loadUser(user.getUuid(), null);
            return true;

        } else if (message instanceof LogMessage) {
            LogMessage msg = (LogMessage) message;
            if (!this.receivedMessages.add(msg.getId())) {
                return false;
            }

            this.plugin.getEventFactory().handleLogReceive(msg.getId(), msg.getLogEntry());
            this.plugin.getLogDispatcher().dispatchFromRemote((ExtendedLogEntry) msg.getLogEntry());
            return true;

        } else {
            this.plugin.getLogger().warn("Unable to decode incoming message: " + message + " (" + message.getClass().getName() + ")");
            return false;
        }
    }

    @Override
    public boolean consumeIncomingMessageAsString(@Nonnull String encodedString) {
        Objects.requireNonNull(encodedString, "encodedString");

        Message decoded = UpdateMessageImpl.decode(encodedString);
        if (decoded != null) {
            return consumeIncomingMessage(decoded);
        }

        decoded = UserUpdateMessageImpl.decode(encodedString);
        if (decoded != null) {
            return consumeIncomingMessage(decoded);
        }

        decoded = LogMessageImpl.decode(encodedString);
        return decoded != null && consumeIncomingMessage(decoded);
    }

    private final class PushUpdateBuffer extends BufferedRequest<Void> {
        public PushUpdateBuffer(LuckPermsPlugin plugin) {
            super(2, TimeUnit.SECONDS, plugin.getBootstrap().getScheduler());
        }

        @Override
        protected Void perform() {
            pushUpdate();
            return null;
        }
    }
}
