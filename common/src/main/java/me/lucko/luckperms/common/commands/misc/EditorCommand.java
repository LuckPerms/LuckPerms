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

package me.lucko.luckperms.common.commands.misc;

import com.google.gson.JsonObject;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.web.WebEditor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EditorCommand extends SingleCommand {
    private static final int MAX_USERS = 500;

    public EditorCommand(LocaleManager locale) {
        super(CommandSpec.EDITOR.localize(locale), "Editor", CommandPermission.EDITOR, Predicates.notInRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Type type = Type.ALL;

        // parse type
        String typeString = ArgumentParser.parseStringOrElse(0, args, null);
        if (typeString != null) {
            try {
                type = Type.valueOf(typeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignored
            }
        }

        // collect holders
        List<PermissionHolder> holders = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();
        if (type.includingGroups) {
            // run a sync task
            plugin.getSyncTaskBuffer().requestDirectly();

            plugin.getGroupManager().getAll().values().stream()
                    .sorted((o1, o2) -> {
                        int i = Integer.compare(o2.getWeight().orElse(0), o1.getWeight().orElse(0));
                        return i != 0 ? i : o1.getName().compareToIgnoreCase(o2.getName());
                    })
                    .forEach(holders::add);
            tracks = new ArrayList<>(plugin.getTrackManager().getAll().values());
        }
        if (type.includingUsers) {
            Set<UUID> users = new LinkedHashSet<>();

            // online players first
            plugin.getUserManager().getAll().values().stream()
                    .sorted((o1, o2) -> o1.getFormattedDisplayName().compareToIgnoreCase(o2.getFormattedDisplayName()))
                    .map(User::getUniqueId)
                    .forEach(users::add);

            // then fill up with other users
            users.addAll(plugin.getStorage().getUniqueUsers().join());

            users.stream().limit(MAX_USERS).forEach(uuid -> {
                User user = plugin.getUserManager().getIfLoaded(uuid);
                if (user != null) {
                    holders.add(user);
                } else {
                    user = plugin.getStorage().loadUser(uuid, null).join();
                    if (user != null) {
                        holders.add(user);
                    }
                    plugin.getUserManager().getHouseKeeper().cleanup(uuid);
                }
            });
        }

        if (holders.isEmpty()) {
            Message.EDITOR_NO_MATCH.send(sender);
            return CommandResult.STATE_ERROR;
        }

        // remove holders which the sender doesn't have perms to view
        holders.removeIf(holder -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder));
        tracks.removeIf(track -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), track));

        // they don't have perms to view any of them
        if (holders.isEmpty() && tracks.isEmpty()) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Message.EDITOR_START.send(sender);

        JsonObject payload = WebEditor.formPayload(holders, tracks, sender, label, plugin);
        return WebEditor.post(payload, sender, plugin);
    }

    private enum Type {
        ALL(true, true),
        USERS(true, false),
        GROUPS(false, true);

        private final boolean includingUsers;
        private final boolean includingGroups;

        Type(boolean includingUsers, boolean includingGroups) {
            this.includingUsers = includingUsers;
            this.includingGroups = includingGroups;
        }
    }
}
