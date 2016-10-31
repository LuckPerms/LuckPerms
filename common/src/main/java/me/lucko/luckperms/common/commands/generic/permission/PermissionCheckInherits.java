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
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.commands.generic.SecondarySubCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.InheritanceInfo;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.utils.ArgumentChecker;

import java.util.List;

public class PermissionCheckInherits extends SecondarySubCommand {
    public PermissionCheckInherits() {
        super("checkinherits", "Checks to see if the object inherits a certain permission node", Permission.USER_PERM_CHECK_INHERITS, Permission.GROUP_PERM_CHECK_INHERITS,
                Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("node", true, "the permission node to check for"),
                        Arg.create("server", false, "the server to check on"),
                        Arg.create("world", false, "the world to check on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        InheritanceInfo result;
        if (args.size() >= 2) {
            if (ArgumentChecker.checkServer(args.get(1))) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return CommandResult.INVALID_ARGS;
            }

            if (args.size() == 2) {
                result = holder.inheritsPermissionInfo(new NodeBuilder(args.get(0)).setServer(args.get(1)).build());
            } else {
                result = holder.inheritsPermissionInfo(new NodeBuilder(args.get(0)).setServer(args.get(1)).setWorld(args.get(2)).build());
            }

        } else {
            result = holder.inheritsPermissionInfo(new NodeBuilder(args.get(0)).build());
        }

        String location = null;
        if (result.getLocation().isPresent()) {
            if (result.getLocation().get().equals(holder.getObjectName())) {
                location = "self";
            } else {
                location = result.getLocation().get();
            }
        }

        Util.sendPluginMessage(sender, "&b" + args.get(0) + ": " + Util.formatTristate(result.getResult()) +
                (result.getLocation().isPresent() ? " &7(inherited from &a" + location + "&7)" : ""));
        return CommandResult.SUCCESS;
    }
}
