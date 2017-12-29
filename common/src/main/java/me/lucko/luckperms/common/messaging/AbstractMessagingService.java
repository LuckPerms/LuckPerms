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

import lombok.Getter;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * An abstract implementation of {@link me.lucko.luckperms.api.MessagingService}.
 */
public abstract class AbstractMessagingService implements ExtendedMessagingService {
    protected static final String CHANNEL = "lpuc";

    private static final String UPDATE_HEADER = "update:";
    private static final String USER_UPDATE_HEADER = "userupdate:";
    private static final String LOG_HEADER = "log";

    @Getter
    private final LuckPermsPlugin plugin;

    @Getter
    private final String name;

    private final Set<UUID> receivedMessages;
    private final Gson gson;

    @Getter
    private final BufferedRequest<Void> updateBuffer;

    public AbstractMessagingService(LuckPermsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        this.receivedMessages = Collections.synchronizedSet(new HashSet<>());
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
        this.updateBuffer = new PushUpdateBuffer(plugin);
    }

    protected abstract void sendMessage(String message);

    protected void onMessage(String msg, Consumer<String> callback) {
        if (msg.startsWith(UPDATE_HEADER) && msg.length() > UPDATE_HEADER.length()) {
            String content = msg.substring(UPDATE_HEADER.length());

            UUID requestId = uuidFromString(content);
            if (requestId == null) {
                return;
            }

            if (!receivedMessages.add(requestId)) {
                return;
            }

            plugin.getLog().info("[" + name + " Messaging] Received update ping with id: " + requestId.toString());

            if (plugin.getApiProvider().getEventFactory().handleNetworkPreSync(false, requestId)) {
                return;
            }

            plugin.getUpdateTaskBuffer().request();

            if (callback != null) {
                callback.accept(msg);
            }

        } else if (msg.startsWith(USER_UPDATE_HEADER) && msg.length() > USER_UPDATE_HEADER.length()) {
            String content = msg.substring(USER_UPDATE_HEADER.length());

            Map.Entry<UUID, UUID> entry = uuidsFromString(content);
            if (entry == null) {
                return;
            }

            UUID requestId = entry.getKey();
            UUID userUuid = entry.getValue();

            if (!receivedMessages.add(requestId)) {
                return;
            }

            User user = plugin.getUserManager().getIfLoaded(userUuid);
            if (user == null) {
                return;
            }

            plugin.getLog().info("[" + name + " Messaging] Received user update ping for '" + user.getFriendlyName() + "' with id: " + uuidToString(requestId));

            if (plugin.getApiProvider().getEventFactory().handleNetworkPreSync(false, requestId)) {
                return;
            }

            plugin.getStorage().loadUser(user.getUuid(), null);

            if (callback != null) {
                callback.accept(msg);
            }

        } else if (msg.startsWith(LOG_HEADER) && msg.length() > LOG_HEADER.length()) {
            String content = msg.substring(LOG_HEADER.length());

            Map.Entry<String, ExtendedLogEntry> entry;
            try {
                entry = ExtendedLogEntry.deserialize(gson.fromJson(content, JsonObject.class));
            } catch (Exception e) {
                return;
            }

            if (entry.getKey() == null) {
                return;
            }

            UUID requestId = uuidFromString(entry.getKey());
            if (requestId == null) {
                return;
            }

            if (!receivedMessages.add(requestId)) {
                return;
            }

            plugin.getApiProvider().getEventFactory().handleLogReceive(requestId, entry.getValue());
            plugin.getLogDispatcher().dispatchFromRemote(entry.getValue());

            if (callback != null) {
                callback.accept(msg);
            }
        }
    }

    @Override
    public void pushUpdate() {
        plugin.getScheduler().doAsync(() -> {
            UUID requestId = generatePingId();
            String strId = uuidToString(requestId);

            plugin.getLog().info("[" + name + " Messaging] Sending ping with id: " + strId);
            sendMessage(UPDATE_HEADER + strId);
        });
    }

    @Override
    public void pushUserUpdate(User user) {
        plugin.getScheduler().doAsync(() -> {
            UUID requestId = generatePingId();
            String strId = uuidToString(requestId);

            plugin.getLog().info("[" + name + " Messaging] Sending user ping for '" + user.getFriendlyName() + "' with id: " + strId);
            sendMessage(USER_UPDATE_HEADER + uuidsToString(requestId, user.getUuid()));
        });
    }

    @Override
    public void pushLog(LogEntry logEntry) {
        plugin.getScheduler().doAsync(() -> {
            UUID requestId = generatePingId();
            String strId = uuidToString(requestId);

            if (plugin.getApiProvider().getEventFactory().handleLogNetworkPublish(!plugin.getConfiguration().get(ConfigKeys.PUSH_LOG_ENTRIES), requestId, logEntry)) {
                return;
            }

            plugin.getLog().info("[" + name + " Messaging] Sending log with id: " + strId);
            sendMessage(LOG_HEADER + gson.toJson(ExtendedLogEntry.serializeWithId(strId, logEntry)));
        });
    }

    private UUID generatePingId() {
        UUID uuid = UUID.randomUUID();
        receivedMessages.add(uuid);
        return uuid;
    }

    private static String uuidToString(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 2);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private static UUID uuidFromString(String s) {
        try {
            byte[] bytes = Base64.getDecoder().decode(s);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            return new UUID(buf.getLong(), buf.getLong());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String uuidsToString(UUID uuid1, UUID uuid2) {
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES * 4);
        buf.putLong(uuid1.getMostSignificantBits());
        buf.putLong(uuid1.getLeastSignificantBits());
        buf.putLong(uuid2.getMostSignificantBits());
        buf.putLong(uuid2.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buf.array());
    }

    private static Map.Entry<UUID, UUID> uuidsFromString(String s) {
        try {
            byte[] bytes = Base64.getDecoder().decode(s);
            ByteBuffer buf = ByteBuffer.wrap(bytes);
            UUID uuid1 = new UUID(buf.getLong(), buf.getLong());
            UUID uuid2 = new UUID(buf.getLong(), buf.getLong());
            return Maps.immutableEntry(uuid1, uuid2);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private final class PushUpdateBuffer extends BufferedRequest<Void> {
        public PushUpdateBuffer(LuckPermsPlugin plugin) {
            super(2000L, 200L, plugin.getScheduler().async());
        }

        @Override
        protected Void perform() {
            pushUpdate();
            return null;
        }
    }

}
