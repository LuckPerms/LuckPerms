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

package me.lucko.luckperms.data;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class LogEntryBuilder {
    private long timestamp = 0L;
    private UUID actor = null;
    private String actorName = null;
    private char type = Character.MIN_VALUE;
    private UUID acted = null;
    private String actedName = null;
    private String action = null;

    public static LogEntryBuilder get() {
        return new LogEntryBuilder();
    }

    public LogEntryBuilder timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LogEntryBuilder actor(UUID actor) {
        this.actor = actor;
        return this;
    }

    public LogEntryBuilder actorName(String actorName) {
        this.actorName = actorName;
        return this;
    }

    public LogEntryBuilder actor(Sender actor) {
        this.actorName = actor.getName();
        this.actor = actor.getUuid();
        return this;
    }

    public LogEntryBuilder type(char type) {
        this.type = type;
        return this;
    }

    public LogEntryBuilder type(String type) {
        this.type = type.toCharArray()[0];
        return this;
    }

    public LogEntryBuilder type(Object object) {
        if (object instanceof User) {
            this.type = 'U';
        } else if (object instanceof Group) {
            this.type = 'G';
        } else if (object instanceof Track) {
            this.type = 'T';
        } else {
            throw new IllegalArgumentException();
        }
        return this;
    }

    public LogEntryBuilder acted(UUID acted) {
        this.acted = acted;
        return this;
    }

    public LogEntryBuilder actedName(String actedName) {
        this.actedName = actedName;
        return this;
    }

    public LogEntryBuilder acted(PermissionHolder acted) {
        if (acted instanceof User) {
            this.actedName = ((User) acted).getName();
            this.acted = ((User) acted).getUuid();
            this.type = 'U';
        } else if (acted instanceof Group) {
            this.actedName = ((Group) acted).getName();
            this.type = 'G';
        }
        return this;
    }

    public LogEntryBuilder acted(Track track) {
        this.actedName = track.getName();
        this.type = 'T';
        return this;
    }

    public LogEntryBuilder action(String action) {
        this.action = action;
        return this;
    }

    private Set<String> getEmpty() {
        Set<String> s = new HashSet<>();
        if (actor == null) {
            s.add("actor");
        }
        if (actorName == null) {
            s.add("actorname");
        }
        if (type == Character.MIN_VALUE) {
            s.add("type");
        }
        if (actedName == null) {
            s.add("actedname");
        }
        if (action == null) {
            s.add("action");
        }
        return s;
    }

    public void submit(LuckPermsPlugin plugin) {
        final LogEntry entry = build();
        plugin.getDatastore().logAction(entry, Callback.empty());

        final String msg = entry.getFormatted();

        plugin.getSenders().stream()
                .filter(Permission.LOG_NOTIFY::isAuthorized)
                .filter(s -> !plugin.getIgnoringLogs().contains(s.getUuid()))
                .forEach(s -> Message.LOG.send(s, msg));
    }

    public LogEntry build() {
        if (timestamp == 0L) {
            timestamp = System.currentTimeMillis() / 1000;
        }

        if (!getEmpty().isEmpty()) {
            throw new IllegalStateException("Missing values: " + getEmpty().toString());
        }

        return new LogEntry(timestamp, actor, actorName, type, acted, actedName, action);
    }
}
