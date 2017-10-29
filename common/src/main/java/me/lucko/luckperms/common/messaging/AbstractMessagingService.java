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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

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
        this.gson = new Gson();
        this.updateBuffer = new PushUpdateBuffer(plugin);
    }

    protected abstract void sendMessage(String message);

    protected void onMessage(String msg, Consumer<String> callback) {
        if (msg.startsWith("update:") && msg.length() > "update:".length()) {
            UUID uuid = parseUpdateMessage(msg);
            if (uuid == null) {
                return;
            }

            if (!receivedMessages.add(uuid)) {
                return;
            }

            plugin.getLog().info("[" + name + " Messaging] Received update ping with id: " + uuid.toString());

            if (plugin.getApiProvider().getEventFactory().handleNetworkPreSync(false, uuid)) {
                return;
            }

            plugin.getUpdateTaskBuffer().request();

            if (callback != null) {
                callback.accept(msg);
            }

        } else if (msg.startsWith("log:") && msg.length() > "log:".length()) {
            String logData = msg.substring("log:".length());
            Map.Entry<UUID, LogEntry> entry = null;
            try {
                entry = ExtendedLogEntry.deserialize(gson.fromJson(logData, JsonObject.class));
            } catch (Exception e) {
                plugin.getLog().warn("Error whilst deserializing log: " + logData);
                e.printStackTrace();
            }

            if (entry == null) {
                return;
            }

            if (!receivedMessages.add(entry.getKey())) {
                return;
            }

            plugin.getApiProvider().getEventFactory().handleLogReceive(entry.getKey(), entry.getValue());
            plugin.getLogDispatcher().dispatchFromRemote(entry.getValue());

            if (callback != null) {
                callback.accept(msg);
            }
        }
    }

    @Override
    public void pushLog(LogEntry logEntry) {
        plugin.getScheduler().doAsync(() -> {
            UUID id = generatePingId();

            if (plugin.getApiProvider().getEventFactory().handleLogNetworkPublish(!plugin.getConfiguration().get(ConfigKeys.PUSH_LOG_ENTRIES), id, logEntry)) {
                return;
            }

            plugin.getLog().info("[" + name + " Messaging] Sending log with id: " + id.toString());
            sendMessage("log:" + gson.toJson(ExtendedLogEntry.serializeWithId(id, logEntry)));
        });
    }

    @Override
    public void pushUpdate() {
        plugin.getScheduler().doAsync(() -> {
            UUID id = generatePingId();
            plugin.getLog().info("[" + name + " Messaging] Sending ping with id: " + id.toString());

            sendMessage("update:" + id.toString());
        });
    }

    private UUID generatePingId() {
        UUID uuid = UUID.randomUUID();
        receivedMessages.add(uuid);
        return uuid;
    }

    private static UUID parseUpdateMessage(String msg) {
        String requestId = msg.substring("update:".length());
        try {
            return UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private final class PushUpdateBuffer extends BufferedRequest<Void> {
        public PushUpdateBuffer(LuckPermsPlugin plugin) {
            super(3000L, 200L, plugin.getScheduler().async());
        }

        @Override
        protected Void perform() {
            pushUpdate();
            return null;
        }
    }

}
