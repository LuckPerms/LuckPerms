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

import java.util.List;

public class TrackClone extends SubCommand<Track> {
    public TrackClone() {
        super("clone", "Clone the track", Permission.TRACK_CLONE, Predicate.not(1),
                Arg.list(Arg.create("name", true, "the name of the clone"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String newTrackName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(newTrackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (plugin.getDatastore().loadTrack(newTrackName).getOrDefault(false)) {
            Message.TRACK_ALREADY_EXISTS.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().createAndLoadTrack(newTrackName).getOrDefault(false)) {
            Message.CREATE_TRACK_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Track newTrack = plugin.getTrackManager().get(newTrackName);
        if (newTrack == null) {
            Message.TRACK_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        newTrack.setGroups(track.getGroups());

        Message.CLONE_SUCCESS.send(sender, track.getName(), newTrack.getName());
        LogEntry.build().actor(sender).acted(track).action("clone " + newTrack.getName()).build().submit(plugin, sender);
        save(newTrack, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
