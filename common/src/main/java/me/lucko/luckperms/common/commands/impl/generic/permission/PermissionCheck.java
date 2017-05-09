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

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

import static me.lucko.luckperms.common.commands.abstraction.SubCommand.getPermissionTabComplete;

public class PermissionCheck extends SharedSubCommand {
    public PermissionCheck() {
        super("check", "Checks to see if the object has a certain permission node", Permission.USER_PERM_CHECK,
                Permission.GROUP_PERM_CHECK, Predicates.is(0),
                Arg.list(
                        Arg.create("node", true, "the permission node to check for"),
                        Arg.create("context...", false, "the contexts to check in")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String node = ArgumentUtils.handleString(0, args);
        MutableContextSet context = ArgumentUtils.handleContext(1, args, plugin);

        Tristate result = holder.hasPermission(NodeFactory.newBuilder(node).withExtraContext(context).build());
        String s = Util.formatTristate(result);

        Message.CHECK_PERMISSION.send(sender, holder.getFriendlyName(), node, s, Util.contextSetToString(context));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getPermissionTabComplete(args, plugin.getPermissionVault());
    }
}
