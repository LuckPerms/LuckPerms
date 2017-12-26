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

package me.lucko.luckperms.common.commands.impl.user;

import com.google.common.collect.Iterables;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserDemote extends SubCommand<User> {
    public UserDemote(LocaleManager locale) {
        super(CommandSpec.USER_DEMOTE.spec(locale), "demote", CommandPermission.USER_DEMOTE, Predicates.is(0));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), user)) {
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

        boolean silent = args.remove("-s");
        MutableContextSet context = ArgumentUtils.handleContext(1, args, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, getPermission().get(), context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        // Load applicable groups
        Set<Node> nodes = user.getEnduringNodes().values().stream()
                .filter(Node::isGroupNode)
                .filter(Node::getValuePrimitive)
                .filter(node -> node.getFullContexts().makeImmutable().equals(context.makeImmutable()))
                .collect(Collectors.toSet());

        nodes.removeIf(g -> !track.containsGroup(g.getGroupName()));

        if (nodes.isEmpty()) {
            Message.USER_TRACK_ERROR_NOT_CONTAIN_GROUP.send(sender, user.getFriendlyName(), track.getName());
            return CommandResult.FAILURE;
        }

        if (nodes.size() != 1) {
            Message.TRACK_AMBIGUOUS_CALL.send(sender, user.getFriendlyName());
            return CommandResult.FAILURE;
        }

        final Node oldNode = Iterables.getFirst(nodes, null);
        final String old = oldNode.getGroupName();
        final String previous;
        try {
            previous = track.getPrevious(old);
        } catch (IllegalArgumentException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
            return CommandResult.STATE_ERROR;
        }

        if (ArgumentPermissions.checkArguments(plugin, sender, getPermission().get(), track.getName(), oldNode.getGroupName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (previous == null) {
            user.unsetPermission(oldNode);
            Message.USER_DEMOTE_ENDOFTRACK.send(sender, track.getName(), user.getFriendlyName(), old);

            ExtendedLogEntry.build().actor(sender).acted(user)
                    .action("demote", track.getName(), context)
                    .build().submit(plugin, sender);

            save(user, sender, plugin);
            plugin.getApiProvider().getEventFactory().handleUserDemote(user, track, old, null);
            return CommandResult.SUCCESS;
        }

        if (!plugin.getStorage().loadGroup(previous).join().isPresent()) {
            Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
            return CommandResult.STATE_ERROR;
        }

        Group previousGroup = plugin.getGroupManager().getIfLoaded(previous);
        if (previousGroup == null) {
            Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
            return CommandResult.LOADING_ERROR;
        }

        user.unsetPermission(oldNode);
        user.setPermission(NodeFactory.buildGroupNode(previousGroup.getName()).withExtraContext(context).build());

        if (context.isEmpty() && user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME).equalsIgnoreCase(old)) {
            user.getPrimaryGroup().setStoredValue(previousGroup.getName());
        }

        Message.USER_DEMOTE_SUCCESS.send(sender, user.getFriendlyName(), track.getName(), old, previousGroup.getFriendlyName(), CommandUtils.contextSetToString(context));
        if (!silent) {
            Message.EMPTY.send(sender, CommandUtils.listToArrowSep(track.getGroups(), previousGroup.getName(), old, true));
        }

        ExtendedLogEntry.build().actor(sender).acted(user)
                .action("demote", track.getName(), context)
                .build().submit(plugin, sender);

        save(user, sender, plugin);
        plugin.getApiProvider().getEventFactory().handleUserDemote(user, track, old, previousGroup.getName());
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getTrackTabComplete(args, plugin);
    }
}
