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
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.List;

public class PermissionSetTemp extends SharedSubCommand {
    public PermissionSetTemp(LocaleManager locale) {
        super(CommandSpec.PERMISSION_SETTEMP.localize(locale), "settemp", CommandPermission.USER_PERM_SET_TEMP, CommandPermission.GROUP_PERM_SET_TEMP, Predicates.inRange(0, 2));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        String node = ArgumentParser.parseString(0, args);
        boolean value = ArgumentParser.parseBoolean(1, args);
        long duration = ArgumentParser.parseDuration(2, args);
        TemporaryNodeMergeStrategy modifier = ArgumentParser.parseTemporaryModifier(3, args).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = ArgumentParser.parseContext(3, args, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, holder, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, node)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        Node builtNode = NodeBuilders.determineMostApplicable(node).value(value).withContext(context).expiry(duration).build();

        if (builtNode instanceof InheritanceNode) {
            if (ArgumentPermissions.checkGroup(plugin, sender, ((InheritanceNode) builtNode).getGroupName(), context)) {
                Message.COMMAND_NO_PERMISSION.send(sender);
                return CommandResult.NO_PERMISSION;
            }
        }

        DataMutateResult.WithMergedNode result = holder.setNode(DataType.NORMAL, builtNode, modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiry().getEpochSecond();
            Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, value, holder.getFormattedDisplayName(), DurationFormatter.LONG.formatDateDiff(duration), MessageUtils.contextSetToString(plugin.getLocaleManager(), context));

            LoggedAction.build().source(sender).target(holder)
                    .description("permission", "settemp", node, value, duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, holder.getFormattedDisplayName(), node, MessageUtils.contextSetToString(plugin.getLocaleManager(), context));
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return TabCompleter.create()
                .at(0, TabCompletions.permissions(plugin))
                .at(1, TabCompletions.booleans())
                .from(3, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
