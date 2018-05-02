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

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.utils.ArgumentParser;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;

import java.util.List;

public class MetaRemoveChatMeta extends SharedSubCommand {
    private final ChatMetaType type;

    public MetaRemoveChatMeta(LocaleManager locale, ChatMetaType type) {
        super(
                type == ChatMetaType.PREFIX ? CommandSpec.META_REMOVEPREFIX.localize(locale) : CommandSpec.META_REMOVESUFFIX.localize(locale),
                "remove" + type.name().toLowerCase(),
                type == ChatMetaType.PREFIX ? CommandPermission.USER_META_REMOVE_PREFIX : CommandPermission.USER_META_REMOVE_SUFFIX,
                type == ChatMetaType.PREFIX ? CommandPermission.GROUP_META_REMOVE_PREFIX : CommandPermission.GROUP_META_REMOVE_SUFFIX,
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
        MutableContextSet context = ArgumentParser.parseContext(2, args, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        // Handle bulk removal
        if (meta.equalsIgnoreCase("null") || meta.equals("*")) {
            holder.removeIf(n ->
                    this.type.matches(n) &&
                            this.type.getEntry(n).getKey() == priority &&
                    !n.isTemporary() &&
                    n.getFullContexts().makeImmutable().equals(context.makeImmutable())
            );
            Message.BULK_REMOVE_CHATMETA_SUCCESS.send(sender, holder.getFriendlyName(), this.type.name().toLowerCase(), priority, MessageUtils.contextSetToString(context));

            ExtendedLogEntry.build().actor(sender).acted(holder)
                    .action("meta" , "remove" + this.type.name().toLowerCase(), priority, "*", context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        }

        DataMutateResult result = holder.unsetPermission(NodeFactory.buildChatMetaNode(this.type, priority, meta).withExtraContext(context).build());

        if (result.asBoolean()) {
            TextComponent.Builder builder = TextUtils.fromLegacy(Message.REMOVE_CHATMETA_SUCCESS.asString(plugin.getLocaleManager(), holder.getFriendlyName(), this.type.name().toLowerCase(), meta, priority, MessageUtils.contextSetToString(context)), CommandManager.SECTION_CHAR).toBuilder();
            HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(
                    "¥3Raw " + this.type.name().toLowerCase() + ": ¥r" + meta,
                    '¥'
            ));
            builder.applyDeep(c -> c.hoverEvent(event));
            sender.sendMessage(builder.build());

            ExtendedLogEntry.build().actor(sender).acted(holder)
                    .action("meta" , "remove" + this.type.name().toLowerCase(), priority, meta, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.DOES_NOT_HAVE_CHAT_META.send(sender, holder.getFriendlyName(), this.type.name().toLowerCase(), meta, priority, MessageUtils.contextSetToString(context));
            return CommandResult.STATE_ERROR;
        }
    }
}
