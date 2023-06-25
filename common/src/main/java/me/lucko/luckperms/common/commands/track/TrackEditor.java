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

package me.lucko.luckperms.common.commands.track;

import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.matcher.ConstraintNodeMatcher;
import me.lucko.luckperms.common.node.matcher.StandardNodeMatchers;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.webeditor.WebEditorRequest;
import me.lucko.luckperms.common.webeditor.WebEditorSession;
import net.luckperms.api.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TrackEditor extends ChildCommand<Track> {
    public TrackEditor() {
        super(CommandSpec.TRACK_EDITOR, "editor", CommandPermission.TRACK_EDITOR, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Track target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        // run a sync task
        plugin.getSyncTaskBuffer().requestDirectly();

        // collect groups
        List<Group> groups = new ArrayList<>();
        WebEditorRequest.includeMatchingGroups(groups, target::containsGroup, plugin);

        // remove groups which the sender doesn't have perms to view
        groups.removeIf(holder -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder) || ArgumentPermissions.checkGroup(plugin, sender, holder, ImmutableContextSetImpl.EMPTY));

        if (groups.isEmpty()) {
            Message.EDITOR_NO_MATCH.send(sender);
            return;
        }

        // then collect users which are a member of any of those groups
        // (users which are on the track)
        List<PermissionHolder> users = new ArrayList<>();
        if (!groups.isEmpty()) {
            List<ConstraintNodeMatcher<Node>> matchers = groups.stream()
                    .map(group -> StandardNodeMatchers.key(Inheritance.key(group.getName())))
                    .collect(Collectors.toList());

            WebEditorRequest.includeMatchingUsers(users, matchers, true, plugin);
        }

        // remove users which the sender doesn't have perms to view
        users.removeIf(holder -> ArgumentPermissions.checkViewPerms(plugin, sender, getPermission().get(), holder));

        List<PermissionHolder> holders = new ArrayList<>(groups.size() + users.size());
        holders.addAll(groups);
        holders.addAll(users);

        Message.EDITOR_START.send(sender);

        WebEditorSession.create(holders, Collections.singletonList(target), sender, label, plugin).open();
    }

}
