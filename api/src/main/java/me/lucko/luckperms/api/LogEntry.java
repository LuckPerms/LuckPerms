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

package me.lucko.luckperms.api;

import com.google.common.base.Preconditions;

import java.util.Comparator;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A single entry in the log.
 */
public class LogEntry implements Comparable<LogEntry> {

    /**
     * Compares two LogEntries
     *
     * @since 3.3
     */
    public static final Comparator<LogEntry> COMPARATOR = Comparator
            .comparingLong(LogEntry::getTimestamp)
            .thenComparing(LogEntry::getActor)
            .thenComparing(LogEntry::getActorName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(LogEntry::getEntryType)
            .thenComparing(e -> {
                UUID u = e.getActed();
                return u == null ? "" : u.toString();
            })
            .thenComparing(LogEntry::getActedName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(LogEntry::getAction);

    /**
     * Compares two LogEntries in reverse order
     *
     * @since 3.3
     * @see #COMPARATOR
     */
    public static final Comparator<LogEntry> REVERSE_ORDER = COMPARATOR.reversed();

    private static final String FORMAT = "&8(&e%s&8) [&a%s&8] (&b%s&8) &7--> &f%s";

    /**
     * Creates a new LogEntry builder
     *
     * @return a new builder
     */
    @Nonnull
    public static LogEntryBuilder builder() {
        return new LogEntryBuilder();
    }

    /**
     * The time when the log event occurred in unix seconds.
     */
    private long timestamp;

    /**
     * The player who enacted the change
     */
    @Nonnull
    private UUID actor;

    /**
     * The name of the player who enacted the change
     */
    @Nonnull
    private String actorName;

    /**
     * The entry type.
     */
    @Nonnull
    private Type type;

    /**
     * The user who was acted upon
     */
    @Nullable
    private UUID acted;

    /**
     * The name of the object who was acted upon
     */
    @Nonnull
    private String actedName;

    /**
     * A description of the action
     */
    @Nonnull
    private String action;

    /**
     * Creates a new log entry
     *
     * @param timestamp the timestamp
     * @param actor the actor
     * @param actorName the actorName
     * @param type the type
     * @param acted the acted object, or null
     * @param actedName the acted object name
     * @param action the action
     * @since 3.3
     */
    public LogEntry(long timestamp, @Nonnull UUID actor, @Nonnull String actorName, @Nonnull Type type, @Nullable UUID acted, @Nonnull String actedName, @Nonnull String action) {
        Preconditions.checkNotNull(actor, "actor");
        Preconditions.checkNotNull(actorName, "actorName");
        Preconditions.checkNotNull(type, "type");
        Preconditions.checkNotNull(actedName, "actedName");
        Preconditions.checkNotNull(action, "action");

        this.timestamp = timestamp;
        this.actor = actor;
        this.actorName = actorName;
        this.type = type;
        this.acted = acted;
        this.actedName = actedName;
        this.action = action;
    }

    /**
     * Creates a new log entry
     *
     * @param timestamp the timestamp
     * @param actor the actor
     * @param actorName the actorName
     * @param type the type code
     * @param acted the acted object, or null
     * @param actedName the acted object name
     * @param action the action
     */
    public LogEntry(long timestamp, @Nonnull UUID actor, @Nonnull String actorName, char type, @Nullable UUID acted, @Nonnull String actedName, @Nonnull String action) {
        Preconditions.checkNotNull(actor, "actor");
        Preconditions.checkNotNull(actorName, "actorName");
        Preconditions.checkNotNull(actedName, "actedName");
        Preconditions.checkNotNull(action, "action");

        this.timestamp = timestamp;
        this.actor = actor;
        this.actorName = actorName;
        this.type = Type.valueOf(type);
        this.acted = acted;
        this.actedName = actedName;
        this.action = action;
    }

    /**
     * Creates a new LogEntry and copies the values from another
     *
     * @param other the entry to copy values from
     * @since 3.3
     */
    protected LogEntry(@Nonnull LogEntry other) {
        this.timestamp = other.timestamp;
        this.actor = other.actor;
        this.actorName = other.actorName;
        this.type = other.type;
        this.acted = other.acted;
        this.actedName = other.actedName;
        this.action = other.action;
    }

    protected LogEntry() {
        this.timestamp = 0L;
        this.actor = null;
        this.actorName = null;
        this.type = null;
        this.acted = null;
        this.actedName = null;
        this.action = null;
    }

    @Override
    public int compareTo(@Nonnull LogEntry other) {
        Preconditions.checkNotNull(other, "other");
        return COMPARATOR.compare(this, other);
    }

    /**
     * Creates a copy of this log entry
     *
     * @return a copy of this log entry
     * @since 3.3
     */
    public LogEntry copy() {
        return new LogEntry(this);
    }

    public long getTimestamp() {
        return timestamp;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Nonnull
    public UUID getActor() {
        return Preconditions.checkNotNull(actor, "actor");
    }

    void setActor(@Nonnull UUID actor) {
        this.actor = Preconditions.checkNotNull(actor, "actor");
    }

    @Nonnull
    public String getActorName() {
        return Preconditions.checkNotNull(actorName, "actorName");
    }

    void setActorName(@Nonnull String actorName) {
        this.actorName = Preconditions.checkNotNull(actorName, "actorName");
    }

    /**
     * Gets the type of this entry
     *
     * @return the type of this entry
     * @since 3.3
     */
    @Nonnull
    public Type getEntryType() {
        return Preconditions.checkNotNull(type, "type");
    }

    /**
     * Sets the type of this entry
     *
     * @param type the new type
     * @since 3.3
     */
    public void setEntryType(@Nonnull Type type) {
        this.type = Preconditions.checkNotNull(type, "type");
    }

    /**
     * Gets the code representing this entry type
     *
     * @return the code representing this entry type
     * @deprecated in favour of {@link #getEntryType()}
     */
    @Deprecated
    public char getType() {
        return getEntryType().getCode();
    }

    /**
     * Sets the type of this entry by code
     *
     * @param code the code type
     * @deprecated in favour of {@link #setEntryType(Type)}
     */
    @Deprecated
    void setType(char code) {
        setEntryType(Type.valueOf(code));
    }

    @Nullable
    public UUID getActed() {
        return acted;
    }

    void setActed(@Nullable UUID acted) {
        this.acted = acted;
    }

    @Nonnull
    public String getActedName() {
        return Preconditions.checkNotNull(actedName, "actedName");
    }

    void setActedName(@Nonnull String actedName) {
        this.actedName = Preconditions.checkNotNull(actedName, "actedName");
    }

    @Nonnull
    public String getAction() {
        return Preconditions.checkNotNull(action, "action");
    }

    void setAction(@Nonnull String action) {
        this.action = Preconditions.checkNotNull(action, "action");
    }

    public boolean matchesSearch(@Nonnull String query) {
        query = Preconditions.checkNotNull(query, "query").toLowerCase();
        return actorName.toLowerCase().contains(query) ||
                actedName.toLowerCase().contains(query) ||
                action.toLowerCase().contains(query);
    }

    @Nonnull
    public String getFormatted() {
        return String.format(FORMAT,
                String.valueOf(actorName).equals("null") ? actor.toString() : actorName,
                Character.toString(type.getCode()),
                String.valueOf(actedName).equals("null") && acted != null ? acted.toString() : actedName,
                action
        );
    }

    @Override
    public String toString() {
        return "LogEntry(" +
                "timestamp=" + this.getTimestamp() + ", " +
                "actor=" + this.getActor() + ", " +
                "actorName=" + this.getActorName() + ", " +
                "type=" + this.getEntryType() + ", " +
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
                this.getEntryType() == other.getEntryType() &&
                (this.getActed() == null ? other.getActed() == null : this.getActed().equals(other.getActed())) &&
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
        result = result * PRIME + this.getEntryType().hashCode();
        result = result * PRIME + (this.getActed() == null ? 43 : this.getActed().hashCode());
        result = result * PRIME + this.getActedName().hashCode();
        result = result * PRIME + this.getAction().hashCode();
        return result;
    }

    /**
     * The LogEntry type
     * @since 3.3
     */
    public enum Type {
        USER('U'),
        GROUP('G'),
        TRACK('T');

        private final char code;

        Type(char code) {
            this.code = code;
        }

        public char getCode() {
            return this.code;
        }

        public static Type valueOf(char code) {
            switch (code) {
                case 'U':
                case 'u':
                    return USER;
                case 'G':
                case 'g':
                    return GROUP;
                case 'T':
                case 't':
                    return TRACK;
                default:
                    throw new IllegalArgumentException("Unknown code: " + code);
            }
        }
    }

    /**
     * Builds LogEntry instances
     */
    public static class LogEntryBuilder extends AbstractLogEntryBuilder<LogEntry, LogEntry.LogEntryBuilder> {

        @Override
        protected LogEntry createEmptyLog() {
            return new LogEntry();
        }

        @Override
        protected LogEntryBuilder getThisBuilder() {
            return this;
        }

    }

    /**
     * An abstract log entry builder
     *
     * @param <T> the log type
     * @param <B> the log builder type
     */
    public static abstract class AbstractLogEntryBuilder<T extends LogEntry, B extends AbstractLogEntryBuilder<T, B>> {
        private final T obj;
        private final B thisObj;

        public AbstractLogEntryBuilder() {
            obj = createEmptyLog();
            thisObj = getThisBuilder();
        }

        protected abstract T createEmptyLog();

        protected abstract B getThisBuilder();

        public long getTimestamp() {
            return obj.getTimestamp();
        }

        @Nonnull
        public UUID getActor() {
            return obj.getActor();
        }

        @Nonnull
        public String getActorName() {
            return obj.getActorName();
        }

        @Nonnull
        public Type getEntryType() {
            return obj.getEntryType();
        }

        @Nonnull
        @Deprecated
        public char getType() {
            return obj.getType();
        }

        @Nullable
        public UUID getActed() {
            return obj.getActed();
        }

        @Nonnull
        public String getActedName() {
            return obj.getActedName();
        }

        @Nonnull
        public String getAction() {
            return obj.getAction();
        }

        @Nonnull
        public B timestamp(long timestamp) {
            obj.setTimestamp(timestamp);
            return thisObj;
        }

        @Nonnull
        public B actor(@Nonnull UUID actor) {
            obj.setActor(actor);
            return thisObj;
        }

        @Nonnull
        public B actorName(@Nonnull String actorName) {
            obj.setActorName(actorName);
            return thisObj;
        }

        @Nonnull
        public B entryType(@Nonnull Type type) {
            obj.setEntryType(type);
            return thisObj;
        }

        @Nonnull
        @Deprecated
        public B type(char type) {
            obj.setType(type);
            return thisObj;
        }

        @Nonnull
        public B acted(@Nullable UUID acted) {
            obj.setActed(acted);
            return thisObj;
        }

        @Nonnull
        public B actedName(@Nonnull String actedName) {
            obj.setActedName(actedName);
            return thisObj;
        }

        @Nonnull
        public B action(@Nonnull String action) {
            obj.setAction(action);
            return thisObj;
        }

        @Nonnull
        public T build() {
            Preconditions.checkNotNull(getActor(), "actor");
            Preconditions.checkNotNull(getActorName(), "actorName");
            Preconditions.checkNotNull(getEntryType(), "type");
            Preconditions.checkNotNull(getActedName(), "actedName");
            Preconditions.checkNotNull(getAction(), "action");

            return obj;
        }

        @Override
        public String toString() {
            return "LogEntry.LogEntryBuilder(" +
                    "timestamp=" + this.getTimestamp() + ", " +
                    "actor=" + this.getActor() + ", " +
                    "actorName=" + this.getActorName() + ", " +
                    "type=" + this.getEntryType() + ", " +
                    "acted=" + this.getActed() + ", " +
                    "actedName=" + this.getActedName() + ", " +
                    "action=" + this.getAction() + ")";
        }
    }

}
