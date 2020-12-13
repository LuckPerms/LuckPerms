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
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeType;

import java.util.List;

public class ParentClearTrack extends GenericChildCommand {
    public ParentClearTrack() {
        super(CommandSpec.PARENT_CLEAR_TRACK, "cleartrack", CommandPermission.USER_PARENT_CLEAR_TRACK, CommandPermission.GROUP_PARENT_CLEAR_TRACK, Predicates.is(0));
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

        int before = target.normalData().size();

        ImmutableContextSet context = args.getContextOrDefault(1, plugin).immutableCopy();

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, track.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        target.removeIf(DataType.NORMAL, context.isEmpty() ? null : context, NodeType.INHERITANCE.predicate(n -> track.containsGroup(n.getGroupName())), false);

        if (target.getType() == HolderType.USER) {
            plugin.getUserManager().giveDefaultIfNeeded(((User) target));
        }

        int changed = before - target.normalData().size();
        Message.PARENT_CLEAR_TRACK_SUCCESS.send(sender, target, track.getName(), context, changed);

        LoggedAction.build().source(sender).target(target)
                .description("parent", "cleartrack", track.getName(), context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.tracks(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
