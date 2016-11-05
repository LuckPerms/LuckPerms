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

package me.lucko.luckperms.common.commands.track.subcommands;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

public class TrackRemove extends SubCommand<Track> {
    public TrackRemove() {
        super("remove", "Removes a group from the track", Permission.TRACK_REMOVE, Predicates.not(1),
                Arg.list(Arg.create("group", true, "the group to remove"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        try {
            track.removeGroup(groupName);
            Message.TRACK_REMOVE_SUCCESS.send(sender, groupName, track.getName());
            if (track.getGroups().size() > 1) {
                Message.EMPTY.send(sender, Util.listToArrowSep(track.getGroups()));
            }
            LogEntry.build().actor(sender).acted(track)
                    .action("remove " + groupName)
                    .build().submit(plugin, sender);
            save(track, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.TRACK_DOES_NOT_CONTAIN.send(sender, track.getName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
