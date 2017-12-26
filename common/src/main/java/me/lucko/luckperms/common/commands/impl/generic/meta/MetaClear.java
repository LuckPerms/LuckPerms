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

package me.lucko.luckperms.common.commands.impl.generic.meta;

import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.MetaType;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

public class MetaClear extends SharedSubCommand {
    public MetaClear(LocaleManager locale) {
        super(CommandSpec.META_CLEAR.spec(locale), "clear", CommandPermission.USER_META_CLEAR, CommandPermission.GROUP_META_CLEAR, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        MetaType type = null;
        if (args.size() > 0) {
            String typeId = args.get(0).toLowerCase();
            if (typeId.equals("any") || typeId.equals("all") || typeId.equals("*")) {
                type = MetaType.ANY;
            }
            if (typeId.equals("chat") || typeId.equals("chatmeta")) {
                type = MetaType.CHAT;
            }
            if (typeId.equals(NodeFactory.META_KEY)) {
                type = MetaType.META;
            }
            if (typeId.equals(NodeFactory.PREFIX_KEY) || typeId.equals("prefixes")) {
                type = MetaType.PREFIX;
            }
            if (typeId.equals(NodeFactory.SUFFIX_KEY) || typeId.equals("suffixes")) {
                type = MetaType.SUFFIX;
            }

            if (type != null) {
                args.remove(0);
            }
        }

        if (type == null) {
            type = MetaType.ANY;
        }

        int before = holder.getEnduringNodes().size();

        MutableContextSet context = ArgumentUtils.handleContext(0, args, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        if (context.isEmpty()) {
            holder.clearMeta(type);
        } else {
            holder.clearMeta(type, context);
        }

        int changed = before - holder.getEnduringNodes().size();
        if (changed == 1) {
            Message.META_CLEAR_SUCCESS_SINGULAR.send(sender, holder.getFriendlyName(), type.name().toLowerCase(), CommandUtils.contextSetToString(context), changed);
        } else {
            Message.META_CLEAR_SUCCESS.send(sender, holder.getFriendlyName(), type.name().toLowerCase(), CommandUtils.contextSetToString(context), changed);
        }

        ExtendedLogEntry.build().actor(sender).acted(holder)
                .action("meta", "clear", context)
                .build().submit(plugin, sender);

        save(holder, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
