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

package me.lucko.luckperms.common.data;

import me.lucko.luckperms.api.event.events.LogNotifyEvent;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;

import java.util.List;

public class LogEntry extends me.lucko.luckperms.api.LogEntry {
    public static LogEntryBuilder build() {
        return new LogEntryBuilder();
    }

    private LogEntry() {
        super();
    }

    public void submit(LuckPermsPlugin plugin) {
        submit(plugin, null);
    }

    public void submit(LuckPermsPlugin plugin, Sender sender) {
        plugin.getStorage().logAction(this);

        LogNotifyEvent event = new LogNotifyEvent(this);
        event.setCancelled(!plugin.getConfiguration().isLogNotify());
        plugin.getApiProvider().fireEvent(event);
        if (event.isCancelled()) return;

        final String msg = super.getFormatted();

        List<Sender> senders = plugin.getSenders();
        senders.add(plugin.getConsoleSender());

        if (sender == null) {
            senders.stream()
                    .filter(Permission.LOG_NOTIFY::isAuthorized)
                    .filter(s -> !plugin.getIgnoringLogs().contains(s.getUuid()))
                    .forEach(s -> Message.LOG.send(s, msg));
        } else {
            senders.stream()
                    .filter(Permission.LOG_NOTIFY::isAuthorized)
                    .filter(s -> !plugin.getIgnoringLogs().contains(s.getUuid()))
                    .filter(s -> !s.getUuid().equals(sender.getUuid()))
                    .forEach(s -> Message.LOG.send(s, msg));
        }
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

        public LogEntryBuilder actor(Sender actor) {
            super.actorName(actor.getName());
            super.actor(actor.getUuid());
            return this;
        }

        public LogEntryBuilder type(String type) {
            super.type(type.toCharArray()[0]);
            return this;
        }

        public LogEntryBuilder type(Object object) {
            if (object instanceof User) {
                super.type('U');
            } else if (object instanceof Group) {
                super.type('G');
            } else if (object instanceof Track) {
                super.type('T');
            } else {
                throw new IllegalArgumentException();
            }
            return this;
        }

        public LogEntryBuilder acted(PermissionHolder acted) {
            if (acted instanceof User) {
                super.actedName(((User) acted).getName());
                super.acted(((User) acted).getUuid());
                super.type('U');
            } else if (acted instanceof Group) {
                super.actedName(((Group) acted).getName());
                super.type('G');
            }
            return this;
        }

        public LogEntryBuilder acted(Track track) {
            super.actedName(track.getName());
            super.type('T');
            return this;
        }

        @Override
        public LogEntry build() {
            if (getTimestamp() == 0L) {
                super.timestamp(System.currentTimeMillis() / 1000L);
            }

            return super.build();
        }
    }
}
