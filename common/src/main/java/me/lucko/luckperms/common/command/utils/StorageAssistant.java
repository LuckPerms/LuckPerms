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

package me.lucko.luckperms.common.command.utils;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;

import java.util.Optional;

/**
 * Utility methods for saving users, groups and tracks.
 */
public final class StorageAssistant {

    public static void save(User user, Sender sender, LuckPermsPlugin plugin) {
        try {
            plugin.getStorage().noBuffer().saveUser(user).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.USER_SAVE_ERROR.send(sender, user.getFriendlyName());
            return;
        }

        if (sender.isImport()) {
            user.getRefreshBuffer().request();
        } else {
            user.getRefreshBuffer().requestDirectly();
        }

        if (!sender.isImport()) {
            Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
            if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
                messagingService.get().pushUserUpdate(user);
            }
        }
    }

    public static void save(Group group, Sender sender, LuckPermsPlugin plugin) {
        try {
            plugin.getStorage().noBuffer().saveGroup(group).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.GROUP_SAVE_ERROR.send(sender, group.getFriendlyName());
            return;
        }

        if (sender.isImport()) {
            plugin.getUpdateTaskBuffer().request();
        } else {
            plugin.getUpdateTaskBuffer().requestDirectly();
        }

        if (!sender.isImport()) {
            Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
            if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
                messagingService.get().getUpdateBuffer().request();
            }
        }
    }

    public static void save(Track track, Sender sender, LuckPermsPlugin plugin) {
        try {
            plugin.getStorage().noBuffer().saveTrack(track).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.TRACK_SAVE_ERROR.send(sender, track.getName());
            return;
        }

        if (sender.isImport()) {
            plugin.getUpdateTaskBuffer().request();
        } else {
            plugin.getUpdateTaskBuffer().requestDirectly();
        }

        if (!sender.isImport()) {
            Optional<InternalMessagingService> messagingService = plugin.getMessagingService();
            if (messagingService.isPresent() && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
                messagingService.get().getUpdateBuffer().request();
            }
        }
    }

    public static void save(PermissionHolder holder, Sender sender, LuckPermsPlugin plugin) {
        if (holder.getType().isUser()) {
            User user = ((User) holder);
            save(user, sender, plugin);
        } else if (holder.getType().isGroup()) {
            Group group = ((Group) holder);
            save(group, sender, plugin);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private StorageAssistant() {}
}
