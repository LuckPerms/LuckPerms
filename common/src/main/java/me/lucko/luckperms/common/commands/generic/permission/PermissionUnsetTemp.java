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

package me.lucko.luckperms.common.commands.generic.permission;

import me.lucko.luckperms.common.actionlog.LoggedAction;
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
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.time.Duration;
import java.util.List;

public class PermissionUnsetTemp extends GenericChildCommand {
    public PermissionUnsetTemp() {
        super(CommandSpec.PERMISSION_UNSETTEMP, "unsettemp", CommandPermission.USER_PERM_UNSET_TEMP, CommandPermission.GROUP_PERM_UNSET_TEMP, Predicates.is(0));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String node = args.get(0);
        Duration duration = args.getDurationOrDefault(1, null);
        int fromIndex = duration == null ? 1 : 2;
        MutableContextSet context = args.getContextOrDefault(fromIndex, plugin);

        if (node.isEmpty()) {
            Message.INVALID_PERMISSION_EMPTY.send(sender);
            return;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, node)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        Node builtNode = NodeBuilders.determineMostApplicable(node).expiry(10L).withContext(context).build();

        if (builtNode instanceof InheritanceNode) {
            if (ArgumentPermissions.checkGroup(plugin, sender, ((InheritanceNode) builtNode).getGroupName(), context)) {
                Message.COMMAND_NO_PERMISSION.send(sender);
                return;
            }
        }

        DataMutateResult.WithMergedNode result = target.unsetNode(DataType.NORMAL, builtNode, duration);
        if (result.getResult().wasSuccessful()) {
            Node mergedNode = result.getMergedNode();
            //noinspection ConstantConditions
            if (mergedNode != null) {
                Message.UNSET_TEMP_PERMISSION_SUBTRACT_SUCCESS.send(sender, mergedNode.getKey(), mergedNode.getValue(), target, mergedNode.getExpiryDuration(), context, duration);

                LoggedAction.build().source(sender).target(target)
                        .description("permission", "unsettemp", node, duration, context)
                        .build().submit(plugin, sender);
            } else {
                Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, target, context);

                LoggedAction.build().source(sender).target(target)
                        .description("permission", "unsettemp", node, context)
                        .build().submit(plugin, sender);
            }

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, target, node, context);
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
