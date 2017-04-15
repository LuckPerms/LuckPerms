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

package me.lucko.luckperms.common.commands.impl.track;

import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SingleCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteTrack extends SingleCommand {
    public DeleteTrack() {
        super("DeleteTrack", "Delete a track", "/%s deletetrack <track>", Permission.DELETE_TRACK, Predicates.not(1),
                Arg.list(
                        Arg.create("name", true, "the name of the track")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String trackName = args.get(0).toLowerCase();
        if (!plugin.getStorage().loadTrack(trackName).join()) {
            Message.TRACK_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Track track = plugin.getTrackManager().getIfLoaded(trackName);
        if (track == null) {
            Message.TRACK_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (!plugin.getStorage().deleteTrack(track, DeletionCause.COMMAND).join()) {
            Message.DELETE_TRACK_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Message.DELETE_SUCCESS.send(sender, trackName);
        LogEntry.build().actor(sender).actedName(trackName).type('T').action("delete").build().submit(plugin, sender);
        plugin.getUpdateTaskBuffer().request();
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        final List<String> tracks = new ArrayList<>(plugin.getTrackManager().getAll().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return tracks;
            }

            return tracks.stream()
                    .filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
