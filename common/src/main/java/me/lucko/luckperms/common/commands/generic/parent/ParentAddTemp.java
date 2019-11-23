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
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.DurationFormatter;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;

import java.util.List;

public class ParentAddTemp extends SharedSubCommand {
    public ParentAddTemp(LocaleManager locale) {
        super(CommandSpec.PARENT_ADD_TEMP.localize(locale), "addtemp", CommandPermission.USER_PARENT_ADD_TEMP, CommandPermission.GROUP_PARENT_ADD_TEMP, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        String groupName = ArgumentParser.parseName(0, args);
        long duration = ArgumentParser.parseDuration(1, args);
        TemporaryNodeMergeStrategy modifier = ArgumentParser.parseTemporaryModifier(2, args).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = ArgumentParser.parseContext(2, args, plugin);

        Group group = StorageAssistant.loadGroup(groupName, sender, plugin, false);
        if (group == null) {
            return CommandResult.INVALID_ARGS;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, holder, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, group, context) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, group.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (group.getName().equalsIgnoreCase(holder.getObjectName())) {
            Message.ALREADY_TEMP_INHERITS.send(sender, holder.getFormattedDisplayName(), group.getFormattedDisplayName(), MessageUtils.contextSetToString(plugin.getLocaleManager(), context));
            return CommandResult.STATE_ERROR;
        }

        DataMutateResult.WithMergedNode ret = holder.setNode(DataType.NORMAL, Inheritance.builder(group.getName()).expiry(duration).withContext(context).build(), modifier);

        if (ret.getResult().wasSuccessful()) {
            duration = ret.getMergedNode().getExpiry().getEpochSecond();
            Message.SET_TEMP_INHERIT_SUCCESS.send(sender, holder.getFormattedDisplayName(), group.getFormattedDisplayName(), DurationFormatter.LONG.formatDateDiff(duration), MessageUtils.contextSetToString(plugin.getLocaleManager(), context));

            LoggedAction.build().source(sender).target(holder)
                    .description("parent", "addtemp", group.getName(), duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.ALREADY_TEMP_INHERITS.send(sender, holder.getFormattedDisplayName(), group.getFormattedDisplayName(), MessageUtils.contextSetToString(plugin.getLocaleManager(), context));
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
