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
import me.lucko.luckperms.common.command.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.ChatMetaType;

import java.util.List;

public class MetaRemoveTempChatMeta extends SharedSubCommand {
    private final ChatMetaType type;

    public MetaRemoveTempChatMeta(LocaleManager locale, ChatMetaType type) {
        super(
                type == ChatMetaType.PREFIX ? CommandSpec.META_REMOVETEMP_PREFIX.localize(locale) : CommandSpec.META_REMOVETEMP_SUFFIX.localize(locale),
                "removetemp" + type.name().toLowerCase(),
                type == ChatMetaType.PREFIX ? CommandPermission.USER_META_REMOVE_TEMP_PREFIX : CommandPermission.USER_META_REMOVE_TEMP_SUFFIX,
                type == ChatMetaType.PREFIX ? CommandPermission.GROUP_META_REMOVE_TEMP_PREFIX : CommandPermission.GROUP_META_REMOVE_TEMP_SUFFIX,
                Predicates.is(0)
        );
        this.type = type;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, holder)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        int priority = ArgumentParser.parsePriority(0, args);
        String meta = ArgumentParser.parseStringOrElse(1, args, "null");
        ImmutableContextSet context = ArgumentParser.parseContext(2, args, plugin).immutableCopy();

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, holder, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        // Handle bulk removal
        if (meta.equalsIgnoreCase("null") || meta.equals("*")) {
            holder.removeIf(DataType.NORMAL, context, this.type.nodeType().predicate(n -> n.getPriority() == priority && n.hasExpiry()), false);
            Message.BULK_REMOVE_TEMP_CHATMETA_SUCCESS.send(sender, holder.getFormattedDisplayName(), this.type.name().toLowerCase(), priority, MessageUtils.contextSetToString(plugin.getLocaleManager(), context));

            LoggedAction.build().source(sender).target(holder)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(), priority, "*", context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        }

        DataMutateResult result = holder.unsetNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(10L).withContext(context).build());

        if (result.wasSuccessful()) {
            TextComponent.Builder builder = Message.REMOVE_TEMP_CHATMETA_SUCCESS.asComponent(plugin.getLocaleManager(), holder.getFormattedDisplayName(), this.type.name().toLowerCase(), meta, priority, MessageUtils.contextSetToString(plugin.getLocaleManager(), context)).toBuilder();
            HoverEvent event = HoverEvent.showText(TextUtils.fromLegacy(
                    "¥3Raw " + this.type.name().toLowerCase() + ": ¥r" + meta,
                    '¥'
            ));
            builder.applyDeep(c -> c.hoverEvent(event));
            sender.sendMessage(builder.build());

            LoggedAction.build().source(sender).target(holder)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(), priority, meta, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.DOES_NOT_HAVE_TEMP_CHAT_META.send(sender, holder.getFormattedDisplayName(), this.type.name().toLowerCase(), meta, priority, MessageUtils.contextSetToString(plugin.getLocaleManager(), context));
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return TabCompleter.create()
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
