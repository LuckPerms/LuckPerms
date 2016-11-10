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
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;
import java.util.stream.Collectors;

public class PermissionUnsetTemp extends SharedSubCommand {
    public PermissionUnsetTemp() {
        super("unsettemp", "Unsets a temporary permission for the object", Permission.USER_PERM_UNSETTEMP,
                Permission.GROUP_PERM_UNSETTEMP, Predicates.notInRange(1, 3),
                Arg.list(
                        Arg.create("node", true, "the permission node to unset"),
                        Arg.create("server", false, "the server to remove the permission node on"),
                        Arg.create("world", false, "the world to remove the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) throws CommandException {
        String node = ArgumentUtils.handleNode(0, args);
        String server = ArgumentUtils.handleServer(1, args);
        String world = ArgumentUtils.handleWorld(2, args);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    holder.unsetPermission(node, true);
                    Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, holder.getFriendlyName());
                    break;
                case SERVER:
                    holder.unsetPermission(node, server, true);
                    Message.UNSET_TEMP_PERMISSION_SERVER_SUCCESS.send(sender, node, holder.getFriendlyName(), server);
                    break;
                case SERVER_AND_WORLD:
                    holder.unsetPermission(node, server, world, true);
                    Message.UNSET_TEMP_PERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, holder.getFriendlyName(), server, world);
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("permission unsettemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, holder.getFriendlyName());
            return CommandResult.STATE_ERROR;
        }
    }
}
