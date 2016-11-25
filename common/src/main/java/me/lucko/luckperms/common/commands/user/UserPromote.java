/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.commands.user;

import com.google.common.base.Objects;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.event.events.UserPromoteEvent;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.internal.TrackLink;
import me.lucko.luckperms.common.api.internal.UserLink;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UserPromote extends SubCommand<User> {
    public UserPromote() {
        super("promote", "Promotes the user up a track", Permission.USER_PROMOTE, Predicates.notInRange(1, 3),
                Arg.list(
                        Arg.create("track", true, "the track to promote the user up"),
                        Arg.create("server", false, "the server to promote on"),
                        Arg.create("world", false, "the world to promote on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        final String trackName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getStorage().loadTrack(trackName).join()) {
            Message.TRACK_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Track track = plugin.getTrackManager().get(trackName);
        if (track == null) {
            Message.TRACK_DOES_NOT_EXIST.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (track.getSize() <= 1) {
            Message.TRACK_EMPTY.send(sender);
            return CommandResult.STATE_ERROR;
        }

        String server = ArgumentUtils.handleServer(1, args);
        String world = ArgumentUtils.handleWorld(2, args);

        // Load applicable groups
        Set<Node> nodes = new HashSet<>();
        for (Node node : user.getNodes()) {
            if (!node.isGroupNode()) {
                continue;
            }

            if (!node.getValue()) {
                continue;
            }

            String s = node.getServer().orElse(null);
            if (!Objects.equal(s, server)) {
                continue;
            }

            String w = node.getWorld().orElse(null);
            if (!Objects.equal(w, world)) {
                continue;
            }

            nodes.add(node);
        }

        Iterator<Node> it = nodes.iterator();
        while (it.hasNext()) {
            Node g = it.next();
            if (!track.containsGroup(g.getGroupName())) {
                it.remove();
            }
        }

        if (nodes.isEmpty()) {
            Message.USER_TRACK_ERROR_NOT_CONTAIN_GROUP.send(sender);
            return CommandResult.FAILURE;
        }

        if (nodes.size() != 1) {
            Message.TRACK_AMBIGUOUS_CALL.send(sender);
            return CommandResult.FAILURE;
        }

        final String old = nodes.stream().findAny().get().getGroupName();
        final String next;
        try {
            next = track.getNext(old);
        } catch (ObjectLacksException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
            return CommandResult.STATE_ERROR;
        }

        if (next == null) {
            Message.USER_PROMOTE_ERROR_ENDOFTRACK.send(sender, track.getName());
            return CommandResult.STATE_ERROR;
        }

        if (!plugin.getStorage().loadGroup(next).join()) {
            Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
            return CommandResult.STATE_ERROR;
        }

        Group nextGroup = plugin.getGroupManager().get(next);
        if (nextGroup == null) {
            Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
            return CommandResult.LOADING_ERROR;
        }

        try {
            user.unsetPermission(nodes.stream().findAny().get());
        } catch (ObjectLacksException ignored) {}
        try {
            user.setPermission(NodeFactory.newBuilder("group." + nextGroup.getName()).setServer(server).setWorld(world).build());
        } catch (ObjectAlreadyHasException ignored) {}

        if (server == null && world == null) {
            user.setPrimaryGroup(nextGroup.getName());
        }

        if (server == null) {
            Message.USER_PROMOTE_SUCCESS.send(sender, track.getName(), old, nextGroup.getDisplayName());
        } else {
            if (world == null) {
                Message.USER_PROMOTE_SUCCESS_SERVER.send(sender, track.getName(), old, nextGroup.getDisplayName(), server);
            } else {
                Message.USER_PROMOTE_SUCCESS_SERVER_WORLD.send(sender, track.getName(), old, nextGroup.getDisplayName(), server, world);
            }
        }

        Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), old, nextGroup.getDisplayName(), false));
        LogEntry.build().actor(sender).acted(user)
                .action("promote " + args.stream().collect(Collectors.joining(" ")))
                .build().submit(plugin, sender);
        save(user, sender, plugin);
        plugin.getApiProvider().fireEventAsync(new UserPromoteEvent(new TrackLink(track), new UserLink(user), old, nextGroup.getName()));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getTrackTabComplete(args, plugin);
    }
}
