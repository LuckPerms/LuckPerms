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
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.model.data.DataMutateResult;

import java.util.List;

public class TrackAppend extends SubCommand<Track> {
    public TrackAppend(LocaleManager locale) {
        super(CommandSpec.TRACK_APPEND.localize(locale), "append", CommandPermission.TRACK_APPEND, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return CommandResult.LOADING_ERROR;
        }

        DataMutateResult result = track.appendGroup(group);

        if (result.wasSuccessful()) {
            Message.TRACK_APPEND_SUCCESS.send(sender, group.getName(), track.getName());
            if (track.getGroups().size() > 1) {
                Message.BLANK.send(sender, MessageUtils.listToArrowSep(track.getGroups(), group.getName()));
            }

            LoggedAction.build().source(sender).target(track)
                    .description("append", group.getName())
                    .build().submit(plugin, sender);

            StorageAssistant.save(track, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.TRACK_ALREADY_CONTAINS.send(sender, track.getName(), group.getName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .complete(args);
    }
}
