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

package me.lucko.luckperms.common.commands.generic.other;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.data.DataType;

import java.util.List;

public class HolderClear<T extends PermissionHolder> extends ChildCommand<T> {
    public HolderClear(HolderType type) {
        super(CommandSpec.HOLDER_CLEAR, "clear", type == HolderType.USER ? CommandPermission.USER_CLEAR : CommandPermission.GROUP_CLEAR, Predicates.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, T target, ArgumentList args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        int before = target.normalData().size();

        MutableContextSet context = args.getContextOrDefault(0, plugin);

        if (ArgumentPermissions.checkContext(plugin, sender, getPermission().get(), context) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, context)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (context.isEmpty()) {
            target.clearNodes(DataType.NORMAL, null, true);
        } else {
            target.clearNodes(DataType.NORMAL, context, true);
        }

        int changed = before - target.normalData().size();
        Message.CLEAR_SUCCESS.send(sender, target, context, changed);

        LoggedAction.build().source(sender).target(target)
                .description("clear", context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(0, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
