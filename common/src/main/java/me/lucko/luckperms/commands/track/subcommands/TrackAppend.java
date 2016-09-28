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

package me.lucko.luckperms.commands.track.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class TrackAppend extends SubCommand<Track> {
    public TrackAppend() {
        super("append", "Appends a group onto the end of the track", Permission.TRACK_APPEND, Predicate.not(1),
                Arg.list(Arg.create("group", true, "the group to append"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().loadGroup(groupName)) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().get(groupName);
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        try {
            track.appendGroup(group);
            Message.TRACK_APPEND_SUCCESS.send(sender, group.getName(), track.getName());
            if (track.getGroups().size() > 1) {
                Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups(), group.getName()));
            }
            LogEntry.build().actor(sender).acted(track)
                    .action("append " + group.getName())
                    .build().submit(plugin, sender);
            save(track, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.TRACK_ALREADY_CONTAINS.send(sender, track.getName(), group.getName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
