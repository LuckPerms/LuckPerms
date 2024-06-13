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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of {@link Action} and {@link Action.Builder},
 * with helper methods for populating and using the entry using internal
 * LuckPerms classes.
 */
public class LoggedAction implements Action {

    /**
     * Creates a new log entry builder
     *
     * @return a new builder
     */
    public static Builder build() {
        return new Builder();
    }

    private final long timestamp;
    private final SourceImpl source;
    private final TargetImpl target;
    private final String description;

    private LoggedAction(long timestamp, UUID sourceUniqueId, String sourceName, UUID targetUniqueId, String targetName, Target.Type targetType, String description) {
        this.timestamp = timestamp;
        this.source = new SourceImpl(sourceUniqueId, sourceName);
        this.target = new TargetImpl(targetUniqueId, targetName, targetType);
        this.description = description;
    }

    @Override
    public @NonNull Instant getTimestamp() {
        return Instant.ofEpochSecond(this.timestamp);
    }

    public Duration getDurationSince() {
        return Duration.between(getTimestamp(), Instant.now());
    }

    @Override
    public @NonNull Source getSource() {
        return this.source;
    }

    @Override
    public @NonNull Target getTarget() {
        return this.target;
    }

    public String getSourceFriendlyString() {
        if (Strings.isNullOrEmpty(this.source.name) || this.source.name.equals("null")) {
            return this.source.uniqueId.toString();
        }
        return this.source.name;
    }

    public String getTargetFriendlyString() {
        if (Strings.isNullOrEmpty(this.target.name) || this.target.name.equals("null")) {
            if (this.target.uniqueId != null) {
                return this.target.uniqueId.toString();
            }
        }
        return String.valueOf(this.target.name);
    }

    @Override
    public @NonNull String getDescription() {
        return this.description;
    }

    @Override
    public int compareTo(@NonNull Action other) {
        Objects.requireNonNull(other, "other");
        return ActionComparator.INSTANCE.compare(this, other);
    }

    public void submit(LuckPermsPlugin plugin, Sender sender) {
        CompletableFuture<Void> future = plugin.getLogDispatcher().dispatch(this, sender);
        if (plugin.getConfiguration().get(ConfigKeys.LOG_SYNCHRONOUSLY_IN_COMMANDS)) {
            future.join();
        }
    }

