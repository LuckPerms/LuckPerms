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

package me.lucko.luckperms.common.commands.group;

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
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.DisplayNameNode;

import java.util.List;

public class GroupSetDisplayName extends ChildCommand<Group> {
    public GroupSetDisplayName() {
        super(CommandSpec.GROUP_SET_DISPLAY_NAME, "setdisplayname", CommandPermission.GROUP_SET_DISPLAY_NAME, Predicates.is(0));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group target, ArgumentList args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), target)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String name = args.get(0);
        ImmutableContextSet context = args.getContextOrDefault(1, plugin).immutableCopy();

        if (name.isEmpty()) {
            Message.INVALID_DISPLAY_NAME_EMPTY.send(sender);
            return;
        }

        String previousName = target.normalData().nodesInContext(context).stream()
                .filter(NodeType.DISPLAY_NAME::matches)
                .map(NodeType.DISPLAY_NAME::cast)
                .findFirst()
                .map(DisplayNameNode::getDisplayName)
                .orElse(null);

        if (previousName == null && name.equals(target.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_DOESNT_HAVE.send(sender, target.getName());
            return;
        }

        if (name.equals(previousName)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_HAS.send(sender, target.getName(), name);
            return;
        }

        Group existing = plugin.getGroupManager().getByDisplayName(name);
        if (existing != null && !target.equals(existing)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_IN_USE.send(sender, name, existing.getName());
            return;
        }

        target.removeIf(DataType.NORMAL, context, NodeType.DISPLAY_NAME::matches, false);

        if (name.equals(target.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_REMOVED.send(sender, target.getName(), context);

            LoggedAction.build().source(sender).target(target)
                    .description("setdisplayname", name, context)
                    .build().submit(plugin, sender);

            StorageAssistant.save(target, sender, plugin);
            return;
        }

        target.setNode(DataType.NORMAL, DisplayName.builder(name).withContext(context).build(), true);

        Message.GROUP_SET_DISPLAY_NAME.send(sender, name, target.getName(), context);

        LoggedAction.build().source(sender).target(target)
                .description("setdisplayname", name, context)
                .build().submit(plugin, sender);

        StorageAssistant.save(target, sender, plugin);
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .from(1, TabCompletions.contexts(plugin))
                .complete(args);
    }
}
