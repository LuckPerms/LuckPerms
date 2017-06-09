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
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.DataConstraints;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getTrackTabComplete;

public class ParentClearTrack extends SharedSubCommand {
    public ParentClearTrack() {
        super("cleartrack", "Clears all parents on a given track", Permission.USER_PARENT_CLEAR_TRACK, Permission.GROUP_PARENT_CLEAR_TRACK, Predicates.is(0),
                Arg.list(
                        Arg.create("track", true, "the track to remove on"),
                        Arg.create("context...", false, "the contexts to filter by")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        final String trackName = args.get(0).toLowerCase();
        if (!DataConstraints.TRACK_NAME_TEST.test(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getStorage().loadTrack(trackName).join()) {
            Message.TRACK_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Track track = plugin.getTrackManager().getIfLoaded(trackName);
        if (track == null) {
            Message.TRACK_DOES_NOT_EXIST.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender);
            return CommandResult.STATE_ERROR;
        }

        int before = holder.getNodes().size();

        MutableContextSet context = ArgumentUtils.handleContext(1, args, plugin);
        if (context.isEmpty()) {
            holder.removeIf(node -> node.isGroupNode() && track.containsGroup(node.getGroupName()));
        } else {
            holder.removeIf(node -> node.isGroupNode() && node.getFullContexts().equals(context) && track.containsGroup(node.getGroupName()));
        }

        if (holder instanceof User) {
            plugin.getUserManager().giveDefaultIfNeeded(((User) holder), false);
        }

        int changed = before - holder.getNodes().size();

        if (changed == 1) {
            Message.PARENT_CLEAR_TRACK_SUCCESS_SINGULAR.send(sender, holder.getFriendlyName(), track.getName(), Util.contextSetToString(context), changed);
        } else {
            Message.PARENT_CLEAR_TRACK_SUCCESS.send(sender, holder.getFriendlyName(), track.getName(), Util.contextSetToString(context), changed);
        }

        LogEntry.build().actor(sender).acted(holder)
                .action("parent cleartrack " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getTrackTabComplete(args, plugin);
    }
}
