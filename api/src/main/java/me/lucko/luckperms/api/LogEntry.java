/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import java.util.UUID;

/**
 * A single entry in the log
 *
 * <p> Implements {@link Comparable} ordering based upon the timestamp of the entry.
 */
public class LogEntry implements Comparable<LogEntry> {
    private static final String FORMAT = "&8(&e%s&8) [&a%s&8] (&b%s&8) &7--> &f%s";

    public static LogEntryBuilder builder() {
        return new LogEntryBuilder();
    }

    private long timestamp;
    private UUID actor;
    private String actorName;
    private char type;
    private UUID acted;
    private String actedName;
    private String action;

    public LogEntry(long timestamp, UUID actor, String actorName, char type, UUID acted, String actedName, String action) {
        if (actor == null) {
            throw new NullPointerException("actor");
        }
        if (actorName == null) {
            throw new NullPointerException("actorName");
        }
        if (actedName == null) {
            throw new NullPointerException("actedName");
        }
        if (action == null) {
            throw new NullPointerException("action");
        }

        this.timestamp = timestamp;
        this.actor = actor;
        this.actorName = actorName;
        this.type = type;
        this.acted = acted;
        this.actedName = actedName;
        this.action = action;
    }

    protected LogEntry() {
        this.timestamp = 0L;
        this.actor = null;
        this.actorName = null;
        this.type = Character.MIN_VALUE;
        this.acted = null;
        this.actedName = null;
        this.action = null;
    }

    @Override
    public int compareTo(LogEntry o) {
        return equals(o) ? 0 : (Long.compare(timestamp, o.getTimestamp()) == 0 ? 1 : Long.compare(timestamp, o.getTimestamp()));
    }

    public boolean matchesSearch(String query) {
        query = query.toLowerCase();
        return actorName.toLowerCase().contains(query) || actedName.toLowerCase().contains(query)
                || action.toLowerCase().contains(query);
    }

    public String getFormatted() {
        return String.format(FORMAT,
                actorName,
                Character.toString(type),
                actedName,
                action
        );
    }

    public long getTimestamp() {
        return timestamp;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public UUID getActor() {
        return actor;
    }

    void setActor(UUID actor) {
        this.actor = actor;
    }

    public String getActorName() {
        return actorName;
    }

    void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public char getType() {
        return type;
    }

    void setType(char type) {
        this.type = type;
    }

    public UUID getActed() {
        return acted;
    }

    void setActed(UUID acted) {
        this.acted = acted;
    }

    public String getActedName() {
        return actedName;
    }

    void setActedName(String actedName) {
        this.actedName = actedName;
    }

    public String getAction() {
        return action;
    }

    void setAction(String action) {
        this.action = action;
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
                "action=" + this.getAction() +
                ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LogEntry)) return false;
        final LogEntry other = (LogEntry) o;
        return this.getTimestamp() == other.getTimestamp() &&
                (this.getActor() == null ? other.getActor() == null : this.getActor().equals(other.getActor())) &&
                (this.getActorName() == null ? other.getActorName() == null : this.getActorName().equals(other.getActorName())) &&
                this.getType() == other.getType() &&
                (this.getActed() == null ? other.getActed() == null : this.getActed().equals(other.getActed())) &&
                (this.getActedName() == null ? other.getActedName() == null : this.getActedName().equals(other.getActedName())) &&
                (this.getAction() == null ? other.getAction() == null : this.getAction().equals(other.getAction()));
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (int) (this.getTimestamp() >>> 32 ^ this.getTimestamp());
        result = result * PRIME + (this.getActor() == null ? 43 : this.getActor().hashCode());
        result = result * PRIME + (this.getActorName() == null ? 43 : this.getActorName().hashCode());
        result = result * PRIME + this.getType();
        result = result * PRIME + (this.getActed() == null ? 43 : this.getActed().hashCode());
        result = result * PRIME + (this.getActedName() == null ? 43 : this.getActedName().hashCode());
        result = result * PRIME + (this.getAction() == null ? 43 : this.getAction().hashCode());
        return result;
    }

    public static class LogEntryBuilder extends AbstractLogEntryBuilder<LogEntry, LogEntry.LogEntryBuilder> {

        @Override
        protected LogEntry createObj() {
            return new LogEntry();
        }

        @Override
        protected LogEntryBuilder getThis() {
            return this;
        }

    }

    public static abstract class AbstractLogEntryBuilder<T extends LogEntry, B extends AbstractLogEntryBuilder<T, B>> {
        private T obj;
        private B thisObj;

        public AbstractLogEntryBuilder() {
            obj = createObj();
            thisObj = getThis();
        }

        protected abstract T createObj();

        protected abstract B getThis();

        public long getTimestamp() {
            return obj.getTimestamp();
        }

        public UUID getActor() {
            return obj.getActor();
        }

        public String getActorName() {
            return obj.getActorName();
        }

        public char getType() {
            return obj.getType();
        }

        public UUID getActed() {
            return obj.getActed();
        }

        public String getActedName() {
            return obj.getActedName();
        }

        public String getAction() {
            return obj.getAction();
        }

        public B timestamp(long timestamp) {
            obj.setTimestamp(timestamp);
            return thisObj;
        }

        public B actor(UUID actor) {
            obj.setActor(actor);
            return thisObj;
        }

        public B actorName(String actorName) {
            obj.setActorName(actorName);
            return thisObj;
        }

        public B type(char type) {
            obj.setType(type);
            return thisObj;
        }

        public B acted(UUID acted) {
            obj.setActed(acted);
            return thisObj;
        }

        public B actedName(String actedName) {
            obj.setActedName(actedName);
            return thisObj;
        }

        public B action(String action) {
            obj.setAction(action);
            return thisObj;
        }

        public T build() {
            if (getActor() == null) {
                throw new NullPointerException("actor");
            }
            if (getActorName() == null) {
                throw new NullPointerException("actorName");
            }
            if (getActedName() == null) {
                throw new NullPointerException("actedName");
            }
            if (getAction() == null) {
                throw new NullPointerException("action");
            }

            return obj;
        }

        @Override
        public String toString() {
            return "LogEntry.LogEntryBuilder(" +
                    "timestamp=" + this.getTimestamp() + ", " +
                    "actor=" + this.getActor() + ", " +
                    "actorName=" + this.getActorName() + ", " +
                    "type=" + this.getType() + ", " +
                    "acted=" + this.getActed() + ", " +
                    "actedName=" + this.getActedName() + ", " +
                    "action=" + this.getAction() +
                    ")";
        }
    }

}
