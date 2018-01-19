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

import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.impl.log.LogNotify;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;

public class LogDispatcher {
    private final LuckPermsPlugin plugin;

    public LogDispatcher(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    private void broadcast(ExtendedLogEntry entry, LogBroadcastEvent.Origin origin, Sender sender) {
        this.plugin.getOnlineSenders()
                .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                .filter(s -> {
                    boolean shouldCancel = LogNotify.isIgnoring(this.plugin, s.getUuid()) || (sender != null && s.getUuid().equals(sender.getUuid()));
                    return !this.plugin.getEventFactory().handleLogNotify(shouldCancel, entry, origin, s);
                })
                .forEach(s -> Message.LOG.send(s,
                        entry.getActorFriendlyString(),
                        Character.toString(entry.getType().getCode()),
                        entry.getActedFriendlyString(),
                        entry.getAction()
                ));
    }

    public void dispatch(ExtendedLogEntry entry, Sender sender) {
        // set the event to cancelled if the sender is import
        if (!this.plugin.getEventFactory().handleLogPublish(sender.isImport(), entry)) {
            this.plugin.getStorage().logAction(entry);
        }

        // don't dispatch log entries sent by an import process
        if (sender.isImport()) {
            return;
        }

        Optional<InternalMessagingService> messagingService = this.plugin.getMessagingService();
        if (!sender.isImport() && messagingService.isPresent()) {
            messagingService.get().pushLog(entry);
        }

        boolean shouldCancel = !this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY);
        if (!this.plugin.getEventFactory().handleLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.LOCAL)) {
            broadcast(entry, LogBroadcastEvent.Origin.LOCAL, sender);
        }
    }

    public void dispatchFromApi(ExtendedLogEntry entry) {
        if (!this.plugin.getEventFactory().handleLogPublish(false, entry)) {
            try {
                this.plugin.getStorage().logAction(entry).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        broadcastFromApi(entry);
    }

    public void broadcastFromApi(ExtendedLogEntry entry) {
        this.plugin.getMessagingService().ifPresent(extendedMessagingService -> extendedMessagingService.pushLog(entry));

        boolean shouldCancel = !this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY);
        if (!this.plugin.getEventFactory().handleLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.LOCAL_API)) {
            broadcast(entry, LogBroadcastEvent.Origin.LOCAL_API, null);
        }
    }

    public void dispatchFromRemote(ExtendedLogEntry entry) {
        boolean shouldCancel = !this.plugin.getConfiguration().get(ConfigKeys.BROADCAST_RECEIVED_LOG_ENTRIES) || !this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY);
        if (!this.plugin.getEventFactory().handleLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.REMOTE)) {
            broadcast(entry, LogBroadcastEvent.Origin.LOCAL_API, null);
        }
    }
}
