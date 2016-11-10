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

package me.lucko.luckperms.common.commands.track;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Command;
import me.lucko.luckperms.common.commands.MainCommand;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.track.subcommands.*;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.tracks.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackMainCommand extends MainCommand<Track> {
    public TrackMainCommand() {
        super("Track", "Track commands", "/%s track <track>", 2, ImmutableList.<Command<Track, ?>>builder()
                .add(new TrackInfo())
                .add(new TrackAppend())
                .add(new TrackInsert())
                .add(new TrackRemove())
                .add(new TrackClear())
                .add(new TrackRename())
                .add(new TrackClone())
                .build()
        );
    }

    @Override
    protected Track getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        if (!plugin.getDatastore().loadTrack(target).getUnchecked()) {
            Message.TRACK_NOT_FOUND.send(sender);
            return null;
        }

        Track track = plugin.getTrackManager().get(target);
        if (track == null) {
            Message.TRACK_NOT_FOUND.send(sender);
            return null;
        }

        return track;
    }

    @Override
    protected void cleanup(Track track, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getTrackManager().getAll().keySet());
    }
}
