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

package me.lucko.luckperms.common.commands.impl.group;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.event.cause.DeletionCause;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

public class GroupRename extends SubCommand<Group> {
    public GroupRename() {
        super("rename", "Rename the group", Permission.GROUP_RENAME, Predicates.not(1),
                Arg.list(Arg.create("name", true, "the new name"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) throws CommandException {
        String newGroupName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(newGroupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (plugin.getStorage().loadGroup(newGroupName).join()) {
            Message.GROUP_ALREADY_EXISTS.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getStorage().createAndLoadGroup(newGroupName, CreationCause.COMMAND).join()) {
            Message.CREATE_GROUP_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Group newGroup = plugin.getGroupManager().getIfLoaded(newGroupName);
        if (newGroup == null) {
            Message.GROUP_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (!plugin.getStorage().deleteGroup(group, DeletionCause.COMMAND).join()) {
            Message.DELETE_GROUP_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        newGroup.replaceNodes(group.getNodes());

        Message.RENAME_SUCCESS.send(sender, group.getName(), newGroup.getName());
        LogEntry.build().actor(sender).acted(group).action("rename " + newGroup.getName()).build().submit(plugin, sender);
        save(newGroup, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
