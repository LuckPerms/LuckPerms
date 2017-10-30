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

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An extended version of {@link LogEntry}, with helper methods for
 * populating and using the entry using internal LuckPerms classes.
 */
public class ExtendedLogEntry extends LogEntry {
    public static ExtendedLogEntryBuilder build() {
        return new ExtendedLogEntryBuilder();
    }

    public ExtendedLogEntry copy() {
        return (ExtendedLogEntry) super.copy();
    }

    public void submit(LuckPermsPlugin plugin, Sender sender) {
        plugin.getLogDispatcher().dispatch(this, sender);
    }

    public static JsonObject serializeWithId(UUID id, LogEntry entry) {
        JsonObject data = new JsonObject();

        data.add("id", new JsonPrimitive(id.toString()));
        data.add("actor", new JsonPrimitive(entry.getActor().toString()));
        data.add("actorName", new JsonPrimitive(entry.getActorName()));
        data.add("type", new JsonPrimitive(entry.getEntryType().name()));
        if (entry.getActed() != null) {
            data.add("acted", new JsonPrimitive(entry.getActed().toString()));
        }
        data.add("actedName", new JsonPrimitive(entry.getActedName()));
        data.add("action", new JsonPrimitive(entry.getAction()));

        return data;
    }

    public static Map.Entry<UUID, LogEntry> deserialize(JsonObject object) {
        LogEntry.LogEntryBuilder builder = LogEntry.builder();

        UUID id = UUID.fromString(object.get("id").getAsString());

        builder.actor(UUID.fromString(object.get("actor").getAsString()));
        builder.actorName(object.get("actorName").getAsString());
        builder.entryType(Type.valueOf(object.get("type").getAsString()));
        if (object.has("acted")) {
            builder.actor(UUID.fromString(object.get("acted").getAsString()));
        }
        builder.actedName(object.get("actedName").getAsString());
        builder.action(object.get("action").getAsString());

        return Maps.immutableEntry(id, builder.build());
    }

    public static class ExtendedLogEntryBuilder extends AbstractLogEntryBuilder<ExtendedLogEntry, ExtendedLogEntryBuilder> {

        @Override
        protected ExtendedLogEntry createEmptyLog() {
            return new ExtendedLogEntry();
        }

        @Override
        protected ExtendedLogEntryBuilder getThisBuilder() {
            return this;
        }

        public ExtendedLogEntryBuilder actor(Sender actor) {
            super.actorName(actor.getName());
            super.actor(actor.getUuid());
            return this;
        }

        public ExtendedLogEntryBuilder acted(PermissionHolder acted) {
            if (acted instanceof User) {
                super.actedName(((User) acted).getName().orElse("null"));
                super.acted(((User) acted).getUuid());
                super.entryType(Type.USER);
            } else if (acted instanceof Group) {
                super.actedName(((Group) acted).getName());
                super.entryType(Type.GROUP);
            }
            return this;
        }

        public ExtendedLogEntryBuilder acted(Track track) {
            super.actedName(track.getName());
            super.entryType(Type.TRACK);
            return this;
        }

        public ExtendedLogEntryBuilder action(Object... args) {
            List<String> parts = new ArrayList<>();

            for (Object o : args) {

                // special formatting for ContextSets instead of just #toString
                if (o instanceof ContextSet) {
                    ContextSet set = (ContextSet) o;

                    for (String value : set.getValues("server")) {
                        parts.add("server=" + value);
                    }
                    for (String value : set.getValues("world")) {
                        parts.add("world=" + value);
                    }

                    for (Map.Entry<String, String> context : set.toSet()) {
                        if (context.getKey().equals("server") || context.getKey().equals("world")) {
                            continue;
                        }
                        parts.add(context.getKey() + "=" + context.getValue());
                    }
                } else {
                    parts.add(String.valueOf(o));
                }
            }

            super.action(parts.stream().collect(Collectors.joining(" ")));
            return this;
        }

        @Override
        public ExtendedLogEntry build() {
            if (getTimestamp() == 0L) {
                super.timestamp(DateUtil.unixSecondsNow());
            }

            return super.build();
        }
    }
}
