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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.commands.abstraction.Command;
import me.lucko.luckperms.common.commands.abstraction.MainCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.ArrayList;
import java.util.List;

public class TrackMainCommand extends MainCommand<Track> {
    public TrackMainCommand(LocaleManager locale) {
        super(CommandSpec.TRACK.spec(locale), "Track", 2, ImmutableList.<Command<Track, ?>>builder()
                .add(new TrackInfo(locale))
                .add(new TrackAppend(locale))
                .add(new TrackInsert(locale))
                .add(new TrackRemove(locale))
                .add(new TrackClear(locale))
                .add(new TrackRename(locale))
                .add(new TrackClone(locale))
                .build()
        );
    }

    @Override
    protected Track getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        if (!plugin.getStorage().loadTrack(target).join()) {
            Message.TRACK_NOT_FOUND.send(sender);
            return null;
        }

        Track track = plugin.getTrackManager().getIfLoaded(target);
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
