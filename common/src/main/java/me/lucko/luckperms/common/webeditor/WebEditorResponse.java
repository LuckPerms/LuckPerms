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

package me.lucko.luckperms.common.webeditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Difference;
import me.lucko.luckperms.common.util.Uuids;
import me.lucko.luckperms.common.webeditor.store.RemoteSession;
import net.kyori.adventure.text.Component;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Encapsulates a response from the web editor.
 */
public class WebEditorResponse {

    /**
     * The id of the response payload
     */
    private final String id;

    /**
     * The encoded json object this payload is made up of
     */
    private final JsonObject payload;

    public WebEditorResponse(String id, JsonObject payload) {
        this.id = id;
        this.payload = payload;
    }

    /**
     * Applies the response to storage, sending appropriate informational messages to the sender.
     *
     * @param plugin the plugin
     * @param sender the sender who is applying the session
     */
    public void apply(LuckPermsPlugin plugin, Sender sender, WebEditorSession editorSession, String commandLabel, boolean ignoreSessionWarning) {
        String sessionId = this.payload.get("sessionId").getAsString();
        RemoteSession remoteSession = plugin.getWebEditorStore().sessions().getSession(sessionId);

        if (remoteSession == null) {
            // session is unknown
            if (!ignoreSessionWarning) {
                Message.APPLY_EDITS_SESSION_UNKNOWN.send(sender, this.id, commandLabel);
                return;
            }
        } else if (remoteSession.isCompleted()) {
            // session has been completed already
            if (!ignoreSessionWarning) {
                Message.APPLY_EDITS_SESSION_APPLIED_ALREADY.send(sender, this.id, commandLabel);
                return;
            }
        }

        ChangeApplier changeApplier = new ChangeApplier(plugin, sender, editorSession, remoteSession);
        boolean work = false;

        if (this.payload.has("changes")) {
            JsonArray changes = this.payload.get("changes").getAsJsonArray();
            for (JsonElement change : changes) {
                if (changeApplier.applyChange(change.getAsJsonObject())) {
                    work = true;
                }
            }
        }
        if (this.payload.has("userDeletions")) {
            JsonArray userDeletions = this.payload.get("userDeletions").getAsJsonArray();
            for (JsonElement userDeletion : userDeletions) {
                if (changeApplier.applyUserDelete(userDeletion)) {
                    work = true;
                }
            }
        }
        if (this.payload.has("groupDeletions")) {
            JsonArray groupDeletions = this.payload.get("groupDeletions").getAsJsonArray();
            for (JsonElement groupDeletion : groupDeletions) {
                if (changeApplier.applyGroupDelete(groupDeletion)) {
                    work = true;
                }
            }
        }
        if (this.payload.has("trackDeletions")) {
            JsonArray trackDeletions = this.payload.get("trackDeletions").getAsJsonArray();
            for (JsonElement trackDeletion : trackDeletions) {
                if (changeApplier.applyTrackDelete(trackDeletion)) {
                    work = true;
                }
            }
        }

        if (remoteSession != null) {
            remoteSession.complete();
        }

        if (!work) {
            Message.APPLY_EDITS_TARGET_NO_CHANGES_PRESENT.send(sender);
        }
    }

    /**
     * Represents the application of a given editor session on this platform.
     */
    private static class ChangeApplier {
        private final LuckPermsPlugin plugin;
        private final Sender sender;
        private final WebEditorSession session;
        private final RemoteSession remoteSession;

        ChangeApplier(LuckPermsPlugin plugin, Sender sender, WebEditorSession session, RemoteSession remoteSession) {
            this.plugin = plugin;
            this.sender = sender;
            this.session = session;
            this.remoteSession = remoteSession;
        }

        private boolean applyChange(JsonObject changeInfo) {
            String type = changeInfo.get("type").getAsString();

            if (type.equals("user") || type.equals("group")) {
                return applyHolderChange(changeInfo);
            } else if (type.equals("track")) {
                return applyTrackChange(changeInfo);
            } else {
                Message.APPLY_EDITS_UNKNOWN_TYPE.send(this.sender, type);
                return false;
            }
        }

