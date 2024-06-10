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

package me.lucko.luckperms.common.commands.track;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.ParentCommand;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.CaffeineFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TrackParentCommand extends ParentCommand<Track, String> {

    // we use a lock per unique track
    // this helps prevent race conditions where commands are being executed concurrently
    // and overriding each other.
    // it's not a great solution, but it mostly works.
    private final LoadingCache<String, ReentrantLock> locks = CaffeineFactory.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(key -> new ReentrantLock());

    public TrackParentCommand() {
        super(CommandSpec.TRACK, "Track", Type.TARGETED, ImmutableList.<Command<Track>>builder()
                .add(new TrackInfo())
                .add(new TrackEditor())
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
    protected String parseTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return target.toLowerCase(Locale.ROOT);
    }

    @Override
    protected Track getTarget(String target, LuckPermsPlugin plugin, Sender sender) {
        return StorageAssistant.loadTrack(target, sender, plugin);
    }

    @Override
    protected ReentrantLock getLockForTarget(String target) {
        return this.locks.get(target);
    }

    @Override
    protected void cleanup(Track track, LuckPermsPlugin plugin) {

    }

    @Override
    protected List<String> getTargets(LuckPermsPlugin plugin) {
        return new ArrayList<>(plugin.getTrackManager().getAll().keySet());
    }
}
