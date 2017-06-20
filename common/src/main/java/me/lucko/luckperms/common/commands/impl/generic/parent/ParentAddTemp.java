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
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getGroupTabComplete;

public class ParentAddTemp extends SharedSubCommand {
    public ParentAddTemp(LocaleManager locale) {
        super(CommandSpec.PARENT_ADD_TEMP.spec(locale), "addtemp", Permission.USER_PARENT_ADDTEMP, Permission.GROUP_PARENT_ADDTEMP, Predicates.inRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String groupName = ArgumentUtils.handleName(0, args);
        long duration = ArgumentUtils.handleDuration(1, args);
        MutableContextSet context = ArgumentUtils.handleContext(2, args, plugin);
        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        if (!plugin.getStorage().loadGroup(groupName).join()) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().getIfLoaded(groupName);
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (group.getName().equalsIgnoreCase(holder.getObjectName())) {
            Message.ALREADY_TEMP_INHERITS.send(sender, holder.getFriendlyName(), group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }

        Map.Entry<DataMutateResult, Node> ret = holder.setPermission(NodeFactory.newBuilder("group." + group.getName()).setExpiry(duration).withExtraContext(context).build(), modifier);

        if (ret.getKey().asBoolean()) {
            duration = ret.getValue().getExpiryUnixTime();
            Message.SET_TEMP_INHERIT_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(), DateUtil.formatDateDiff(duration), Util.contextSetToString(context));

            LogEntry.build().actor(sender).acted(holder)
                    .action("parent addtemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.ALREADY_TEMP_INHERITS.send(sender, holder.getFriendlyName(), group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
