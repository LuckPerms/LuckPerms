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

package me.lucko.luckperms.common.commands.generic.meta;

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
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;

import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeType;

import java.util.List;

public class MetaClear extends GenericChildCommand {
    public MetaClear() {
        super(CommandSpec.META_CLEAR, "clear", CommandPermission.USER_META_CLEAR, CommandPermission.GROUP_META_CLEAR, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        NodeType<?> type = null;
        if (!args.isEmpty()) {
            String typeId = args.get(0).toLowerCase();
            if (typeId.equals("any") || typeId.equals("all") || typeId.equals("*")) {
                type = NodeType.META_OR_CHAT_META;
            }
            if (typeId.equals("chat") || typeId.equals("chatmeta")) {
                type = NodeType.CHAT_META;
            }
            if (typeId.equals("meta")) {
                type = NodeType.META;
            }
            if (typeId.equals("prefix") || typeId.equals("prefixes")) {
                type = NodeType.PREFIX;
            }
            if (typeId.equals("suffix") || typeId.equals("suffixes")) {
                type = NodeType.SUFFIX;
            }

            if (type != null) {
                args.remove(0);
            }
        }

        if (type == null) {
            type = NodeType.META_OR_CHAT_META;
        }

        int before = target.normalData().size();

        MutableContextSet context = args.getContextOrDefault(0, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (context.isEmpty()) {
            target.removeIf(DataType.NORMAL, null, type::matches, false);
        } else {
            target.removeIf(DataType.NORMAL, context, type::matches, false);
        }

        int changed = before - target.normalData().size();
        Message.META_CLEAR_SUCCESS.send(sender, target.getFormattedDisplayName(), type.name().toLowerCase(), context, changed);

        LoggedAction.build().source(sender).target(target)
                .description("meta", "clear", context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(0, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
