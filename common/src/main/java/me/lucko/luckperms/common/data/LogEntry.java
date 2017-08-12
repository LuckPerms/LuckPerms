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

package me.lucko.luckperms.common.data;

import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;

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
        if (!plugin.getApiProvider().getEventFactory().handleLogPublish(false, this)) {
            plugin.getStorage().logAction(this);
        }

        if (plugin.getApiProvider().getEventFactory().handleLogBroadcast(!plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY), this)) {
            return;
        }

        final String msg = super.getFormatted();

        List<Sender> senders = plugin.getOnlineSenders();
        senders.add(plugin.getConsoleSender());

        if (sender == null) {
            senders.stream()
                    .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                    .filter(s -> !plugin.getIgnoringLogs().contains(s.getUuid()))
                    .forEach(s -> Message.LOG.send(s, msg));
        } else {
            senders.stream()
                    .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
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
                super.actedName(((User) acted).getName().orElse("null"));
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
                super.timestamp(DateUtil.unixSecondsNow());
            }

            return super.build();
        }
    }
}
