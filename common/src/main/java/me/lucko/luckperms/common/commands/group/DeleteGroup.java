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

package me.lucko.luckperms.common.commands.group;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.event.cause.DeletionCause;

import java.util.List;

public class DeleteGroup extends SingleCommand {
    public DeleteGroup() {
        super(CommandSpec.DELETE_GROUP, "DeleteGroup", CommandPermission.DELETE_GROUP, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        if (args.isEmpty()) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        String groupName = args.get(0).toLowerCase();

        if (groupName.equalsIgnoreCase(GroupManager.DEFAULT_GROUP_NAME)) {
            Message.DELETE_GROUP_ERROR_DEFAULT.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getStorage().loadGroup(groupName).join().orElse(null);
        if (group == null) {
            Message.GROUP_LOAD_ERROR.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), group)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        try {
            plugin.getStorage().deleteGroup(group, DeletionCause.COMMAND).get();
        } catch (Exception e) {
            plugin.getLogger().warn("Error whilst deleting group", e);
            Message.DELETE_ERROR.send(sender, group.getFormattedDisplayName());
            return CommandResult.FAILURE;
        }

        Message.DELETE_SUCCESS.send(sender, group.getFormattedDisplayName());

        LoggedAction.build().source(sender).targetName(groupName).targetType(Action.Target.Type.GROUP)
                .description("delete")
                .build().submit(plugin, sender);

        plugin.getSyncTaskBuffer().request();
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .complete(args);
    }
}
