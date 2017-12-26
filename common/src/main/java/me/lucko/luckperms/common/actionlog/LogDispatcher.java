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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.event.log.LogBroadcastEvent;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.impl.log.LogNotify;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Optional;

@RequiredArgsConstructor
public class LogDispatcher {
    private final LuckPermsPlugin plugin;

    public void dispatch(ExtendedLogEntry entry, Sender sender) {
        // set the event to cancelled if the sender is import
        if (!plugin.getApiProvider().getEventFactory().handleLogPublish(sender.isImport(), entry)) {
            plugin.getStorage().logAction(entry);
        }

        // don't dispatch log entries sent by an import process
        if (sender.isImport()) {
            return;
        }

        Optional<ExtendedMessagingService> messagingService = plugin.getMessagingService();
        if (!sender.isImport() && messagingService.isPresent()) {
            messagingService.get().pushLog(entry);
        }

        if (!plugin.getApiProvider().getEventFactory().handleLogBroadcast(!plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY), entry, LogBroadcastEvent.Origin.LOCAL)) {
            plugin.getOnlineSenders()
                    .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                    .filter(s -> !LogNotify.isIgnoring(plugin, s.getUuid()))
                    .filter(s -> !s.getUuid().equals(sender.getUuid()))
                    .forEach(s -> Message.LOG.send(s,
                            entry.getActorFriendlyString(),
                            Character.toString(entry.getType().getCode()),
                            entry.getActedFriendlyString(),
                            entry.getAction()
                    ));
        }
    }

    public void dispatchFromRemote(ExtendedLogEntry entry) {
        if (!plugin.getConfiguration().get(ConfigKeys.BROADCAST_RECEIVED_LOG_ENTRIES)) {
            return;
        }

        if (!plugin.getApiProvider().getEventFactory().handleLogBroadcast(!plugin.getConfiguration().get(ConfigKeys.LOG_NOTIFY), entry, LogBroadcastEvent.Origin.REMOTE)) {
            plugin.getOnlineSenders()
                    .filter(CommandPermission.LOG_NOTIFY::isAuthorized)
                    .filter(s -> !LogNotify.isIgnoring(plugin, s.getUuid()))
                    .forEach(s -> Message.LOG.send(s,
                            entry.getActorFriendlyString(),
                            Character.toString(entry.getType().getCode()),
                            entry.getActedFriendlyString(),
                            entry.getAction()
                    ));
        }
    }
}
