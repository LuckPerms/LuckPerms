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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;

import com.google.common.base.Preconditions;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * An implementation of {@link LogEntry} and {@link LogEntry.Builder},
 * with helper methods for populating and using the entry using internal
 * LuckPerms classes.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExtendedLogEntry implements LogEntry {

    private static final String FORMAT = "&8(&e%s&8) [&a%s&8] (&b%s&8) &7--> &f%s";

    /**
     * Compares two LogEntries
     *
     * @since 3.3
     */
    public static final Comparator<LogEntry> COMPARATOR = Comparator
            .comparingLong(LogEntry::getTimestamp)
            .thenComparing(LogEntry::getActor)
            .thenComparing(LogEntry::getActorName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(LogEntry::getType)
            .thenComparing(e -> e.getActed().map(UUID::toString).orElse(""))
            .thenComparing(LogEntry::getActedName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(LogEntry::getAction);

    /**
     * Compares two LogEntries in reverse order
     *
     * @since 3.3
     * @see #COMPARATOR
     */
    public static final Comparator<LogEntry> REVERSE_ORDER = COMPARATOR.reversed();

    /**
     * Creates a new log entry builder
     *
     * @return a new builder
     */
    public static ExtendedLogEntryBuilder build() {
        return new ExtendedLogEntryBuilder();
    }

    private final long timestamp;
    private final UUID actor;
    private final String actorName;
    private final Type type;
    private final UUID acted;
    private final String actedName;
    private final String action;

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public UUID getActor() {
        return actor;
    }

    @Override
    public String getActorName() {
        return actorName;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Optional<UUID> getActed() {
        return Optional.ofNullable(acted);
    }

    @Override
    public String getActedName() {
        return actedName;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public int compareTo(LogEntry other) {
        Preconditions.checkNotNull(other, "other");
        return COMPARATOR.compare(this, other);
    }

    public boolean matchesSearch(String query) {
        query = Preconditions.checkNotNull(query, "query").toLowerCase();
        return actorName.toLowerCase().contains(query) ||
                actedName.toLowerCase().contains(query) ||
                action.toLowerCase().contains(query);
    }

    public String getFormatted() {
        return String.format(FORMAT,
                String.valueOf(actorName).equals("null") ? actor.toString() : actorName,
                Character.toString(type.getCode()),
                String.valueOf(actedName).equals("null") && acted != null ? acted.toString() : actedName,
                action
        );
    }

    public void submit(LuckPermsPlugin plugin, Sender sender) {
        plugin.getLogDispatcher().dispatch(this, sender);
    }

    @Override
    public String toString() {
        return "LogEntry(" +
                "timestamp=" + this.getTimestamp() + ", " +
                "actor=" + this.getActor() + ", " +
                "actorName=" + this.getActorName() + ", " +
                "type=" + this.getType() + ", " +
                "acted=" + this.getActed() + ", " +
                "actedName=" + this.getActedName() + ", " +
                "action=" + this.getAction() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LogEntry)) return false;
        final LogEntry other = (LogEntry) o;

        return this.getTimestamp() == other.getTimestamp() &&
                this.getActor().equals(other.getActor()) &&
                this.getActorName().equals(other.getActorName()) &&
                this.getType() == other.getType() &&
                this.getActed().equals(other.getActed()) &&
                this.getActedName().equals(other.getActedName()) &&
                this.getAction().equals(other.getAction());
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (int) (this.getTimestamp() >>> 32 ^ this.getTimestamp());
        result = result * PRIME + this.getActor().hashCode();
        result = result * PRIME + this.getActorName().hashCode();
        result = result * PRIME + this.getType().hashCode();
        result = result * PRIME + this.getActed().hashCode();
        result = result * PRIME + this.getActedName().hashCode();
        result = result * PRIME + this.getAction().hashCode();
        return result;
    }

    @ToString
    public static class ExtendedLogEntryBuilder implements LogEntry.Builder {

        private long timestamp = 0L;
        private UUID actor = null;
        private String actorName = null;
        private Type type = null;
        private UUID acted = null;
        private String actedName = null;
        private String action = null;

        @Override
        public ExtendedLogEntryBuilder setTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setActor(UUID actor) {
            this.actor = Preconditions.checkNotNull(actor, "actor");
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setActorName(String actorName) {
            this.actorName = Preconditions.checkNotNull(actorName, "actorName");
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setType(Type type) {
            this.type = Preconditions.checkNotNull(type, "type");
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setActed(UUID acted) {
            this.acted = acted; // nullable
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setActedName(String actedName) {
            this.actedName = Preconditions.checkNotNull(actedName, "actedName");
            return this;
        }

        @Override
        public ExtendedLogEntryBuilder setAction(String action) {
            this.action = Preconditions.checkNotNull(action, "action");
            return this;
        }

        public ExtendedLogEntryBuilder timestamp(long timestamp) {
            return setTimestamp(timestamp);
        }

        public ExtendedLogEntryBuilder actor(UUID actor) {
            return setActor(actor);
        }

        public ExtendedLogEntryBuilder actorName(String actorName) {
            return setActorName(actorName);
        }

        public ExtendedLogEntryBuilder type(Type type) {
            return setType(type);
        }

        public ExtendedLogEntryBuilder acted(UUID acted) {
            return setActed(acted);
        }

        public ExtendedLogEntryBuilder actedName(String actedName) {
            return setActedName(actedName);
        }

        public ExtendedLogEntryBuilder action(String action) {
            return setAction(action);
        }

        public ExtendedLogEntryBuilder actor(Sender actor) {
            actorName(actor.getNameWithLocation());
            actor(actor.getUuid());
            return this;
        }

        public ExtendedLogEntryBuilder acted(PermissionHolder acted) {
            if (acted instanceof User) {
                actedName(((User) acted).getName().orElse("null"));
                acted(((User) acted).getUuid());
                type(Type.USER);
            } else if (acted instanceof Group) {
                actedName(((Group) acted).getName());
                type(Type.GROUP);
            }
            return this;
        }

        public ExtendedLogEntryBuilder acted(Track track) {
            actedName(track.getName());
            type(Type.TRACK);
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

            action(parts.stream().collect(Collectors.joining(" ")));
            return this;
        }

        @Override
        public ExtendedLogEntry build() {
            if (timestamp == 0L) {
                timestamp(DateUtil.unixSecondsNow());
            }

            Preconditions.checkNotNull(actor, "actor");
            Preconditions.checkNotNull(actorName, "actorName");
            Preconditions.checkNotNull(type, "type");
            Preconditions.checkNotNull(actedName, "actedName");
            Preconditions.checkNotNull(action, "action");

            return new ExtendedLogEntry(timestamp, actor, actorName, type, acted, actedName, action);
        }
    }

    public static JsonObject serializeWithId(UUID id, LogEntry entry) {
        JsonObject data = new JsonObject();

        data.add("id", new JsonPrimitive(id.toString()));
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

    public static Map.Entry<UUID, ExtendedLogEntry> deserialize(JsonObject object) {
        ExtendedLogEntryBuilder builder = build();

        UUID id = UUID.fromString(object.get("id").getAsString());

        builder.actor(UUID.fromString(object.get("actor").getAsString()));
        builder.actorName(object.get("actorName").getAsString());
        builder.type(Type.valueOf(object.get("type").getAsString()));
        if (object.has("acted")) {
            builder.actor(UUID.fromString(object.get("acted").getAsString()));
        }
        builder.actedName(object.get("actedName").getAsString());
        builder.action(object.get("action").getAsString());

        return Maps.immutableEntry(id, builder.build());
    }
}
