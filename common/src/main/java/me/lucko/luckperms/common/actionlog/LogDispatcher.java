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
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.log.LogNotifyEvent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
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

    // broadcast the entry to online players
    private void broadcast(LoggedAction entry, LogBroadcastEvent.Origin broadcastOrigin, LogNotifyEvent.Origin origin, Sender sender) {
        if (!shouldBroadcast(entry, broadcastOrigin)) {
            return;
        }

        this.plugin.getOnlineSenders()
                .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                .filter(s -> {
                    boolean shouldCancel = LogNotify.isIgnoring(this.plugin, s.getUniqueId()) || sender != null && s.getUniqueId().equals(sender.getUniqueId());
                    return !this.plugin.getEventDispatcher().dispatchLogNotify(shouldCancel, entry, origin, s);
                })
                .forEach(s -> Message.LOG.send(s, entry));
    }

    // log the entry to storage
    public CompletableFuture<Void> logToStorage(LoggedAction entry) {
        if (!this.plugin.getEventDispatcher().dispatchLogPublish(false, entry)) {
            return this.plugin.getStorage().logAction(entry);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    // log the entry to messaging
    public CompletableFuture<Void> logToMessaging(LoggedAction entry) {
        InternalMessagingService messagingService = this.plugin.getMessagingService().orElse(null);
        if (messagingService != null) {
            return messagingService.pushLog(entry);
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    // log the entry to storage and messaging, and broadcast it to online players
    private CompletableFuture<Void> dispatch(LoggedAction entry, Sender sender, LogBroadcastEvent.Origin broadcastOrigin, LogNotifyEvent.Origin origin) {
        CompletableFuture<Void> storageFuture = logToStorage(entry);
        CompletableFuture<Void> messagingFuture = logToMessaging(entry);
        broadcast(entry, broadcastOrigin, origin, sender);
        return CompletableFuture.allOf(storageFuture, messagingFuture);
    }

    public CompletableFuture<Void> dispatch(LoggedAction entry, Sender sender) {
        return dispatch(entry, sender, LogBroadcastEvent.Origin.LOCAL, LogNotifyEvent.Origin.LOCAL);
    }

    public CompletableFuture<Void> dispatchFromApi(LoggedAction entry) {
        return dispatch(entry, null, LogBroadcastEvent.Origin.LOCAL_API, LogNotifyEvent.Origin.LOCAL_API);
    }

    public void broadcastFromApi(LoggedAction entry) {
        broadcast(entry, LogBroadcastEvent.Origin.LOCAL_API, LogNotifyEvent.Origin.LOCAL_API, null);
    }

    public void broadcastFromRemote(LoggedAction entry) {
        broadcast(entry, LogBroadcastEvent.Origin.REMOTE, LogNotifyEvent.Origin.REMOTE, null);
    }
}
