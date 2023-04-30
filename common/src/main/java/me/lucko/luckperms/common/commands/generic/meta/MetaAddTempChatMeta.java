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
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.node.ChatMetaType;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public class MetaAddTempChatMeta extends GenericChildCommand {

    public static MetaAddTempChatMeta forPrefix() {
        return new MetaAddTempChatMeta(
                ChatMetaType.PREFIX,
                CommandSpec.META_ADDTEMP_PREFIX,
                "addtempprefix",
                CommandPermission.USER_META_ADD_TEMP_PREFIX,
                CommandPermission.GROUP_META_ADD_TEMP_PREFIX
        );
    }

    public static MetaAddTempChatMeta forSuffix() {
        return new MetaAddTempChatMeta(
                ChatMetaType.SUFFIX,
                CommandSpec.META_ADDTEMP_SUFFIX,
                "addtempsuffix",
                CommandPermission.USER_META_ADD_TEMP_SUFFIX,
                CommandPermission.GROUP_META_ADD_TEMP_SUFFIX
        );
    }

    private final ChatMetaType type;

    private MetaAddTempChatMeta(ChatMetaType type, CommandSpec spec, String name, CommandPermission userPermission, CommandPermission groupPermission) {
        super(spec, name, userPermission, groupPermission, Predicates.inRange(0, 2));
        this.type = type;
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int priority = args.getPriority(0);
        String meta = args.get(1);
        Duration duration = args.getDuration(2);
        TemporaryNodeMergeStrategy modifier = args.getTemporaryModifierAndRemove(3).orElseGet(() -> plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR));
        MutableContextSet context = args.getContextOrDefault(3, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, permission, context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        DataMutateResult.WithMergedNode result = target.setNode(DataType.NORMAL, this.type.builder(meta, priority).expiry(duration).withContext(context).build(), modifier);

        if (result.getResult().wasSuccessful()) {
            duration = result.getMergedNode().getExpiryDuration();

            Message.ADD_TEMP_CHATMETA_SUCCESS.send(sender, target, this.type, meta, priority, duration, context);

            LoggedAction.build().source(sender).target(target)
                    .description("meta" , "addtemp" + this.type.name().toLowerCase(Locale.ROOT), priority, meta, duration, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
        } else {
            Message.ALREADY_HAS_TEMP_CHAT_META.send(sender, target, this.type, meta, priority, context);
        }
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(3, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
