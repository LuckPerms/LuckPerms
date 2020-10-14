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

package me.lucko.luckperms.common.commands.generic.parent;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeType;

import java.util.List;

public class ParentSetTrack extends GenericChildCommand {
    public ParentSetTrack() {
        super(CommandSpec.PARENT_SET_TRACK, "settrack", CommandPermission.USER_PARENT_SET_TRACK, CommandPermission.GROUP_PARENT_SET_TRACK, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        final String trackName = args.get(0).toLowerCase();
        if (!DataConstraints.TRACK_NAME_TEST.test(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, trackName);
            return CommandResult.INVALID_ARGS;
        }

        Track track = StorageAssistant.loadTrack(trackName, sender, plugin);
        if (track == null) {
            return CommandResult.LOADING_ERROR;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender, track.getName());
            return CommandResult.STATE_ERROR;
        }

        int index = args.getIntOrDefault(1, -1);
        String groupName;
        if (index > 0) {
            List<String> trackGroups = track.getGroups();
            if ((index - 1) >= trackGroups.size()) {
                Message.DOES_NOT_EXIST.send(sender, String.valueOf(index));
                return CommandResult.INVALID_ARGS;
            }
            groupName = track.getGroups().get(index - 1);
        } else {
            groupName = args.getLowercase(1, DataConstraints.GROUP_NAME_TEST);
            if (!track.containsGroup(groupName)) {
                Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), groupName);
                return CommandResult.INVALID_ARGS;
            }
        }

        ImmutableContextSet context = args.getContextOrDefault(2, plugin).immutableCopy();

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return CommandResult.LOADING_ERROR;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, group, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, track.getName(), group.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        target.removeIf(DataType.NORMAL, context, NodeType.INHERITANCE.predicate(n -> track.containsGroup(n.getGroupName())), false);
        target.setNode(DataType.NORMAL, Inheritance.builder(group.getName()).withContext(context).build(), true);

        Message.SET_TRACK_PARENT_SUCCESS.send(sender, target.getFormattedDisplayName(), track.getName(), group.getFormattedDisplayName(), context);

        LoggedAction.build().source(sender).target(target)
                .description("parent", "settrack", track.getName(), groupName, context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.tracks(plugin))
                .at(1, TabCompletions.groups(plugin))
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
