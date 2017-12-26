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

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DataConstraints;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

public class TrackInsert extends SubCommand<Track> {
    public TrackInsert(LocaleManager locale) {
        super(CommandSpec.TRACK_INSERT.spec(locale), "insert", CommandPermission.TRACK_INSERT, Predicates.not(2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track track, List<String> args, String label) throws CommandException {
        String groupName = args.get(0).toLowerCase();
        if (!DataConstraints.GROUP_NAME_TEST.test(groupName)) {
            sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        int pos;
        try {
            pos = Integer.parseInt(args.get(1));
        } catch (NumberFormatException e) {
            Message.TRACK_INSERT_ERROR_NUMBER.send(sender, args.get(1));
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getStorage().loadGroup(groupName).join().isPresent()) {
            Message.DOES_NOT_EXIST.send(sender, groupName);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) {
            Message.DOES_NOT_EXIST.send(sender, groupName);
            return CommandResult.LOADING_ERROR;
        }

        try {
            DataMutateResult result = track.insertGroup(group, pos - 1);

            if (result.asBoolean()) {
                Message.TRACK_INSERT_SUCCESS.send(sender, group.getName(), track.getName(), pos);
                if (track.getGroups().size() > 1) {
                    Message.EMPTY.send(sender, CommandUtils.listToArrowSep(track.getGroups(), group.getName()));
                }

                ExtendedLogEntry.build().actor(sender).acted(track)
                        .action("insert", group.getName(), pos)
                        .build().submit(plugin, sender);

                save(track, sender, plugin);
                return CommandResult.SUCCESS;
            } else {
                Message.TRACK_ALREADY_CONTAINS.send(sender, track.getName(), group.getName());
                return CommandResult.STATE_ERROR;
            }

        } catch (IndexOutOfBoundsException e) {
            Message.TRACK_INSERT_ERROR_INVALID_POS.send(sender, pos);
            return CommandResult.INVALID_ARGS;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
