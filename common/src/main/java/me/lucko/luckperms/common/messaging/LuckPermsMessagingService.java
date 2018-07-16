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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import me.lucko.luckperms.common.utils.gson.JObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LuckPermsMessagingService implements InternalMessagingService, IncomingMessageConsumer {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    
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

        if (!this.receivedMessages.add(message.getId())) {
            return false;
        }

        // determine if the message can be handled by us
        boolean valid = message instanceof UpdateMessage ||
                message instanceof UserUpdateMessage ||
                message instanceof LogMessage;

        // instead of throwing an exception here, just return false
        // it means an instance of LP can gracefully handle messages it doesn't
        // "understand" yet. (sent from an instance running a newer version, etc)
        if (!valid) {
            return false;
        }

        processIncomingMessage(message);
        return true;
    }

    @Override
    public boolean consumeIncomingMessageAsString(@Nonnull String encodedString) {
        Objects.requireNonNull(encodedString, "encodedString");
        JsonObject decodedObject = GSON.fromJson(encodedString, JsonObject.class).getAsJsonObject();

        // extract id
        JsonElement idElement = decodedObject.get("id");
        if (idElement == null) {
            throw new IllegalStateException("Incoming message has no id argument: " + encodedString);
        }
        UUID id = UUID.fromString(idElement.getAsString());

        // ensure the message hasn't been received already
        if (!this.receivedMessages.add(id)) {
            return false;
        }

        // extract type
        JsonElement typeElement = decodedObject.get("type");
        if (typeElement == null) {
            throw new IllegalStateException("Incoming message has no type argument: " + encodedString);
        }
        String type = typeElement.getAsString();

        // extract content
        @Nullable JsonElement content = decodedObject.get("content");

        // decode message
        Message decoded;
        switch (type) {
            case UpdateMessageImpl.TYPE:
                decoded = UpdateMessageImpl.decode(content, id);
                break;
            case UserUpdateMessageImpl.TYPE:
                decoded = UserUpdateMessageImpl.decode(content, id);
                break;
            case LogMessageImpl.TYPE:
                decoded = LogMessageImpl.decode(content, id);
                break;
            default:
                // gracefully return if we just don't recognise the type
                return false;
        }

        // consume the message
        processIncomingMessage(decoded);
        return true;
    }

    public static String encodeMessageAsString(String type, UUID id, @Nullable JsonElement content) {
        JsonObject json = new JObject()
                .add("id", id.toString())
                .add("type", type)
                .consume(o -> {
                    if (content != null) {
                        o.add("content", content);
                    }
                })
                .toJson();

        return GSON.toJson(json);
    }

    private void processIncomingMessage(Message message) {
        if (message instanceof UpdateMessage) {
            UpdateMessage msg = (UpdateMessage) message;

            this.plugin.getLogger().info("[" + getName() + " Messaging] Received update ping with id: " + msg.getId());

            if (this.plugin.getEventFactory().handleNetworkPreSync(false, msg.getId())) {
                return;
            }

            this.plugin.getUpdateTaskBuffer().request();
        } else if (message instanceof UserUpdateMessage) {
            UserUpdateMessage msg = (UserUpdateMessage) message;

            User user = this.plugin.getUserManager().getIfLoaded(msg.getUser());
            if (user == null) {
                return;
            }

            this.plugin.getLogger().info("[" + getName() + " Messaging] Received user update ping for '" + user.getFriendlyName() + "' with id: " + msg.getId());

            if (this.plugin.getEventFactory().handleNetworkPreSync(false, msg.getId())) {
                return;
            }

            this.plugin.getStorage().loadUser(user.getUuid(), null);
        } else if (message instanceof LogMessage) {
            LogMessage msg = (LogMessage) message;

            this.plugin.getEventFactory().handleLogReceive(msg.getId(), msg.getLogEntry());
            this.plugin.getLogDispatcher().dispatchFromRemote((ExtendedLogEntry) msg.getLogEntry());
        } else {
            throw new IllegalArgumentException("Unknown message type: " + message.getClass().getName());
        }
    }

    private final class PushUpdateBuffer extends BufferedRequest<Void> {
        PushUpdateBuffer(LuckPermsPlugin plugin) {
            super(2, TimeUnit.SECONDS, plugin.getBootstrap().getScheduler());
        }

        @Override
        protected Void perform() {
            pushUpdate();
            return null;
        }
    }
}
