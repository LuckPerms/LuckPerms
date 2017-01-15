/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.commands.generic.permission;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.generic.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.InheritanceInfo;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;

import static me.lucko.luckperms.common.commands.SubCommand.getPermissionTabComplete;

public class PermissionCheckInherits extends SharedSubCommand {
    public PermissionCheckInherits() {
        super("checkinherits", "Checks to see if the object inherits a certain permission node",
                Permission.USER_PERM_CHECK_INHERITS, Permission.GROUP_PERM_CHECK_INHERITS, Predicates.notInRange(1, 3),
                Arg.list(
                        Arg.create("node", true, "the permission node to check for"),
                        Arg.create("server", false, "the server to check on"),
                        Arg.create("world", false, "the world to check on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String node = ArgumentUtils.handleString(0, args);
        String server = ArgumentUtils.handleServer(1, args);
        String world = ArgumentUtils.handleWorld(2, args);

        InheritanceInfo result = null;
        switch (ContextHelper.determine(server, world)) {
            case NONE:
                result = holder.inheritsPermissionInfo(new NodeBuilder(node).build());
                break;
            case SERVER:
                result = holder.inheritsPermissionInfo(new NodeBuilder(node).setServer(server).build());
                break;
            case SERVER_AND_WORLD:
                result = holder.inheritsPermissionInfo(new NodeBuilder(node).setServer(server).setWorld(world).build());
                break;
        }

        String location = result.getLocation().orElse(null);
        if (location != null && location.equalsIgnoreCase(holder.getObjectName())) {
            location = "self";
        }

        Util.sendPluginMessage(sender, "&b" + node + ": " + Util.formatTristate(result.getResult()) +
                (location != null ? " &7(inherited from &a" + location + "&7)" : ""));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getPermissionTabComplete(args, plugin.getPermissionCache());
    }
}