        private boolean applyHolderChange(JsonObject changeInfo) {
            String type = changeInfo.get("type").getAsString();
            String id = changeInfo.get("id").getAsString();

            PermissionHolder holder;
            if (type.equals("user")) {
                // user
                UUID uuid = Uuids.parse(id);
                if (uuid == null) {
                    Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(this.sender, id);
                    return false;
                }
                holder = this.plugin.getStorage().loadUser(uuid, null).join();
                if (holder == null) {
                    Message.APPLY_EDITS_TARGET_USER_UNABLE_TO_LOAD.send(this.sender, uuid.toString());
                    return false;
                }
            } else {
                // group
                holder = this.plugin.getStorage().loadGroup(id).join().orElse(null);
                if (holder == null) {
                    holder = this.plugin.getStorage().createAndLoadGroup(id, CreationCause.WEB_EDITOR).join();
                    if (this.session != null) {
                        this.session.includeCreatedGroup((Group) holder);
                    }
                }
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, holder) || ArgumentPermissions.checkGroup(this.plugin, this.sender, holder, ImmutableContextSetImpl.EMPTY)) {
                Message.COMMAND_NO_PERMISSION.send(this.sender);
                return false;
            }

            Set<Node> nodes = NodeJsonSerializer.deserializeNodes(changeInfo.getAsJsonArray("nodes"));
            Difference<Node> res = applyNodeChanges(holder, nodes);

            if (res.isEmpty()) {
                return false;
            }

            Set<Node> added = res.getAdded();
            Set<Node> removed = res.getRemoved();

            for (Node n : added) {
                LoggedAction.build().source(this.sender).target(holder)
                        .description("webeditor", "add", n.getKey(), n.getValue(), n.getContexts())
                        .build().submit(this.plugin, this.sender);
            }
            for (Node n : removed) {
                LoggedAction.build().source(this.sender).target(holder)
                        .description("webeditor", "remove", n.getKey(), n.getValue(), n.getContexts())
                        .build().submit(this.plugin, this.sender);
            }

            Message.APPLY_EDITS_SUCCESS.send(this.sender, type, holder.getFormattedDisplayName());
            Message.APPLY_EDITS_SUCCESS_SUMMARY.send(this.sender, added.size(), removed.size());

            for (Node n : added) {
                Message.APPLY_EDITS_DIFF_ADDED.send(this.sender, n);
            }
            for (Node n : removed) {
                Message.APPLY_EDITS_DIFF_REMOVED.send(this.sender, n);
            }

            StorageAssistant.save(holder, this.sender, this.plugin);
            return true;
        }

        private Difference<Node> applyNodeChanges(PermissionHolder holder, Set<Node> nodes) {
            if (this.remoteSession != null) {

                WebEditorRequest request = this.remoteSession.request();
                if (request != null) {

                    List<Node> nodesBefore = request.getHolders().get(holder.getIdentifier());
                    if (nodesBefore != null) {

                        // if the initial data sent to the remote session is still known
                        // use that to calculate a diff of the changes made to avoid overriding
                        // modified/added/removed nodes since the editor session was created
                        Difference<Node> diff = new Difference<>();
                        diff.recordChanges(Difference.ChangeType.REMOVE, nodesBefore);
                        diff.recordChanges(Difference.ChangeType.ADD, nodes);

                        return holder.setNodes(DataType.NORMAL, diff, true);
                    }
                }
            }

            return holder.setNodes(DataType.NORMAL, nodes, true);
        }

        private boolean applyTrackChange(JsonObject changeInfo) {
            String id = changeInfo.get("id").getAsString();

            Track track = this.plugin.getStorage().loadTrack(id).join().orElse(null);
            if (track == null) {
                track = this.plugin.getStorage().createAndLoadTrack(id, CreationCause.WEB_EDITOR).join();
                if (this.session != null) {
                    this.session.includeCreatedTrack(track);
                }
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, track)) {
                Message.COMMAND_NO_PERMISSION.send(this.sender);
                return false;
            }

            List<String> before = track.getGroups();
            List<String> after = new ArrayList<>();
            changeInfo.getAsJsonArray("groups").forEach(e -> after.add(e.getAsString()));

            if (before.equals(after)) {
                return false;
            }

            Difference<String> diff = new Difference<>();
            diff.recordChanges(Difference.ChangeType.REMOVE, before);
            diff.recordChanges(Difference.ChangeType.ADD, after);

            Set<String> added = diff.getAdded();
            Set<String> removed = diff.getRemoved();

            track.setGroups(after);

