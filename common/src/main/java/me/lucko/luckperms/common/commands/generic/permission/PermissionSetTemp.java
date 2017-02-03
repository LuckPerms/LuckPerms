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

import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.generic.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.ContextHelper;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeBuilder;
import me.lucko.luckperms.common.core.TemporaryModifier;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;
import java.util.stream.Collectors;

import static me.lucko.luckperms.common.commands.SubCommand.getBoolTabComplete;
import static me.lucko.luckperms.common.commands.SubCommand.getPermissionTabComplete;

public class PermissionSetTemp extends SharedSubCommand {
    public PermissionSetTemp() {
        super("settemp", "Sets a permission for the object temporarily", Permission.USER_PERM_SETTEMP,
                Permission.GROUP_PERM_SETTEMP, Predicates.notInRange(2, 5),
                Arg.list(
                        Arg.create("node", true, "the permission node to set"),
                        Arg.create("true|false", false, "the value of the node"),
                        Arg.create("duration", true, "the duration until the permission node expires"),
                        Arg.create("server", false, "the server to add the permission node on"),
                        Arg.create("world", false, "the world to add the permission node on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        String node = ArgumentUtils.handleNode(0, args);
        boolean b = ArgumentUtils.handleBoolean(1, args);

        long duration = ArgumentUtils.handleDuration(2, args);

        String server = ArgumentUtils.handleServer(3, args);
        String world = ArgumentUtils.handleWorld(4, args);

        TemporaryModifier modifier = plugin.getConfiguration().get(ConfigKeys.TEMPORARY_ADD_BEHAVIOUR);

        try {
            switch (ContextHelper.determine(server, world)) {
                case NONE:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(b).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, b, holder.getFriendlyName(),
                            DateUtil.formatDateDiff(duration)
                    );
                    break;
                case SERVER:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(b).setServer(server).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SETPERMISSION_TEMP_SERVER_SUCCESS.send(sender, node, b, holder.getFriendlyName(), server,
                            DateUtil.formatDateDiff(duration)
                    );
                    break;
                case SERVER_AND_WORLD:
                    duration = holder.setPermission(new NodeBuilder(node).setValue(b).setServer(server).setWorld(world).setExpiry(duration).build(), modifier).getExpiryUnixTime();
                    Message.SETPERMISSION_TEMP_SERVER_WORLD_SUCCESS.send(sender, node, b, holder.getFriendlyName(),
                            server, world, DateUtil.formatDateDiff(duration)
                    );
                    break;
            }

            LogEntry.build().actor(sender).acted(holder)
                    .action("permission settemp " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;

        } catch (ObjectAlreadyHasException e) {
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
        return getPermissionTabComplete(args, plugin.getPermissionCache());
    }
}
