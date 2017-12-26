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

package me.lucko.luckperms.common.commands.impl.generic.parent;

import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getGroupTabComplete;
import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getTrackTabComplete;

public class ParentSetTrack extends SharedSubCommand {
    public ParentSetTrack(LocaleManager locale) {
        super(CommandSpec.PARENT_SET_TRACK.spec(locale), "settrack", CommandPermission.USER_PARENT_SET_TRACK, CommandPermission.GROUP_PARENT_SET_TRACK, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        final String trackName = args.get(0).toLowerCase();
        if (!DataConstraints.TRACK_NAME_TEST.test(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, trackName);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getStorage().loadTrack(trackName).join().isPresent()) {
            Message.DOES_NOT_EXIST.send(sender, trackName);
            return CommandResult.INVALID_ARGS;
        }

        Track track = plugin.getTrackManager().getIfLoaded(trackName);
        if (track == null) {
            Message.DOES_NOT_EXIST.send(sender, trackName);
            return CommandResult.LOADING_ERROR;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender, track.getName());
            return CommandResult.STATE_ERROR;
        }

        int index = ArgumentUtils.handleIntOrElse(1, args, -1);
        String groupName;
        if (index > 0) {
            List<String> trackGroups = track.getGroups();
            if ((index - 1) >= trackGroups.size()) {
                Message.DOES_NOT_EXIST.send(sender, index);
                return CommandResult.INVALID_ARGS;
            }
            groupName = track.getGroups().get(index - 1);
        } else {
            groupName = ArgumentUtils.handleName(1, args);
            if (!track.containsGroup(groupName)) {
                Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), groupName);
                return CommandResult.INVALID_ARGS;
            }
        }

        MutableContextSet context = ArgumentUtils.handleContext(2, args, plugin);

        if (!plugin.getStorage().loadGroup(groupName).join().isPresent()) {
            Message.DOES_NOT_EXIST.send(sender, groupName);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) {
            Message.DOES_NOT_EXIST.send(sender, groupName);
            return CommandResult.LOADING_ERROR;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (ArgumentPermissions.checkArguments(plugin, sender, permission, track.getName(), group.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        holder.removeIf(node -> node.isGroupNode() && node.getFullContexts().equals(context) && track.containsGroup(node.getGroupName()));
        holder.setPermission(NodeFactory.buildGroupNode(group.getName()).withExtraContext(context).build());

        Message.SET_TRACK_PARENT_SUCCESS.send(sender, holder.getFriendlyName(), track.getName(), group.getFriendlyName(), CommandUtils.contextSetToString(context));

        ExtendedLogEntry.build().actor(sender).acted(holder)
                .action("parent", "settrack", track.getName(), groupName, context)
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (args.size() == 0 || args.size() == 1) {
            return getTrackTabComplete(args, plugin);
        }

        args.remove(0);
        return getGroupTabComplete(args, plugin);
    }
}
