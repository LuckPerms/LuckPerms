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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.Uuids;
import me.lucko.luckperms.common.web.UnsuccessfulRequestException;
import me.lucko.luckperms.common.web.WebEditor;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ApplyEditsCommand extends SingleCommand {
    public ApplyEditsCommand(LocaleManager locale) {
        super(CommandSpec.APPLY_EDITS.localize(locale), "ApplyEdits", CommandPermission.APPLY_EDITS, Predicates.notInRange(1, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        String code = args.get(0);

        if (code.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return CommandResult.INVALID_ARGS;
        }

        JsonObject data;
        try {
            data = WebEditor.readDataFromBytebin(plugin.getBytebin(), code);
        } catch (UnsuccessfulRequestException e) {
            Message.EDITOR_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
            return CommandResult.STATE_ERROR;
        } catch (IOException e) {
            new RuntimeException("Error uploading data to bytebin", e).printStackTrace();
            Message.EDITOR_HTTP_UNKNOWN_FAILURE.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (data == null) {
            Message.APPLY_EDITS_UNABLE_TO_READ.send(sender, code);
            return CommandResult.FAILURE;
        }

        boolean work = false;
        if (data.has("changes")) {
            JsonArray changes = data.get("changes").getAsJsonArray();
            for (JsonElement change : changes) {
                if (readChanges(change.getAsJsonObject(), sender, plugin)) {
                    work = true;
                }
            }
        }
        if (data.has("groupDeletions")) {
            JsonArray groupDeletions = data.get("groupDeletions").getAsJsonArray();
            for (JsonElement groupDeletion : groupDeletions) {
                if (readGroupDeletion(groupDeletion, sender, plugin)) {
                    work = true;
                }
            }
        }
        if (data.has("trackDeletions")) {
            JsonArray trackDeletions = data.get("trackDeletions").getAsJsonArray();
            for (JsonElement trackDeletion : trackDeletions) {
                if (readTrackDeletion(trackDeletion, sender, plugin)) {
                    work = true;
                }
            }
        }

        if (!work) {
            Message.APPLY_EDITS_TARGET_NO_CHANGES_PRESENT.send(sender);
        }

        return CommandResult.SUCCESS;
    }

    private boolean readChanges(JsonObject data, Sender sender, LuckPermsPlugin plugin) {
        String type = data.get("type").getAsString();

        if (type.equals("user") || type.equals("group")) {
            return readHolderChanges(data, sender, plugin);
        } else if (type.equals("track")) {
            return readTrackChanges(data, sender, plugin);
        } else {
            Message.APPLY_EDITS_UNKNOWN_TYPE.send(sender, type);
            return false;
        }
    }

    private boolean readHolderChanges(JsonObject data, Sender sender, LuckPermsPlugin plugin) {
        String type = data.get("type").getAsString();
        String id = data.get("id").getAsString();

        PermissionHolder holder;
        if (type.equals("user")) {
            // user
            UUID uuid = Uuids.parse(id);
            if (uuid == null) {
                Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(sender, id);
                return false;
            }
            holder = plugin.getStorage().loadUser(uuid, null).join();
            if (holder == null) {
                Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.send(sender, uuid.toString());
                return false;
            }
        } else {
            // group
            holder = plugin.getStorage().loadGroup(id).join().orElse(null);
            if (holder == null) {
                holder = plugin.getStorage().createAndLoadGroup(id, CreationCause.WEB_EDITOR).join();
            }
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), holder) || ArgumentPermissions.checkGroup(plugin, sender, holder, ImmutableContextSetImpl.EMPTY)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return false;
        }

        Set<Node> before = holder.normalData().asSet();
        Set<Node> after = new HashSet<>(NodeJsonSerializer.deserializeNodes(data.getAsJsonArray("nodes")));

        Set<Node> diffAdded = getAdded(before, after);
        Set<Node> diffRemoved = getRemoved(before, after);

        int additions = diffAdded.size();
        int deletions = diffRemoved.size();

        if (additions == 0 && deletions == 0) {
            return false;
        }

        holder.setNodes(DataType.NORMAL, after);

        for (Node n : diffAdded) {
            LoggedAction.build().source(sender).target(holder)
                    .description("webeditor", "add", n.getKey(), n.getValue(), n.getContexts())
                    .build().submit(plugin, sender);
        }
        for (Node n : diffRemoved) {
            LoggedAction.build().source(sender).target(holder)
                    .description("webeditor", "remove", n.getKey(), n.getValue(), n.getContexts())
                    .build().submit(plugin, sender);
        }

        String additionsSummary = "addition" + (additions == 1 ? "" : "s");
        String deletionsSummary = "deletion" + (deletions == 1 ? "" : "s");

        Message.APPLY_EDITS_SUCCESS.send(sender, type, holder.getFormattedDisplayName());
        Message.APPLY_EDITS_SUCCESS_SUMMARY.send(sender, additions, additionsSummary, deletions, deletionsSummary);
        for (Node n : diffAdded) {
            Message.APPLY_EDITS_DIFF_ADDED.send(sender, formatNode(plugin.getLocaleManager(), n));
        }
        for (Node n : diffRemoved) {
            Message.APPLY_EDITS_DIFF_REMOVED.send(sender, formatNode(plugin.getLocaleManager(), n));
        }
        StorageAssistant.save(holder, sender, plugin);
        return true;
    }

    private boolean readTrackChanges(JsonObject data, Sender sender, LuckPermsPlugin plugin) {
        String id = data.get("id").getAsString();

        Track track = plugin.getStorage().loadTrack(id).join().orElse(null);
        if (track == null) {
            track = plugin.getStorage().createAndLoadTrack(id, CreationCause.WEB_EDITOR).join();
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), track)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return false;
        }

        List<String> before = track.getGroups();
        List<String> after = new ArrayList<>();
        data.getAsJsonArray("groups").forEach(e -> after.add(e.getAsString()));

        if (before.equals(after)) {
            return false;
        }

        Set<String> diffAdded = getAdded(before, after);
        Set<String> diffRemoved = getRemoved(before, after);

        int additions = diffAdded.size();
        int deletions = diffRemoved.size();

        track.setGroups(after);

        if (hasBeenReordered(before, after, diffAdded, diffRemoved)) {
            LoggedAction.build().source(sender).target(track)
                    .description("webeditor", "reorder", after)
                    .build().submit(plugin, sender);
        }
        for (String n : diffAdded) {
            LoggedAction.build().source(sender).target(track)
                    .description("webeditor", "add", n)
                    .build().submit(plugin, sender);
        }
        for (String n : diffRemoved) {
            LoggedAction.build().source(sender).target(track)
                    .description("webeditor", "remove", n)
                    .build().submit(plugin, sender);
        }

        String additionsSummary = "addition" + (additions == 1 ? "" : "s");
        String deletionsSummary = "deletion" + (deletions == 1 ? "" : "s");

        Message.APPLY_EDITS_SUCCESS.send(sender, "track", track.getName());
        Message.APPLY_EDITS_SUCCESS_SUMMARY.send(sender, additions, additionsSummary, deletions, deletionsSummary);
        Message.APPLY_EDITS_DIFF_REMOVED.send(sender, before);
        Message.APPLY_EDITS_DIFF_ADDED.send(sender, after);
        StorageAssistant.save(track, sender, plugin);
        return true;
    }

    private boolean readGroupDeletion(JsonElement data, Sender sender, LuckPermsPlugin plugin) {
        String groupName = data.getAsString();

        if (groupName.equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
            Message.DELETE_GROUP_ERROR_DEFAULT.send(sender);
            return true;
        }

        Group group = plugin.getStorage().loadGroup(groupName).join().orElse(null);
        if (group == null) {
            return false;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), group) || ArgumentPermissions.checkGroup(plugin, sender, group, ImmutableContextSetImpl.EMPTY)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return false;
        }

        try {
            plugin.getStorage().deleteGroup(group, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.DELETE_ERROR.send(sender, group.getFormattedDisplayName());
            return true;
        }

        Message.DELETE_SUCCESS.send(sender, group.getFormattedDisplayName());

        LoggedAction.build().source(sender).targetName(groupName).targetType(Action.Target.Type.GROUP)
                .description("webeditor", "delete")
                .build().submit(plugin, sender);

        return true;
    }

    private boolean readTrackDeletion(JsonElement data, Sender sender, LuckPermsPlugin plugin) {
        String trackName = data.getAsString();

        Track track = plugin.getStorage().loadTrack(trackName).join().orElse(null);
        if (track == null) {
            return false;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), track)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return false;
        }

        try {
            plugin.getStorage().deleteTrack(track, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.DELETE_ERROR.send(sender, track.getName());
            return true;
        }

        Message.DELETE_SUCCESS.send(sender, trackName);

        LoggedAction.build().source(sender).targetName(trackName).targetType(Action.Target.Type.TRACK)
                .description("webeditor", "delete")
                .build().submit(plugin, sender);

        return true;
    }

    private static String formatNode(LocaleManager localeManager, Node n) {
        return n.getKey() + " &7(" + (n.getValue() ? "&a" : "&c") + n.getValue() + "&7)" + MessageUtils.getAppendableNodeContextString(localeManager, n) +
                (n.hasExpiry() ? " &7(" + DurationFormatter.CONCISE.format(n.getExpiryDuration()) + ")" : "");
    }

    private static <T> Set<T> getAdded(Collection<T> before, Collection<T> after) {
        Set<T> added = new LinkedHashSet<>(after);
        added.removeAll(before);
        return added;
    }

    private static <T> Set<T> getRemoved(Collection<T> before, Collection<T> after) {
        Set<T> removed = new LinkedHashSet<>(before);
        removed.removeAll(after);
        return removed;
    }

    private static <T> boolean hasBeenReordered(List<T> before, List<T> after, Collection<T> diffAdded, Collection<T> diffRemoved) {
        after = new ArrayList<>(after);
        before = new ArrayList<>(before);

        after.removeAll(diffAdded);
        before.removeAll(diffRemoved);

        return !before.equals(after);
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }
}
