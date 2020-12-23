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

import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.commands.log.LogNotify;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.log.LogNotifyEvent;

import java.util.Collection;
import java.util.regex.Pattern;

public class LogDispatcher {
    private final LuckPermsPlugin plugin;

    public LogDispatcher(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean shouldBroadcast(LoggedAction entry, LogBroadcastEvent.Origin origin) {
        boolean cancelled = false;

        if (!this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY)) {
            cancelled = true;
        } else if (origin == LogBroadcastEvent.Origin.REMOTE && !this.plugin.getConfiguration().get(ConfigKeys.BROADCAST_RECEIVED_LOG_ENTRIES)) {
            cancelled = true;
        } else {
            Collection<Pattern> filters = this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY_FILTERED_DESCRIPTIONS);
            for (Pattern filter : filters) {
                if (filter.matcher(entry.getDescription()).matches()) {
                    cancelled = true;
                    break;
                }
            }
        }

        return !this.plugin.getEventDispatcher().dispatchLogBroadcast(cancelled, entry, origin);
    }

    private void broadcast(LoggedAction entry, LogNotifyEvent.Origin origin, Sender sender) {
        this.plugin.getOnlineSenders()
                .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                .filter(s -> {
                    boolean shouldCancel = LogNotify.isIgnoring(this.plugin, s.getUniqueId()) || (sender != null && s.getUniqueId().equals(sender.getUniqueId()));
                    return !this.plugin.getEventDispatcher().dispatchLogNotify(shouldCancel, entry, origin, s);
                })
                .forEach(s -> Message.LOG.send(s, entry));
    }

    public void dispatch(LoggedAction entry, Sender sender) {
        if (!this.plugin.getEventDispatcher().dispatchLogPublish(false, entry)) {
            this.plugin.getStorage().logAction(entry);
        }

        this.plugin.getMessagingService().ifPresent(service -> service.pushLog(entry));

        if (shouldBroadcast(entry, LogBroadcastEvent.Origin.LOCAL)) {
            broadcast(entry, LogNotifyEvent.Origin.LOCAL, sender);
        }
    }

    public void broadcastFromApi(LoggedAction entry) {
        this.plugin.getMessagingService().ifPresent(extendedMessagingService -> extendedMessagingService.pushLog(entry));

        if (shouldBroadcast(entry, LogBroadcastEvent.Origin.LOCAL_API)) {
            broadcast(entry, LogNotifyEvent.Origin.LOCAL_API, null);
        }
    }

    public void dispatchFromApi(LoggedAction entry) {
        if (!this.plugin.getEventDispatcher().dispatchLogPublish(false, entry)) {
            try {
                this.plugin.getStorage().logAction(entry).get();
            } catch (Exception e) {
                this.plugin.getLogger().warn("Error whilst storing action", e);
            }
        }

        broadcastFromApi(entry);
    }

    public void dispatchFromRemote(LoggedAction entry) {
        if (shouldBroadcast(entry, LogBroadcastEvent.Origin.REMOTE)) {
            broadcast(entry, LogNotifyEvent.Origin.REMOTE, null);
        }
    }
}
