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

package me.lucko.luckperms.common.commands.impl.user;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

public class UserSwitchPrimaryGroup extends SubCommand<User> {
    public UserSwitchPrimaryGroup() {
        super("switchprimarygroup", "Switches the user's primary group", Permission.USER_SWITCHPRIMARYGROUP, Predicates.not(1),
                Arg.list(Arg.create("group", true, "the group to switch to"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) throws CommandException {
        String opt = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD);
        if (!opt.equals("stored")) {
            Message.USER_PRIMARYGROUP_WARN_OPTION.send(sender, opt);
        }

        Group group = plugin.getGroupManager().getIfLoaded(args.get(0).toLowerCase());
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (user.getPrimaryGroup().getStoredValue().equalsIgnoreCase(group.getName())) {
            Message.USER_PRIMARYGROUP_ERROR_ALREADYHAS.send(sender);
            return CommandResult.STATE_ERROR;
        }

        if (!user.inheritsGroup(group)) {
            Message.USER_PRIMARYGROUP_ERROR_NOTMEMBER.send(sender, user.getFriendlyName(), group.getName());
            user.setInheritGroup(group, ContextSet.empty());
        }

        user.getPrimaryGroup().setStoredValue(group.getName());
        Message.USER_PRIMARYGROUP_SUCCESS.send(sender, user.getFriendlyName(), group.getDisplayName());
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
