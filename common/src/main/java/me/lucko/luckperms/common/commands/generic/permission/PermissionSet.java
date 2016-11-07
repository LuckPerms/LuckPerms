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
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.SubCommand.getBoolTabComplete;

public class PermissionSet extends SecondarySubCommand {
    public PermissionSet() {
        super("set", "Sets a permission for the object", Permission.USER_PERM_SET, Permission.GROUP_PERM_SET,
                Predicates.notInRange(2, 4),
                Arg.list(
                        Arg.create("node", true, "the permission node to set"),
                        Arg.create("true|false", true, "the value of the node"),
                        Arg.create("server", false, "the server to add the permission node on"),
                        Arg.create("world", false, "the world to add the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) throws CommandException {
        String node = ArgumentUtils.handleNode(0, args);
        boolean b = ArgumentUtils.handleBoolean(1, args);

        String server = ArgumentUtils.handleServer(2, args);
        String world = ArgumentUtils.handleWorld(3, args);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    holder.setPermission(node, b);
                    Message.SETPERMISSION_SUCCESS.send(sender, node, b, holder.getFriendlyName());
                    break;
                case SERVER:
                    holder.setPermission(node, b, server);
                    Message.SETPERMISSION_SERVER_SUCCESS.send(sender, node, b, holder.getFriendlyName(), server);
                    break;
                case SERVER_AND_WORLD:
                    holder.setPermission(node, b, server, world);
                    Message.SETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, b, holder.getFriendlyName(), server, world);
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("permission set " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HASPERMISSION.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getBoolTabComplete(args);
    }
}
