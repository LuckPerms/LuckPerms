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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.cache.BufferedRequest;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.messaging.message.ActionLogMessageImpl;
import me.lucko.luckperms.common.messaging.message.CustomMessageImpl;
import me.lucko.luckperms.common.messaging.message.UpdateMessageImpl;
import me.lucko.luckperms.common.messaging.message.UserUpdateMessageImpl;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.ExpiringSet;
import me.lucko.luckperms.common.util.gson.GsonProvider;
import me.lucko.luckperms.common.util.gson.JObject;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.sync.SyncType;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import net.luckperms.api.messenger.message.Message;
import net.luckperms.api.messenger.message.type.ActionLogMessage;
import net.luckperms.api.messenger.message.type.CustomMessage;
import net.luckperms.api.messenger.message.type.UpdateMessage;
import net.luckperms.api.messenger.message.type.UserUpdateMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LuckPermsMessagingService implements InternalMessagingService, IncomingMessageConsumer {
    private final LuckPermsPlugin plugin;
    private final ExpiringSet<UUID> receivedMessages;
    private final PushUpdateBuffer updateBuffer;

    private final MessengerProvider messengerProvider;
    private final Messenger messenger;

    public LuckPermsMessagingService(LuckPermsPlugin plugin, MessengerProvider messengerProvider) {
        this.plugin = plugin;

        this.messengerProvider = messengerProvider;
        this.messenger = messengerProvider.obtain(this);
        Objects.requireNonNull(this.messenger, "messenger");

        this.receivedMessages = new ExpiringSet<>(5, TimeUnit.MINUTES);
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
        this.plugin.getBootstrap().getScheduler().async(() -> {
            UUID requestId = generatePingId();
            this.plugin.getLogger().info("[Messaging] Sending ping with id: " + requestId);
            this.messenger.sendOutgoingMessage(new UpdateMessageImpl(requestId));
        });
    }

    @Override
    public void pushUserUpdate(User user) {
        this.plugin.getBootstrap().getScheduler().async(() -> {
            UUID requestId = generatePingId();
            this.plugin.getLogger().info("[Messaging] Sending user ping for '" + user.getPlainDisplayName() + "' with id: " + requestId);
            this.messenger.sendOutgoingMessage(new UserUpdateMessageImpl(requestId, user.getUniqueId()));
        });
    }

    @Override
    public void pushLog(Action logEntry) {
        this.plugin.getBootstrap().getScheduler().async(() -> {
            UUID requestId = generatePingId();

            if (this.plugin.getEventDispatcher().dispatchLogNetworkPublish(!this.plugin.getConfiguration().get(ConfigKeys.PUSH_LOG_ENTRIES), requestId, logEntry)) {
                return;
            }

            this.plugin.getLogger().info("[Messaging] Sending log with id: " + requestId);
            this.messenger.sendOutgoingMessage(new ActionLogMessageImpl(requestId, logEntry));
        });
    }

    @Override
    public void pushCustomPayload(String channelId, String payload) {
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            UUID requestId = generatePingId();
            this.messenger.sendOutgoingMessage(new CustomMessageImpl(requestId, channelId, payload));
        });
    }

    @Override
    public boolean consumeIncomingMessage(@NonNull Message message) {
        Objects.requireNonNull(message, "message");

        if (!this.receivedMessages.add(message.getId())) {
            return false;
        }

        // determine if the message can be handled by us
        boolean valid = message instanceof UpdateMessage ||
                message instanceof UserUpdateMessage ||
                message instanceof ActionLogMessage ||
                message instanceof CustomMessage;

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
    public boolean consumeIncomingMessageAsString(@NonNull String encodedString) {
        try {
            return consumeIncomingMessageAsString0(encodedString);
        } catch (Exception e) {
            this.plugin.getLogger().warn("Unable to decode incoming messaging service message: '" + encodedString + "'", e);
            return false;
        }
    }

    private boolean consumeIncomingMessageAsString0(@NonNull String encodedString) {
        Objects.requireNonNull(encodedString, "encodedString");
        JsonObject parsed = Objects.requireNonNull(GsonProvider.normal().fromJson(encodedString, JsonObject.class), "parsed");
        JsonObject json = parsed.getAsJsonObject();

        // extract id
        JsonElement idElement = json.get("id");
        if (idElement == null) {
            throw new IllegalStateException("Incoming message has no id argument: " + encodedString);
        }
        UUID id = UUID.fromString(idElement.getAsString());

        // ensure the message hasn't been received already
        if (!this.receivedMessages.add(id)) {
            return false;
        }

        // extract type
        JsonElement typeElement = json.get("type");
        if (typeElement == null) {
            throw new IllegalStateException("Incoming message has no type argument: " + encodedString);
        }
        String type = typeElement.getAsString();

        // extract content
        @Nullable JsonElement content = json.get("content");

        // decode message
        Message decoded;
        switch (type) {
            case UpdateMessageImpl.TYPE:
                decoded = UpdateMessageImpl.decode(content, id);
                break;
            case UserUpdateMessageImpl.TYPE:
                decoded = UserUpdateMessageImpl.decode(content, id);
                break;
            case ActionLogMessageImpl.TYPE:
                decoded = ActionLogMessageImpl.decode(content, id);
                break;
            case CustomMessageImpl.TYPE:
                decoded = CustomMessageImpl.decode(content, id);
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

        return GsonProvider.normal().toJson(json);
    }

    private void processIncomingMessage(Message message) {
        if (message instanceof UpdateMessage) {
            UpdateMessage msg = (UpdateMessage) message;
            UUID msgId = msg.getId();

            if (this.plugin.getEventDispatcher().dispatchNetworkPreSync(false, msgId, SyncType.FULL, null)) {
                return;
            }

            this.plugin.getLogger().info("[Messaging] Received update ping with id: " + msgId);
            this.plugin.getSyncTaskBuffer().request()
                    .thenRunAsync(() -> this.plugin.getEventDispatcher().dispatchNetworkPostSync(msgId, SyncType.FULL, true, null));

        } else if (message instanceof UserUpdateMessage) {
            UserUpdateMessage msg = (UserUpdateMessage) message;
            UUID msgId = msg.getId();
            UUID userUniqueId = msg.getUserUniqueId();

            if (this.plugin.getEventDispatcher().dispatchNetworkPreSync(false, msgId, SyncType.SPECIFIC_USER, userUniqueId)) {
                return;
            }

            User user = this.plugin.getUserManager().getIfLoaded(userUniqueId);
            if (user == null) {
                this.plugin.getEventDispatcher().dispatchNetworkPostSync(msgId, SyncType.SPECIFIC_USER, false, userUniqueId);
                return;
            }

            this.plugin.getLogger().info("[Messaging] Received user update ping for '" + user.getPlainDisplayName() + "' with id: " + msgId);
            this.plugin.getStorage().loadUser(user.getUniqueId(), null)
                    .thenRunAsync(() -> this.plugin.getEventDispatcher().dispatchNetworkPostSync(msgId, SyncType.SPECIFIC_USER, true, userUniqueId));
            
        } else if (message instanceof ActionLogMessage) {
            ActionLogMessage msg = (ActionLogMessage) message;

            this.plugin.getEventDispatcher().dispatchLogReceive(msg.getId(), msg.getAction());
            this.plugin.getLogDispatcher().dispatchFromRemote((LoggedAction) msg.getAction());

        } else if (message instanceof CustomMessage) {
            CustomMessage msg = (CustomMessage) message;

            this.plugin.getEventDispatcher().dispatchCustomMessageReceive(msg.getChannelId(), msg.getPayload());

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