    @Override
    public String toString() {
        return "LoggedAction(" +
                "timestamp=" + this.getTimestamp() + ", " +
                "source=" + getSource().getUniqueId() + ", " +
                "sourceName=" + getSource().getName() + ", " +
                "target=" + getTarget().getUniqueId() + ", " +
                "targetName=" + getTarget().getName() + ", " +
                "targetType=" + getTarget().getType() + ", " +
                "description=" + this.getDescription() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Action)) return false;
        final Action that = (Action) o;

        return getTimestamp().equals(that.getTimestamp()) &&
                getSource().equals(that.getSource()) &&
                getTarget().equals(that.getTarget()) &&
                getDescription().equals(that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTimestamp(), getSource(), getTarget(), getDescription());
    }

    private static final class SourceImpl implements Source {
        private final UUID uniqueId;
        private final String name;

        private SourceImpl(UUID uniqueId, String name) {
            this.uniqueId = uniqueId;
            this.name = name;
        }

        @Override
        public @NonNull UUID getUniqueId() {
            return this.uniqueId;
        }

        @Override
        public @NonNull String getName() {
            return this.name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SourceImpl source = (SourceImpl) o;
            return this.uniqueId.equals(source.uniqueId) &&
                    this.name.equals(source.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uniqueId, this.name);
        }
    }

    private static final class TargetImpl implements Target {
        private final UUID uniqueId;
        private final String name;
        private final Type type;

        private TargetImpl(UUID uniqueId, String name, Type type) {
            this.uniqueId = uniqueId;
            this.name = name;
            this.type = type;
        }

        @Override
        public @NonNull Optional<UUID> getUniqueId() {
            return Optional.ofNullable(this.uniqueId);
        }

        @Override
        public @NonNull String getName() {
            return this.name;
        }

        @Override
        public @NonNull Type getType() {
            return this.type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TargetImpl target = (TargetImpl) o;
            return Objects.equals(this.uniqueId, target.uniqueId) &&
                    this.name.equals(target.name) &&
                    this.type == target.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.uniqueId, this.name, this.type);
        }
    }

    public static class Builder implements Action.Builder {
        private long timestamp = 0L;
        private UUID sourceUniqueId = null;
        private String sourceName = null;
        private UUID targetUniqueId = null;
        private String targetName = null;
        private Target.Type targetType = null;
        private String description = null;

        @Override
        public @NonNull Builder timestamp(@NonNull Instant timestamp) {
            this.timestamp = timestamp.getEpochSecond();
            return this;
        }

        @Override
        public @NonNull Builder source(@NonNull UUID source) {
            this.sourceUniqueId = Objects.requireNonNull(source, "source");
            return this;
        }

        @Override
        public @NonNull Builder sourceName(@NonNull String sourceName) {
            this.sourceName = Objects.requireNonNull(sourceName, "sourceName");
            return this;
        }

        @Override
        public @NonNull Builder targetType(Target.@NonNull Type type) {
            this.targetType = Objects.requireNonNull(type, "type");
            return this;
        }

        @Override
        public @NonNull Builder target(UUID target) {
            this.targetUniqueId = target; // nullable
            return this;
        }

        @Override
        public @NonNull Builder targetName(@NonNull String targetName) {
            this.targetName = Objects.requireNonNull(targetName, "targetName");
            return this;
        }

        @Override
        public @NonNull Builder description(@NonNull String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder source(Sender source) {
            sourceName(source.getNameWithLocation());
            source(source.getUniqueId());
            return this;
        }

        public Builder target(PermissionHolder target) {
            if (target.getType() == HolderType.USER) {
                targetName(((User) target).getUsername().orElse("null"));
                target(((User) target).getUniqueId());
                targetType(Target.Type.USER);
            } else if (target.getType() == HolderType.GROUP) {
                targetName(((Group) target).getName());
                targetType(Target.Type.GROUP);
            }
            return this;
        }

        public Builder target(Track track) {
            targetName(track.getName());
            targetType(Target.Type.TRACK);
            return this;
        }

        public Builder description(Object... args) {
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

                    for (Context context : set) {
                        if (context.getKey().equals(DefaultContextKeys.SERVER_KEY) || context.getKey().equals(DefaultContextKeys.WORLD_KEY)) {
                            continue;
                        }
                        parts.add(context.getKey() + "=" + context.getValue());
                    }
                } else if (o instanceof Duration) {
                    parts.add(DurationFormatter.CONCISE.formatString((Duration) o));
                } else {
                    parts.add(String.valueOf(o));
                }
            }

            description(String.join(" ", parts));
            return this;
        }

        @Override
        public @NonNull LoggedAction build() {
            if (this.timestamp == 0L) {
                timestamp(Instant.now());
            }

            Objects.requireNonNull(this.sourceUniqueId, "sourceUniqueId");
            Objects.requireNonNull(this.sourceName, "sourceName");
            Objects.requireNonNull(this.targetType, "targetType");
            Objects.requireNonNull(this.targetName, "targetName");
            Objects.requireNonNull(this.description, "description");

            return new LoggedAction(this.timestamp, this.sourceUniqueId, this.sourceName, this.targetUniqueId, this.targetName, this.targetType, this.description);
        }
    }

    public static Target.@NonNull Type parseType(String type) {
        try {
            return Target.Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        try {
            return parseTypeCharacter(type.charAt(0));
        } catch (IllegalArgumentException e) {
            // ignore
        }
        throw new IllegalArgumentException("Unknown type: " + type);
    }

    public static Target.@NonNull Type parseTypeCharacter(char code) {
        switch (code) {
            case 'U':
            case 'u':
                return Target.Type.USER;
            case 'G':
            case 'g':
                return Target.Type.GROUP;
            case 'T':
            case 't':
                return Target.Type.TRACK;
            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }

    public static String getTypeString(Target.Type type) {
        switch (type) {
            case USER:
                return "U";
            case GROUP:
                return "G";
            case TRACK:
                return "T";
            default:
                throw new AssertionError();
        }
    }
}
