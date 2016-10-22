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

package me.lucko.luckperms.common.commands.user.subcommands;

import me.lucko.luckperms.api.event.events.UserDemoteEvent;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.api.internal.TrackLink;
import me.lucko.luckperms.common.api.internal.UserLink;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

public class UserDemote extends SubCommand<User> {
    public UserDemote() {
        super("demote", "Demotes the user down a track", Permission.USER_DEMOTE, Predicate.not(1),
                Arg.list(Arg.create("track", true, "the track to demote the user down"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        final String trackName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().loadTrack(trackName).getUnchecked()) {
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

        final String old = user.getPrimaryGroup();
        final String previous;
        try {
            previous = track.getPrevious(old);
        } catch (ObjectLacksException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
            Message.USER_DEMOTE_ERROR_NOT_CONTAIN_GROUP.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (previous == null) {
            Message.USER_DEMOTE_ERROR_ENDOFTRACK.send(sender, track.getName());
            return CommandResult.STATE_ERROR;
        }

        if (!plugin.getDatastore().loadGroup(previous).getUnchecked()) {
            Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
            return CommandResult.STATE_ERROR;
        }

        Group previousGroup = plugin.getGroupManager().get(previous);
        if (previousGroup == null) {
            Message.USER_DEMOTE_ERROR_MALFORMED.send(sender, previous);
            return CommandResult.LOADING_ERROR;
        }

        try {
            user.unsetPermission("group." + old);
        } catch (ObjectLacksException ignored) {}
        try {
            user.setInheritGroup(previousGroup);
        } catch (ObjectAlreadyHasException ignored) {}
        user.setPrimaryGroup(previousGroup.getName());

        Message.USER_DEMOTE_SUCCESS_PROMOTE.send(sender, track.getName(), old, previousGroup.getDisplayName());
        Message.USER_DEMOTE_SUCCESS_REMOVE.send(sender, user.getName(), old, previousGroup.getDisplayName(), previousGroup.getDisplayName());
        Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), previousGroup.getDisplayName(), old, true));
        LogEntry.build().actor(sender).acted(user)
                .action("demote " + track.getName() + "(from " + old + " to " + previousGroup.getName() + ")")
                .build().submit(plugin, sender);
        save(user, sender, plugin);
        plugin.getApiProvider().fireEventAsync(new UserDemoteEvent(new TrackLink(track), new UserLink(user), old, previousGroup.getName()));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getTrackTabComplete(args, plugin);
    }
}
