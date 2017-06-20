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

package me.lucko.luckperms.common.commands.abstraction;

import lombok.Getter;

import com.google.common.base.Splitter;

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.locale.LocalizedSpec;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.messaging.InternalMessagingService;
import me.lucko.luckperms.common.messaging.NoopMessagingService;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.treeview.PermissionVault;
import me.lucko.luckperms.common.treeview.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Abstract SubCommand class
 */
@Getter
public abstract class SubCommand<T> extends Command<T, Void> {

    public SubCommand(LocalizedSpec spec, String name, Permission permission, Predicate<Integer> argumentCheck) {
        super(spec, name, permission, argumentCheck);
    }

    /**
     * Send the command usage to a sender
     *
     * @param sender the sender to send the usage to
     */
    @Override
    public void sendUsage(Sender sender, String label) {
        StringBuilder sb = new StringBuilder();
        if (getArgs().isPresent()) {
            sb.append("&3 - &7");
            for (Arg arg : getArgs().get()) {
                sb.append(arg.asPrettyString()).append(" ");
            }
        }

        Util.sendPluginMessage(sender, "&3> &a" + getName().toLowerCase() + sb.toString());
    }

    @Override
    public void sendDetailedUsage(Sender sender, String label) {
        Util.sendPluginMessage(sender, "&3&lCommand Usage &3- &b" + getName());
        Util.sendPluginMessage(sender, "&b> &7" + getDescription());
        if (getArgs().isPresent()) {
            Util.sendPluginMessage(sender, "&3Arguments:");
            for (Arg arg : getArgs().get()) {
                Util.sendPluginMessage(sender, "&b- " + arg.asPrettyString() + "&3 -> &7" + arg.getDescription());
            }
        }
    }

    /* Utility methods used by #tabComplete and #execute implementations in sub classes */

    public static List<String> getGroupTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getGroupManager().getAll().keySet()), args);
    }

    public static List<String> getTrackTabComplete(List<String> args, LuckPermsPlugin plugin) {
        return getTabComplete(new ArrayList<>(plugin.getTrackManager().getAll().keySet()), args);
    }

    public static List<String> getBoolTabComplete(List<String> args) {
        if (args.size() == 2) {
            return Arrays.asList("true", "false");
        } else {
            return Collections.emptyList();
        }
    }

    public static List<String> getPermissionTabComplete(List<String> args, PermissionVault cache) {
        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equals("")) {
                return cache.getRootNode().getChildren()
                        .map(Map::keySet)
                        .map(s -> s.stream().collect(Collectors.toList()))
                        .orElse(Collections.emptyList());
            }

            String start = args.get(0).toLowerCase();
            List<String> parts = new ArrayList<>(Splitter.on('.').splitToList(start));
            TreeNode root = cache.getRootNode();

            if (parts.size() <= 1) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                return root.getChildren().get().keySet().stream().filter(s -> s.startsWith(start)).collect(Collectors.toList());
            }

            String incomplete = parts.remove(parts.size() - 1);

            for (String s : parts) {
                if (!root.getChildren().isPresent()) {
                    return Collections.emptyList();
                }

                TreeNode n = root.getChildren().get().get(s);
                if (n == null) {
                    return Collections.emptyList();
                }

                root = n;
            }

            if (!root.getChildren().isPresent()) {
                return Collections.emptyList();
            }

            return root.getChildren().get().keySet().stream()
                    .filter(s -> s.startsWith(incomplete))
                    .map(s -> parts.stream().collect(Collectors.joining(".")) + "." + s)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public static List<String> getTabComplete(List<String> options, List<String> args) {
        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return options;
            }

            return options.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public static void save(User user, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getStorage().force().saveUser(user).join();

        if (sender.isImport()) {
            user.getRefreshBuffer().request();
        } else {
            user.getRefreshBuffer().requestDirectly();
        }

        InternalMessagingService messagingService = plugin.getMessagingService();
        if (!sender.isImport() && !(messagingService instanceof NoopMessagingService) && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
            messagingService.getUpdateBuffer().request();
        }

        if (success) {
            Message.USER_SAVE_SUCCESS.send(sender);
        } else {
            Message.USER_SAVE_ERROR.send(sender);
        }
    }

    public static void save(Group group, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getStorage().force().saveGroup(group).join();

        if (sender.isImport()) {
            plugin.getUpdateTaskBuffer().request();
        } else {
            plugin.getUpdateTaskBuffer().requestDirectly();
        }

        InternalMessagingService messagingService = plugin.getMessagingService();
        if (!sender.isImport() && !(messagingService instanceof NoopMessagingService) && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
            messagingService.getUpdateBuffer().request();
        }

        if (success) {
            Message.GROUP_SAVE_SUCCESS.send(sender);
        } else {
            Message.GROUP_SAVE_ERROR.send(sender);
        }
    }

    public static void save(Track track, Sender sender, LuckPermsPlugin plugin) {
        boolean success = plugin.getStorage().force().saveTrack(track).join();

        if (sender.isImport()) {
            plugin.getUpdateTaskBuffer().request();
        } else {
            plugin.getUpdateTaskBuffer().requestDirectly();
        }

        InternalMessagingService messagingService = plugin.getMessagingService();
        if (!sender.isImport() && !(messagingService instanceof NoopMessagingService) && plugin.getConfiguration().get(ConfigKeys.AUTO_PUSH_UPDATES)) {
            messagingService.getUpdateBuffer().request();
        }

        if (success) {
            Message.TRACK_SAVE_SUCCESS.send(sender);
        } else {
            Message.TRACK_SAVE_ERROR.send(sender);
        }
    }
}
