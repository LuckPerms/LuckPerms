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

package me.lucko.luckperms.common.commands.impl.generic.permission;

import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Node;
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
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getBoolTabComplete;
import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getPermissionTabComplete;

public class PermissionSetTemp extends SharedSubCommand {
    public PermissionSetTemp() {
        super("settemp", "Sets a permission for the object temporarily", Permission.USER_PERM_SETTEMP,
                Permission.GROUP_PERM_SETTEMP, Predicates.inRange(0, 1),
                Arg.list(
                        Arg.create("node", true, "the permission node to set"),
                        Arg.create("true|false", false, "the value of the node"),
                        Arg.create("duration", true, "the duration until the permission node expires"),
                        Arg.create("context...", false, "the contexts to add the permission in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        boolean b = ArgumentUtils.handleBoolean(1, args);
        String node = b ? ArgumentUtils.handleNode(0, args) : ArgumentUtils.handleString(0, args);
        long duration = ArgumentUtils.handleDuration(2, args);
        MutableContextSet context = ArgumentUtils.handleContext(3, args);

        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);
        Map.Entry<DataMutateResult, Node> result = holder.setPermission(NodeFactory.newBuilder(node).setValue(b).withExtraContext(context).setExpiry(duration).build(), modifier);

        if (result.getKey().asBoolean()) {
            duration = result.getValue().getExpiryUnixTime();
            Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, b, holder.getFriendlyName(), DateUtil.formatDateDiff(duration), Util.contextSetToString(context));

            LogEntry.build().actor(sender).acted(holder)
                    .action("permission settemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        List<String> ret = getBoolTabComplete(args);
        if (!ret.isEmpty()) {
            return ret;
        }
        return getPermissionTabComplete(args, plugin.getPermissionVault());
    }
}
