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

public class LogEntry implements Comparable<LogEntry> {
    public static LogEntryBuilder builder() {
        return new LogEntryBuilder();
    }

    private static final String FORMAT = "&8(&e%s&8) [&a%s&8] (&b%s&8) &7--> &f%s";

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
        return Long.compare(timestamp, o.getTimestamp());
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

    public UUID getActor() {
        return actor;
    }

    public String getActorName() {
        return actorName;
    }

    public char getType() {
        return type;
    }

    public UUID getActed() {
        return acted;
    }

    public String getActedName() {
        return actedName;
    }

    public String getAction() {
        return action;
    }

    void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    void setActor(UUID actor) {
        this.actor = actor;
    }

    void setActorName(String actorName) {
        this.actorName = actorName;
    }

    void setType(char type) {
        this.type = type;
    }

    void setActed(UUID acted) {
        this.acted = acted;
    }

    void setActedName(String actedName) {
        this.actedName = actedName;
    }

    void setAction(String action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "LogEntry(timestamp=" + this.getTimestamp() + ", actor=" + this.getActor() + ", actorName=" +
                this.getActorName() + ", type=" + this.getType() + ", acted=" + this.getActed() + ", actedName=" +
                this.getActedName() + ", action=" + this.getAction() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof LogEntry)) return false;
        final LogEntry other = (LogEntry) o;
        if (this.getTimestamp() != other.getTimestamp()) return false;
        final Object this$actor = this.getActor();
        final Object other$actor = other.getActor();
        if (this$actor == null ? other$actor != null : !this$actor.equals(other$actor)) return false;
        final Object this$actorName = this.getActorName();
        final Object other$actorName = other.getActorName();
        if (this$actorName == null ? other$actorName != null : !this$actorName.equals(other$actorName)) return false;
        if (this.getType() != other.getType()) return false;
        final Object this$acted = this.getActed();
        final Object other$acted = other.getActed();
        if (this$acted == null ? other$acted != null : !this$acted.equals(other$acted)) return false;
        final Object this$actedName = this.getActedName();
        final Object other$actedName = other.getActedName();
        if (this$actedName == null ? other$actedName != null : !this$actedName.equals(other$actedName)) return false;
        final Object this$action = this.getAction();
        final Object other$action = other.getAction();
        return this$action == null ? other$action == null : this$action.equals(other$action);
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final long $timestamp = this.getTimestamp();
        result = result * PRIME + (int) ($timestamp >>> 32 ^ $timestamp);
        final Object $actor = this.getActor();
        result = result * PRIME + ($actor == null ? 43 : $actor.hashCode());
        final Object $actorName = this.getActorName();
        result = result * PRIME + ($actorName == null ? 43 : $actorName.hashCode());
        result = result * PRIME + this.getType();
        final Object $acted = this.getActed();
        result = result * PRIME + ($acted == null ? 43 : $acted.hashCode());
        final Object $actedName = this.getActedName();
        result = result * PRIME + ($actedName == null ? 43 : $actedName.hashCode());
        final Object $action = this.getAction();
        result = result * PRIME + ($action == null ? 43 : $action.hashCode());
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
            return "LogEntry.LogEntryBuilder(timestamp=" + getTimestamp() + ", actor=" + getActor() + ", actorName=" +
                    getActorName() + ", type=" + getType() + ", acted=" + getActed() + ", actedName=" + getActedName() +
                    ", action=" + getAction() + ")";
        }
    }

}
