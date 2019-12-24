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
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import net.luckperms.api.event.log.LogBroadcastEvent;
import net.luckperms.api.event.log.LogNotifyEvent;

import java.util.Optional;

public class LogDispatcher {
    private final LuckPermsPlugin plugin;

    public LogDispatcher(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    private void broadcast(LoggedAction entry, LogNotifyEvent.Origin origin, Sender sender) {
        this.plugin.getOnlineSenders()
                .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                .filter(s -> {
                    boolean shouldCancel = LogNotify.isIgnoring(this.plugin, s.getUniqueId()) || (sender != null && s.getUniqueId().equals(sender.getUniqueId()));
                    return !this.plugin.getEventDispatcher().dispatchLogNotify(shouldCancel, entry, origin, s);
                })
                .forEach(s -> Message.LOG.send(s,
                        entry.getSourceFriendlyString(),
                        Character.toString(LoggedAction.getTypeCharacter(entry.getTarget().getType())),
                        entry.getTargetFriendlyString(),
                        entry.getDescription()
                ));
    }

    public void dispatch(LoggedAction entry, Sender sender) {
        // set the event to cancelled if the sender is import
        if (!this.plugin.getEventDispatcher().dispatchLogPublish(sender.isImport(), entry)) {
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
        if (!this.plugin.getEventDispatcher().dispatchLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.LOCAL)) {
            broadcast(entry, LogNotifyEvent.Origin.LOCAL, sender);
        }
    }

    public void dispatchFromApi(LoggedAction entry) {
        if (!this.plugin.getEventDispatcher().dispatchLogPublish(false, entry)) {
            try {
                this.plugin.getStorage().logAction(entry).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        broadcastFromApi(entry);
    }

    public void broadcastFromApi(LoggedAction entry) {
        this.plugin.getMessagingService().ifPresent(extendedMessagingService -> extendedMessagingService.pushLog(entry));

        boolean shouldCancel = !this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY);
        if (!this.plugin.getEventDispatcher().dispatchLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.LOCAL_API)) {
            broadcast(entry, LogNotifyEvent.Origin.LOCAL_API, null);
        }
    }

    public void dispatchFromRemote(LoggedAction entry) {
        boolean shouldCancel = !this.plugin.getConfiguration().get(ConfigKeys.BROADCAST_RECEIVED_LOG_ENTRIES) || !this.plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY);
        if (!this.plugin.getEventDispatcher().dispatchLogBroadcast(shouldCancel, entry, LogBroadcastEvent.Origin.REMOTE)) {
            broadcast(entry, LogNotifyEvent.Origin.REMOTE, null);
        }
    }
}
