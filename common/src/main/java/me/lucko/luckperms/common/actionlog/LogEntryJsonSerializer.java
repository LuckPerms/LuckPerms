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

import me.lucko.luckperms.api.actionlog.Action;

import java.util.UUID;

public final class LogEntryJsonSerializer {
    private LogEntryJsonSerializer() {}

    public static JsonObject serialize(Action logEntry) {
        JsonObject data = new JsonObject();
        data.add("actor", new JsonPrimitive(logEntry.getActor().toString()));
        data.add("actorName", new JsonPrimitive(logEntry.getActorName()));
        data.add("type", new JsonPrimitive(logEntry.getType().name()));
        if (logEntry.getActed().isPresent()) {
            data.add("acted", new JsonPrimitive(logEntry.getActed().get().toString()));
        }
        data.add("actedName", new JsonPrimitive(logEntry.getActedName()));
        data.add("action", new JsonPrimitive(logEntry.getAction()));

        return data;
    }

    public static ExtendedLogEntry deserialize(JsonElement element) {
        Preconditions.checkArgument(element.isJsonObject());
        JsonObject data = element.getAsJsonObject();

        ExtendedLogEntry.Builder builder = ExtendedLogEntry.build();

        builder.actor(UUID.fromString(data.get("actor").getAsString()));
        builder.actorName(data.get("actorName").getAsString());
        builder.type(Action.Type.parse(data.get("type").getAsString()));
        if (data.has("acted")) {
            builder.actor(UUID.fromString(data.get("acted").getAsString()));
        }
        builder.actedName(data.get("actedName").getAsString());
        builder.action(data.get("action").getAsString());

        return builder.build();
    }

}
