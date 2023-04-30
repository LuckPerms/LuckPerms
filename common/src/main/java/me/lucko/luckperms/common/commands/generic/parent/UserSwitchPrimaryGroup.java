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
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.access.ArgumentPermissions;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.tabcomplete.TabCompleter;
import me.lucko.luckperms.common.command.tabcomplete.TabCompletions;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.command.utils.StorageAssistant;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeEqualityPredicate;

import java.util.List;
import java.util.Locale;

public class UserSwitchPrimaryGroup extends GenericChildCommand {
    public UserSwitchPrimaryGroup() {
        super(CommandSpec.USER_SWITCHPRIMARYGROUP, "switchprimarygroup", CommandPermission.USER_PARENT_SWITCHPRIMARYGROUP, null, Predicates.not(1));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder target, ArgumentList args, String label, CommandPermission permission) {
        // cast to user
        // although this command is build as a sharedsubcommand,
        // it is only added to the listings for users.
        User user = (User) target;

        if (ArgumentPermissions.checkModifyPerms(plugin, sender, permission, user)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        String opt = plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD);
        if (!opt.equals("stored")) {
            Message.USER_PRIMARYGROUP_WARN_OPTION.send(sender, opt);
        }

        Group group = plugin.getGroupManager().getIfLoaded(args.get(0).toLowerCase(Locale.ROOT));
        if (group == null) {
            Message.DOES_NOT_EXIST.send(sender, args.get(0).toLowerCase(Locale.ROOT));
            return;
        }

        if (ArgumentPermissions.checkContext(plugin, sender, permission, ImmutableContextSetImpl.EMPTY) ||
                ArgumentPermissions.checkGroup(plugin, sender, target, ImmutableContextSetImpl.EMPTY) ||
                ArgumentPermissions.checkGroup(plugin, sender, group, ImmutableContextSetImpl.EMPTY) ||
                ArgumentPermissions.checkArguments(plugin, sender, permission, group.getName())) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return;
        }

        if (user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME).equalsIgnoreCase(group.getName())) {
            Message.USER_PRIMARYGROUP_ERROR_ALREADYHAS.send(sender, user, group);
            return;
        }

        Node node = Inheritance.builder(group.getName()).build();
        if (!user.hasNode(DataType.NORMAL, node, NodeEqualityPredicate.IGNORE_VALUE).asBoolean()) {
            Message.USER_PRIMARYGROUP_ERROR_NOTMEMBER.send(sender, user, group);
            target.setNode(DataType.NORMAL, node, true);
        }

        user.getPrimaryGroup().setStoredValue(group.getName());
        Message.USER_PRIMARYGROUP_SUCCESS.send(sender, user, group);

        LoggedAction.build().source(sender).target(user)
                .description("parent", "switchprimarygroup", group.getName())
                .build().submit(plugin, sender);

        StorageAssistant.save(user, sender, plugin);
    }

    @Override
    public List<String> tabComplete(LuckPermsPlugin plugin, Sender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(0, TabCompletions.groups(plugin))
                .complete(args);
    }
}
