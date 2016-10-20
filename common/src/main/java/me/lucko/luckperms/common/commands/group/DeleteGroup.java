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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.SingleMainCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DeleteGroup extends SingleMainCommand {
    public DeleteGroup() {
        super("DeleteGroup", "/%s deletegroup <group>", 1, Permission.DELETE_GROUP);
    }

    @Override
    protected CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        if (args.size() == 0) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String groupName = args.get(0).toLowerCase();

        if (groupName.equalsIgnoreCase(plugin.getConfiguration().getDefaultGroupName())) {
            Message.DELETE_GROUP_ERROR_DEFAULT.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (!plugin.getDatastore().loadGroup(groupName).getOrDefault(false)) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().get(groupName);
        if (group == null) {
            Message.GROUP_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (!plugin.getDatastore().deleteGroup(group).getOrDefault(false)) {
            Message.DELETE_GROUP_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Message.DELETE_SUCCESS.send(sender, group.getDisplayName());
        LogEntry.build().actor(sender).actedName(groupName).type('G').action("delete").build().submit(plugin, sender);
        plugin.runUpdateTask();
        return CommandResult.SUCCESS;
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        final List<String> groups = new ArrayList<>(plugin.getGroupManager().getAll().keySet());

        if (args.size() <= 1) {
            if (args.isEmpty() || args.get(0).equalsIgnoreCase("")) {
                return groups;
            }

            return groups.stream().filter(s -> s.toLowerCase().startsWith(args.get(0).toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
