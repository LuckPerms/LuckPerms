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

package me.lucko.luckperms.common.commands.user;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;

public class UserSetPrimaryGroup extends SubCommand<User> {
    public UserSetPrimaryGroup() {
        super("setprimarygroup", "Sets the user's primary group", Permission.USER_SETPRIMARYGROUP, Predicates.not(1),
                Arg.list(Arg.create("group", true, "the group to set as the primary group"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        Group group = plugin.getGroupManager().get(args.get(0).toLowerCase());
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (user.getPrimaryGroup().equalsIgnoreCase(group.getName())) {
            Message.USER_PRIMARYGROUP_ERROR_ALREADYHAS.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (!user.inheritsGroup(group)) {
            Message.USER_PRIMARYGROUP_ERROR_NOTMEMBER.send(sender, user.getName(), group.getName());
            try {
                user.setInheritGroup(group);
            } catch (ObjectAlreadyHasException ignored) {}
        }

        user.setPrimaryGroup(group.getName());
        Message.USER_PRIMARYGROUP_SUCCESS.send(sender, user.getName(), group.getDisplayName());
        LogEntry.build().actor(sender).acted(user)
                .action("setprimarygroup " + group.getName())
                .build().submit(plugin, sender);

        save(user, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