            if (hasBeenReordered(before, after, added, removed)) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "reorder", after)
                        .build().submit(this.plugin, this.sender);
            }
            for (String n : added) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "add", n)
                        .build().submit(this.plugin, this.sender);
            }
            for (String n : removed) {
                LoggedAction.build().source(this.sender).target(track)
                        .description("webeditor", "remove", n)
                        .build().submit(this.plugin, this.sender);
            }

            Message.APPLY_EDITS_SUCCESS.send(this.sender, "track", Component.text(track.getName()));
            Message.APPLY_EDITS_SUCCESS_SUMMARY.send(this.sender, added.size(), removed.size());
            Message.APPLY_EDITS_TRACK_BEFORE.send(this.sender, before);
            Message.APPLY_EDITS_TRACK_AFTER.send(this.sender, after);

            StorageAssistant.save(track, this.sender, this.plugin);
            return true;
        }

        private boolean applyUserDelete(JsonElement changeInfo) {
            String id = changeInfo.getAsString();

            UUID uuid = Uuids.parse(id);
            if (uuid == null) {
                Message.APPLY_EDITS_TARGET_USER_NOT_UUID.send(this.sender, id);
                return false;
            }

            User user = this.plugin.getStorage().loadUser(uuid, null).join();
            if (user == null) {
                try {
                    this.plugin.getStorage().deletePlayerData(uuid).get();
                } catch (Exception e) {
                    e.printStackTrace();
                    Message.DELETE_ERROR.send(this.sender, Component.text(uuid.toString()));
                }
                return true;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, user)) {
                Message.COMMAND_NO_PERMISSION.send(this.sender);
                return false;
            }

            user.clearNodes(DataType.NORMAL, null, true);

            try {
                StorageAssistant.save(user, this.sender, this.plugin);
                this.plugin.getStorage().deletePlayerData(user.getUniqueId()).get();
            } catch (Exception e) {
                e.printStackTrace();
                Message.DELETE_ERROR.send(this.sender, user.getFormattedDisplayName());
                return true;
            }

            Message.DELETE_SUCCESS.send(this.sender, user.getFormattedDisplayName());

            LoggedAction.build().source(this.sender).target(user).targetType(Action.Target.Type.USER)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            if (this.session != null) {
                this.session.excludeDeletedUser(user);
            }

            return true;
        }

        private boolean applyGroupDelete(JsonElement changeInfo) {
            String groupName = changeInfo.getAsString();

            if (groupName.equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
                Message.DELETE_GROUP_ERROR_DEFAULT.send(this.sender);
                return true;
            }

            Group group = this.plugin.getStorage().loadGroup(groupName).join().orElse(null);
            if (group == null) {
                return false;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, group) || ArgumentPermissions.checkGroup(this.plugin, this.sender, group, ImmutableContextSetImpl.EMPTY)) {
                Message.COMMAND_NO_PERMISSION.send(this.sender);
                return false;
            }

            try {
                this.plugin.getStorage().deleteGroup(group, DeletionCause.WEB_EDITOR).get();
            } catch (Exception e) {
                e.printStackTrace();
                Message.DELETE_ERROR.send(this.sender, group.getFormattedDisplayName());
                return true;
            }

            Message.DELETE_SUCCESS.send(this.sender, group.getFormattedDisplayName());

            LoggedAction.build().source(this.sender).target(group)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            if (this.session != null) {
                this.session.excludeDeletedGroup(group);
            }

            return true;
        }

        private boolean applyTrackDelete(JsonElement changeInfo) {
            String trackName = changeInfo.getAsString();

            Track track = this.plugin.getStorage().loadTrack(trackName).join().orElse(null);
            if (track == null) {
                return false;
            }

            if (ArgumentPermissions.checkModifyPerms(this.plugin, this.sender, CommandPermission.APPLY_EDITS, track)) {
                Message.COMMAND_NO_PERMISSION.send(this.sender);
                return false;
            }

            try {
                this.plugin.getStorage().deleteTrack(track, DeletionCause.WEB_EDITOR).get();
            } catch (Exception e) {
                e.printStackTrace();
                Message.DELETE_ERROR.send(this.sender, Component.text(track.getName()));
                return true;
            }

            Message.DELETE_SUCCESS.send(this.sender, Component.text(trackName));

            LoggedAction.build().source(this.sender).target(track)
                    .description("webeditor", "delete")
                    .build().submit(this.plugin, this.sender);

            if (this.session != null) {
                this.session.excludeDeletedTrack(track);
            }

            return true;
        }

        private static <T> boolean hasBeenReordered(List<T> before, List<T> after, Collection<T> diffAdded, Collection<T> diffRemoved) {
            after = new ArrayList<>(after);
            before = new ArrayList<>(before);

            after.removeAll(diffAdded);
            before.removeAll(diffRemoved);

            return !before.equals(after);
        }

    }
}
