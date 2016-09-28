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

package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.event.events.UserPromoteEvent;
import me.lucko.luckperms.api.implementation.internal.TrackLink;
import me.lucko.luckperms.api.implementation.internal.UserLink;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class UserPromote extends SubCommand<User> {
    public UserPromote() {
        super("promote", "Promotes the user up a track", Permission.USER_PROMOTE, Predicate.not(1),
                Arg.list(Arg.create("track", true, "the track to promote the user up"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        final String trackName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(trackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().loadTrack(trackName)) {
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
        final String next;
        try {
            next = track.getNext(old);
        } catch (ObjectLacksException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), old);
            Message.USER_PROMOTE_ERROR_NOT_CONTAIN_GROUP.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (next == null) {
            Message.USER_PROMOTE_ERROR_ENDOFTRACK.send(sender, track.getName());
            return CommandResult.STATE_ERROR;
        }

        if (!plugin.getDatastore().loadGroup(next)) {
            Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
            return CommandResult.STATE_ERROR;
        }

        Group nextGroup = plugin.getGroupManager().get(next);
        if (nextGroup == null) {
            Message.USER_PROMOTE_ERROR_MALFORMED.send(sender, next);
            return CommandResult.LOADING_ERROR;
        }

        try {
            user.unsetPermission("group." + old);
        } catch (ObjectLacksException ignored) {}
        try {
            user.addGroup(nextGroup);
        } catch (ObjectAlreadyHasException ignored) {}
        user.setPrimaryGroup(nextGroup.getName());

        Message.USER_PROMOTE_SUCCESS_PROMOTE.send(sender, track.getName(), old, nextGroup.getDisplayName());
        Message.USER_PROMOTE_SUCCESS_REMOVE.send(sender, user.getName(), old, nextGroup.getDisplayName(), nextGroup.getDisplayName());
        Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), old, nextGroup.getDisplayName(), false));
        LogEntry.build().actor(sender).acted(user)
                .action("promote " + track.getName() + "(from " + old + " to " + nextGroup.getName() + ")")
                .build().submit(plugin, sender);
        save(user, sender, plugin);
        plugin.getApiProvider().fireEventAsync(new UserPromoteEvent(new TrackLink(track), new UserLink(user), old, nextGroup.getName()));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getTrackTabComplete(args, plugin);
    }
}
