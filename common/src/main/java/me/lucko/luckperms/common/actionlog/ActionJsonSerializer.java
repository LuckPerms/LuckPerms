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

package me.lucko.luckperms.common.actionlog;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.lucko.luckperms.common.util.gson.JObject;
import net.luckperms.api.actionlog.Action;

import java.time.Instant;
import java.util.UUID;

public final class ActionJsonSerializer {
    private ActionJsonSerializer() {}

    public static JsonObject serialize(Action logEntry) {
        return new JObject()
                .add("timestamp", new JsonPrimitive(logEntry.getTimestamp().getEpochSecond()))
                .add("source", new JObject()
                        .add("uniqueId", new JsonPrimitive(logEntry.getSource().getUniqueId().toString()))
                        .add("name", new JsonPrimitive(logEntry.getSource().getName()))
                )
                .add("target", new JObject()
                        .add("type", new JsonPrimitive(logEntry.getTarget().getType().name()))
                        .consume(obj -> {
                            if (logEntry.getTarget().getUniqueId().isPresent()) {
                                obj.add("uniqueId", new JsonPrimitive(logEntry.getTarget().getUniqueId().get().toString()));
                            }
                        })
                        .add("name", new JsonPrimitive(logEntry.getTarget().getName()))
                )
                .add("description", new JsonPrimitive(logEntry.getDescription()))
                .toJson();
    }

    public static LoggedAction deserialize(JsonElement element) {
        Preconditions.checkArgument(element.isJsonObject());
        JsonObject data = element.getAsJsonObject();

        LoggedAction.Builder builder = LoggedAction.build();

        if (data.has("timestamp")) { // sigh - this wasn't included in the first implementations
            builder.timestamp(Instant.ofEpochSecond(data.get("timestamp").getAsLong()));
        }

        if (data.has("source")) {
            JsonObject source = data.get("source").getAsJsonObject();
            builder.source(UUID.fromString(source.get("uniqueId").getAsString()));
            builder.sourceName(source.get("name").getAsString());
        } else {
            builder.source(UUID.fromString(data.get("actor").getAsString()));
            builder.sourceName(data.get("actorName").getAsString());
        }

        if (data.has("target")) {
            JsonObject target = data.get("target").getAsJsonObject();
            builder.targetType(LoggedAction.parseType(target.get("type").getAsString()));
            if (target.has("uniqueId")) {
                builder.target(UUID.fromString(target.get("uniqueId").getAsString()));
            }
            builder.targetName(target.get("name").getAsString());
        } else {
            builder.targetType(LoggedAction.parseType(data.get("type").getAsString()));
            if (data.has("acted")) {
                builder.target(UUID.fromString(data.get("acted").getAsString()));
            }
            builder.targetName(data.get("actedName").getAsString());
        }

        if (data.has("description")) {
            builder.description(data.get("description").getAsString());
        } else {
            builder.description(data.get("action").getAsString());
        }

        return builder.build();
    }

}
