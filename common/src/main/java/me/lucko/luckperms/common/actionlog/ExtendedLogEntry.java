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

import com.google.common.base.Strings;

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * An implementation of {@link Action} and {@link Action.Builder},
 * with helper methods for populating and using the entry using internal
 * LuckPerms classes.
 */
public class ExtendedLogEntry implements Action {

    private static final Comparator<Action> COMPARATOR = Comparator
            .comparingLong(Action::getTimestamp)
            .thenComparing(Action::getActor)
            .thenComparing(Action::getActorName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Action::getType)
            .thenComparing(e -> e.getActed().map(UUID::toString).orElse(""))
            .thenComparing(Action::getActedName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Action::getAction);

    /**
     * Creates a new log entry builder
     *
     * @return a new builder
     */
    public static Builder build() {
        return new Builder();
    }

    private final long timestamp;
    private final UUID actor;
    private final String actorName;
    private final Type type;
    private final UUID acted;
    private final String actedName;
    private final String action;

    private ExtendedLogEntry(long timestamp, UUID actor, String actorName, Type type, UUID acted, String actedName, String action) {
        this.timestamp = timestamp;
        this.actor = actor;
        this.actorName = actorName;
        this.type = type;
        this.acted = acted;
        this.actedName = actedName;
        this.action = action;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public @NonNull UUID getActor() {
        return this.actor;
    }

    @Override
    public @NonNull String getActorName() {
        return this.actorName;
    }

    public String getActorFriendlyString() {
        if (Strings.isNullOrEmpty(this.actorName) || this.actorName.equals("null")) {
            return this.actor.toString();
        }
        return this.actorName;
    }

    @Override
    public @NonNull Type getType() {
        return this.type;
    }

    @Override
    public @NonNull Optional<UUID> getActed() {
        return Optional.ofNullable(this.acted);
    }

    @Override
    public @NonNull String getActedName() {
        return this.actedName;
    }

    public String getActedFriendlyString() {
        if (Strings.isNullOrEmpty(this.actedName) || this.actedName.equals("null")) {
            if (this.acted != null) {
                return this.acted.toString();
            }
        }
        return String.valueOf(this.actedName);
    }

    @Override
    public @NonNull String getAction() {
        return this.action;
    }

    @Override
    public int compareTo(@NonNull Action other) {
        Objects.requireNonNull(other, "other");
        return COMPARATOR.compare(this, other);
    }

    public boolean matchesSearch(String query) {
        query = Objects.requireNonNull(query, "query").toLowerCase();
        return this.actorName.toLowerCase().contains(query) ||
                this.actedName.toLowerCase().contains(query) ||
                this.action.toLowerCase().contains(query);
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
        if (!(o instanceof Action)) return false;
        final Action that = (Action) o;

        return this.getTimestamp() == that.getTimestamp() &&
                this.getActor().equals(that.getActor()) &&
                this.getActorName().equals(that.getActorName()) &&
                this.getType() == that.getType() &&
                this.getActed().equals(that.getActed()) &&
                this.getActedName().equals(that.getActedName()) &&
                this.getAction().equals(that.getAction());
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

    public static class Builder implements Action.Builder {

        private long timestamp = 0L;
        private UUID actor = null;
        private String actorName = null;
        private Type type = null;
        private UUID acted = null;
        private String actedName = null;
        private String action = null;

        public @NonNull Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public @NonNull Builder actor(@NonNull UUID actor) {
            this.actor = Objects.requireNonNull(actor, "actor");
            return this;
        }

        public @NonNull Builder actorName(@NonNull String actorName) {
            this.actorName = Objects.requireNonNull(actorName, "actorName");
            return this;
        }

        public @NonNull Builder type(@NonNull Type type) {
            this.type = Objects.requireNonNull(type, "type");
            return this;
        }

        public @NonNull Builder acted(UUID acted) {
            this.acted = acted; // nullable
            return this;
        }

        public @NonNull Builder actedName(@NonNull String actedName) {
            this.actedName = Objects.requireNonNull(actedName, "actedName");
            return this;
        }

        public @NonNull Builder action(@NonNull String action) {
            this.action = Objects.requireNonNull(action, "action");
            return this;
        }

        public Builder actor(Sender actor) {
            actorName(actor.getNameWithLocation());
            actor(actor.getUuid());
            return this;
        }

        public Builder acted(PermissionHolder acted) {
            if (acted.getType() == HolderType.USER) {
                actedName(((User) acted).getName().orElse("null"));
                acted(((User) acted).getUuid());
                type(Type.USER);
            } else if (acted.getType() == HolderType.GROUP) {
                actedName(((Group) acted).getName());
                type(Type.GROUP);
            }
            return this;
        }

        public Builder acted(Track track) {
            actedName(track.getName());
            type(Type.TRACK);
            return this;
        }

        public Builder action(Object... args) {
            List<String> parts = new ArrayList<>();

            for (Object o : args) {

                // special formatting for ContextSets instead of just #toString
                if (o instanceof ContextSet) {
                    ContextSet set = (ContextSet) o;

                    for (String value : set.getValues(DefaultContextKeys.SERVER_KEY)) {
                        parts.add("server=" + value);
                    }
                    for (String value : set.getValues(DefaultContextKeys.WORLD_KEY)) {
                        parts.add("world=" + value);
                    }

                    for (Map.Entry<String, String> context : set.toSet()) {
                        if (context.getKey().equals(DefaultContextKeys.SERVER_KEY) || context.getKey().equals(DefaultContextKeys.WORLD_KEY)) {
                            continue;
                        }
                        parts.add(context.getKey() + "=" + context.getValue());
                    }
                } else {
                    parts.add(String.valueOf(o));
                }
            }

            action(String.join(" ", parts));
            return this;
        }

        @Override
        public @NonNull ExtendedLogEntry build() {
            if (this.timestamp == 0L) {
                timestamp(System.currentTimeMillis() / 1000L);
            }

            Objects.requireNonNull(this.actor, "actor");
            Objects.requireNonNull(this.actorName, "actorName");
            Objects.requireNonNull(this.type, "type");
            Objects.requireNonNull(this.actedName, "actedName");
            Objects.requireNonNull(this.action, "action");

            return new ExtendedLogEntry(this.timestamp, this.actor, this.actorName, this.type, this.acted, this.actedName, this.action);
        }

        @Override
        public String toString() {
            return "ExtendedLogEntry.Builder(" +
                    "timestamp=" + this.timestamp + ", " +
                    "actor=" + this.actor + ", " +
                    "actorName=" + this.actorName + ", " +
                    "type=" + this.type + ", " +
                    "acted=" + this.acted + ", " +
                    "actedName=" + this.actedName + ", " +
                    "action=" + this.action + ")";
        }
    }
}
