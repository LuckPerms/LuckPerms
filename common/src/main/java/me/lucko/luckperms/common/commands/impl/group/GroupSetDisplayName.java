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

package me.lucko.luckperms.common.commands.impl.group;

import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.commands.ArgumentPermissions;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

public class GroupSetDisplayName extends SubCommand<Group> {
    public GroupSetDisplayName(LocaleManager locale) {
        super(CommandSpec.GROUP_SET_DISPLAY_NAME.spec(locale), "setdisplayname", CommandPermission.GROUP_SET_DISPLAY_NAME, Predicates.not(1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) throws CommandException {
        if (ArgumentPermissions.checkModifyPerms(plugin, sender, getPermission().get(), group)) {
            Message.COMMAND_NO_PERMISSION.send(sender);
            return CommandResult.NO_PERMISSION;
        }

        String name = ArgumentUtils.handleString(0, args);
        String previousName = group.getDisplayName().orElse(null);

        if (previousName == null && name.equals(group.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_DOESNT_HAVE.send(sender, group.getName());
            return CommandResult.STATE_ERROR;
        }

        if (name.equals(previousName)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_HAS.send(sender, group.getName(), name);
            return CommandResult.STATE_ERROR;
        }

        Group existing = plugin.getGroupManager().getByDisplayName(name);
        if (existing != null && !group.equals(existing)) {
            Message.GROUP_SET_DISPLAY_NAME_ALREADY_IN_USE.send(sender, name, existing.getName());
            return CommandResult.STATE_ERROR;
        }

        group.removeIf(n -> n.getPermission().startsWith("displayname."));

        if (name.equals(group.getName())) {
            Message.GROUP_SET_DISPLAY_NAME_REMOVED.send(sender, group.getName());

            ExtendedLogEntry.build().actor(sender).acted(group)
                    .action("setdisplayname", name)
                    .build().submit(plugin, sender);

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        }

        group.setPermission(NodeFactory.builder("displayname." + name).build());

        Message.GROUP_SET_DISPLAY_NAME.send(sender, name, group.getName());

        ExtendedLogEntry.build().actor(sender).acted(group)
                .action("setdisplayname", name)
                .build().submit(plugin, sender);

        save(group, sender, plugin);
        return CommandResult.SUCCESS;
    }
}
