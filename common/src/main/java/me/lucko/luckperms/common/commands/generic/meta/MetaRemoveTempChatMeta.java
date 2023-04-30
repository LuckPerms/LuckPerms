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
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.ChatMetaType;

import java.util.List;
import java.util.Locale;

public class MetaRemoveTempChatMeta extends GenericChildCommand {

    public static MetaRemoveTempChatMeta forPrefix() {
        return new MetaRemoveTempChatMeta(
                ChatMetaType.PREFIX,
                CommandSpec.META_REMOVETEMP_PREFIX,
                "removetempprefix",
                CommandPermission.USER_META_REMOVE_TEMP_PREFIX,
                CommandPermission.GROUP_META_REMOVE_TEMP_PREFIX
        );
    }

    public static MetaRemoveTempChatMeta forSuffix() {
        return new MetaRemoveTempChatMeta(
                ChatMetaType.SUFFIX,
                CommandSpec.META_REMOVETEMP_SUFFIX,
                "removetempsuffix",
                CommandPermission.USER_META_REMOVE_TEMP_SUFFIX,
                CommandPermission.GROUP_META_REMOVE_TEMP_SUFFIX
        );
    }

    private final ChatMetaType type;

    private MetaRemoveTempChatMeta(ChatMetaType type, CommandSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission) {
        super(spec, name, userPermission, groupPermission, Predicates.is(0));
        this.type = type;
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int priority = args.getPriority(0);
        String meta = args.getOrDefault(1, "null");
        ImmutableContextSet context = args.getContextOrDefault(2, plugin).immutableCopy();

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        // Handle bulk removal
        if (meta.equalsIgnoreCase("null") || meta.equals("*")) {
            target.removeIf(DataType.NORMAL, context, this.type.nodeType().predicate(n -> n.getPriority() == priority && n.hasExpiry()), false);
            Message.BULK_REMOVE_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, priority, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(Locale.ROOT), priority, "*", context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
            return;
        }

        DataMutateResult result = target.unsetNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(10L).withContext(context).build());

        if (result.wasSuccessful()) {
            Message.REMOVE_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, meta, priority, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "removetemp" + this.type.name().toLowerCase(Locale.ROOT), priority, meta, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.DOES_NOT_HAVE_TEMP_CHAT_META.send(sender, target, this.type, meta, priority, context);
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(2, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
