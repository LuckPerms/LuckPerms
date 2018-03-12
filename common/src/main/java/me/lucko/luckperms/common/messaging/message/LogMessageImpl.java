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

package me.lucko.luckperms.common.messaging.message;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.messenger.message.type.LogMessage;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.Nonnull;

public class LogMessageImpl extends AbstractMessage implements LogMessage {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String LOG_HEADER = "log";

    public static LogMessageImpl decode(String msg) {
        if (msg.startsWith(LOG_HEADER) && msg.length() > LOG_HEADER.length()) {
            String content = msg.substring(LOG_HEADER.length());

            try {
                return decodeContent(GSON.fromJson(content, JsonObject.class));
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private final LogEntry logEntry;

    public LogMessageImpl(UUID id, LogEntry logEntry) {
        super(id);
        this.logEntry = logEntry;
    }

    @Nonnull
    @Override
    public LogEntry getLogEntry() {
        return this.logEntry;
    }

    @Nonnull
    @Override
    public String asEncodedString() {
        return LOG_HEADER + GSON.toJson(encodeContent(uuidToString(getId()), this.logEntry));
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

    private static JsonObject encodeContent(String id, LogEntry entry) {
        JsonObject data = new JsonObject();

        data.add("id", new JsonPrimitive(id));
        data.add("actor", new JsonPrimitive(entry.getActor().toString()));
        data.add("actorName", new JsonPrimitive(entry.getActorName()));
        data.add("type", new JsonPrimitive(entry.getType().name()));
        if (entry.getActed().isPresent()) {
            data.add("acted", new JsonPrimitive(entry.getActed().get().toString()));
        }
        data.add("actedName", new JsonPrimitive(entry.getActedName()));
        data.add("action", new JsonPrimitive(entry.getAction()));

        return data;
    }

    private static LogMessageImpl decodeContent(JsonObject object) {
        ExtendedLogEntry.Builder builder = ExtendedLogEntry.build();

        String id = object.get("id").getAsString();
        if (id == null) {
            return null;
        }

        UUID uuid = uuidFromString(id);
        if (uuid == null) {
            return null;
        }

        builder.actor(UUID.fromString(object.get("actor").getAsString()));
        builder.actorName(object.get("actorName").getAsString());
        builder.type(LogEntry.Type.valueOf(object.get("type").getAsString()));
        if (object.has("acted")) {
            builder.actor(UUID.fromString(object.get("acted").getAsString()));
        }
        builder.actedName(object.get("actedName").getAsString());
        builder.action(object.get("action").getAsString());

        return new LogMessageImpl(uuid, builder.build());
    }

}
