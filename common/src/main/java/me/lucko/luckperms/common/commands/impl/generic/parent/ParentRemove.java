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

package me.lucko.luckperms.common.commands.impl.generic.parent;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getGroupTabComplete;

public class ParentRemove extends SharedSubCommand {
    public ParentRemove() {
        super("remove", "Removes a previously set inheritance rule", Permission.USER_PARENT_REMOVE,
                Permission.GROUP_PARENT_REMOVE, Predicates.is(0),
                Arg.list(
                        Arg.create("group", true, "the group to remove"),
                        Arg.create("context...", false, "the contexts to remove the group in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String groupName = ArgumentUtils.handleNameWithSpace(0, args);
        MutableContextSet context = ArgumentUtils.handleContext(1, args, plugin);

        if (holder instanceof User) {
            User user = (User) holder;

            boolean shouldPrevent = (context.isEmpty() || context.has("server", "global")) &&
                    plugin.getConfiguration().get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD).equals("stored") &&
                    user.getPrimaryGroup().getStoredValue().equalsIgnoreCase(groupName);

            if (shouldPrevent) {
                Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
                return CommandResult.STATE_ERROR;
            }
        }

        DataMutateResult result = holder.unsetPermission(NodeFactory.newBuilder("group." + groupName).withExtraContext(context).build());
        if (result.asBoolean()) {
            Message.UNSET_INHERIT_SUCCESS.send(sender, holder.getFriendlyName(), groupName, Util.contextSetToString(context));

            LogEntry.build().actor(sender).acted(holder)
                    .action("parent remove " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.DOES_NOT_INHERIT.send(sender, holder.getFriendlyName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
