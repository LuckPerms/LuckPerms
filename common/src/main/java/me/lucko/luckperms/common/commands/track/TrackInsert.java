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
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.model.data.DataMutateResult;

import java.util.List;

public class TrackInsert extends ChildCommand<Track> {
    public TrackInsert() {
        super(CommandSpec.TRACK_INSERT, "insert", CommandPermission.TRACK_INSERT, Predicates.not(2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Track target, ArgumentList args, String label) {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

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

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return CommandResult.LOADING_ERROR;
        }

        try {
            DataMutateResult result = target.insertGroup(group, pos - 1);

            if (result.wasSuccessful()) {
                Message.TRACK_INSERT_SUCCESS.send(sender, group.getName(), target.getName(), pos);
                if (target.getGroups().size() > 1) {
                    Message.TRACK_PATH_HIGHLIGHTED.send(sender, target.getGroups(), group.getName());
                }

                LoggedAction.build().source(sender).target(target)
                        .description("insert", group.getName(), pos)
                        .build().submit(plugin, sender);

                StorageAssistant.save(target, sender, plugin);
                return CommandResult.SUCCESS;
            } else {
                Message.TRACK_ALREADY_CONTAINS.send(sender, target.getName(), group);
                return CommandResult.STATE_ERROR;
            }

        } catch (IndexOutOfBoundsException e) {
            Message.TRACK_INSERT_ERROR_INVALID_POS.send(sender, pos);
            return CommandResult.INVALID_ARGS;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .complete(args);
    }
}
