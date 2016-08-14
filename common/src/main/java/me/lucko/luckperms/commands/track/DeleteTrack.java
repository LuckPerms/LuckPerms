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

package me.lucko.luckperms.commands.track;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntryBuilder;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteTrack extends SingleMainCommand {
    public DeleteTrack() {
        super("DeleteTrack", "/%s deletetrack <track>", 1, Permission.DELETE_TRACK);
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String trackName = args.get(0).toLowerCase();
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
            Message.TRACK_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (!plugin.getDatastore().deleteTrack(track)) {
            Message.DELETE_TRACK_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Message.DELETE_SUCCESS.send(sender, trackName);
        LogEntryBuilder.get().actor(sender).actedName(trackName).type('T').action("delete").submit(plugin);
        plugin.runUpdateTask();
        return CommandResult.SUCCESS;
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> tracks = new ArrayList<>(plugin.getTrackManager().getAll().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return tracks;
            }

            return tracks.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase())).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
