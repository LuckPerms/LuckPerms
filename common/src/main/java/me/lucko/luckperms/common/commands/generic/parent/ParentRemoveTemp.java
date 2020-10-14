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

package me.lucko.luckperms.common.commands.generic.parent;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.misc.DataConstraints;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import java.time.Duration;
import java.util.List;

public class ParentRemoveTemp extends GenericChildCommand {
    public ParentRemoveTemp() {
        super(CommandSpec.PARENT_REMOVE_TEMP, "removetemp", CommandPermission.USER_PARENT_REMOVE_TEMP, CommandPermission.GROUP_PARENT_REMOVE_TEMP, Predicates.is(0));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        String groupName = args.getLowercase(0, DataConstraints.GROUP_NAME_TEST_ALLOW_SPACE);
        Duration duration = args.getDurationOrDefault(1, null);
        int fromIndex = duration == null ? 1 : 2;
        MutableContextSet context = args.getContextOrDefault(fromIndex, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, groupName, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, groupName)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        DataMutateResult.WithMergedNode result = target.unsetNode(DataType.NORMAL, Inheritance.builder(groupName).expiry(10L).withContext(context).build(), duration);
        if (result.getResult().wasSuccessful()) {
            Node mergedNode = result.getMergedNode();
            //noinspection ConstantConditions
            if (mergedNode != null) {
                Message.UNSET_TEMP_INHERIT_SUBTRACT_SUCCESS.send(sender, target.getFormattedDisplayName(), groupName, mergedNode.getExpiryDuration(), context, duration);

                LoggedAction.build().source(sender).target(target)
                        .description("parent", "removetemp", groupName, duration, context)
                        .build().submit(plugin, sender);
            } else {
                Message.UNSET_TEMP_INHERIT_SUCCESS.send(sender, target.getFormattedDisplayName(), groupName, context);

                LoggedAction.build().source(sender).target(target)
                        .description("parent", "removetemp", groupName, context)
                        .build().submit(plugin, sender);
            }

            StorageAssistant.save(target, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.DOES_NOT_TEMP_INHERIT.send(sender, target.getFormattedDisplayName(), groupName, context);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
