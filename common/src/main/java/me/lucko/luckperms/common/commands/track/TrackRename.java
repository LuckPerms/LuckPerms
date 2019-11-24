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

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;

import java.util.List;

public class TrackRename extends SubCommand<Track> {
    public TrackRename(LocaleManager locale) {
        super(CommandSpec.TRACK_RENAME.localize(locale), "rename", CommandPermission.TRACK_RENAME, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String newTrackName = args.get(0).toLowerCase();
        if (!DataConstraints.TRACK_NAME_TEST.test(newTrackName)) {
            Message.TRACK_INVALID_ENTRY.send(sender, newTrackName);
            return CommandResult.INVALID_ARGS;
        }

        if (plugin.getStorage().loadTrack(newTrackName).join().isPresent()) {
            Message.ALREADY_EXISTS.send(sender, newTrackName);
            return CommandResult.INVALID_ARGS;
        }

        Track newTrack;
        try {
            newTrack = plugin.getStorage().createAndLoadTrack(newTrackName, CreationCause.COMMAND).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.CREATE_ERROR.send(sender, newTrackName);
            return CommandResult.FAILURE;
        }

        try {
            plugin.getStorage().deleteTrack(track, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            e.printStackTrace();
            Message.DELETE_ERROR.send(sender, track.getName());
            return CommandResult.FAILURE;
        }

        newTrack.setGroups(track.getGroups());

        Message.RENAME_SUCCESS.send(sender, track.getName(), newTrack.getName());

        LoggedAction.build().source(sender).target(track)
                .description("rename", newTrack.getName())
                .build().submit(plugin, sender);

        StorageAssistant.save(newTrack, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
