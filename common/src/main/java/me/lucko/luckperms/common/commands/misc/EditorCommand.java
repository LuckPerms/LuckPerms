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

import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.webeditor.WebEditorRequest;
import me.lucko.luckperms.common.webeditor.WebEditorSession;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditorCommand extends SingleCommand {
    public EditorCommand() {
        super(CommandSpec.EDITOR, "Editor", CommandPermission.EDITOR, Predicates.notInRange(0, 2));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Type type = Type.ALL;
        String filter = null;

        // attempt to parse type
        String arg0 = args.getOrDefault(0, null);
        if (arg0 != null) {
            try {
                type = Type.valueOf(arg0.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                // assume they meant it as a filter
                filter = arg0;
            }

            if (filter == null) {
                filter = args.getOrDefault(1, null);
            }
        }

        // run a sync task
        plugin.getSyncTaskBuffer().requestDirectly();

        // collect holders
        List<PermissionHolder> holders = new ArrayList<>();
        List<Track> tracks = new ArrayList<>();

        if (type.includingGroups) {
            WebEditorRequest.includeMatchingGroups(holders, Predicates.alwaysTrue(), plugin);
            tracks.addAll(plugin.getTrackManager().getAll().values());
        }

        if (type.includingUsers) {
            // include all online players
            ConstraintNodeMatcher<Node> matcher = filter != null ? StandardNodeMatchers.keyStartsWith(filter) : null;
            WebEditorRequest.includeMatchingUsers(holders, matcher, type.includingOffline, plugin);
        }

        if (holders.isEmpty()) {
            Message.EDITOR_NO_MATCH.send(sender);
            return;
        }

        // remove holders which the sender doesn't have perms to view
        holders.removeIf(holder -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder) || ArgumentPermissions.checkGroup(plugin, sender, holder, ImmutableContextSetImpl.EMPTY));
        tracks.removeIf(track -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), track));

        // they don't have perms to view any of them
        if (holders.isEmpty() && tracks.isEmpty()) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Message.EDITOR_START.send(sender);

        WebEditorSession.create(holders, tracks, sender, label, plugin).open();
    }

    private enum Type {
        ALL(true, true, true),
        ONLINE(true, true, false),
        USERS(true, false, true),
        GROUPS(false, true, true);

        private final boolean includingUsers;
        private final boolean includingGroups;
        private final boolean includingOffline;

        Type(boolean includingUsers, boolean includingGroups, boolean includingOffline) {
            this.includingUsers = includingUsers;
            this.includingGroups = includingGroups;
            this.includingOffline = includingOffline;
        }
    }
}
