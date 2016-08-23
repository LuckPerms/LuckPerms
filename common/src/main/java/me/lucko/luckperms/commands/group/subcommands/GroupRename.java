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

package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntry;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class GroupRename extends SubCommand<Group> {
    public GroupRename() {
        super("rename", "Rename this group", "/%s group <group> rename <new name>",
                Permission.TRACK_APPEND, Predicate.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String newGroupName = args.get(0).toLowerCase();
        if (ArgumentChecker.checkName(newGroupName)) {
            Message.GROUP_INVALID_ENTRY.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (plugin.getDatastore().loadGroup(newGroupName)) {
            Message.GROUP_ALREADY_EXISTS.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().createAndLoadGroup(newGroupName)) {
            Message.CREATE_GROUP_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Group newGroup = plugin.getGroupManager().get(newGroupName);
        if (newGroup == null) {
            Message.GROUP_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (!plugin.getDatastore().deleteGroup(group)) {
            Message.DELETE_GROUP_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        plugin.getGroupManager().copy(group, newGroup);

        Message.RENAME_SUCCESS.send(sender, group.getName(), newGroup.getName());
        LogEntry.build().actor(sender).acted(group).action("rename " + newGroup.getName()).build().submit(plugin, sender);
        save(newGroup, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
