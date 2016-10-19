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

package me.lucko.luckperms.common.commands.generic.parent;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.Predicate;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.commands.generic.SecondarySubCommand;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.List;

import static me.lucko.luckperms.common.commands.SubCommand.getGroupTabComplete;

public class ParentRemove extends SecondarySubCommand {
    public ParentRemove() {
        super("remove", "Removes a previously set inheritance rule", Permission.USER_PARENT_REMOVE, Permission.GROUP_PARENT_REMOVE,
                Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("group", true, "the group to remove"),
                        Arg.create("server", false, "the server to remove the group on"),
                        Arg.create("world", false, "the world to remove the group on")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }

        if (holder instanceof User) {
            User user = (User) holder;
            if ((args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("global")))
                    && user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
                Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
                return CommandResult.STATE_ERROR;
            }
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 2) {
                    holder.unsetPermission("group." + groupName, server);
                    Message.UNSET_INHERIT_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), groupName, server);
                    LogEntry.build().actor(sender).acted(holder)
                            .action("parent remove " + groupName + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(2).toLowerCase();
                    holder.unsetPermission("group." + groupName, server, world);
                    Message.UNSET_INHERIT_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), groupName, server, world);
                    LogEntry.build().actor(sender).acted(holder)
                            .action("parent remove " + groupName + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                holder.unsetPermission("group." + groupName);
                Message.UNSET_INHERIT_SUCCESS.send(sender, holder.getFriendlyName(), groupName);
                LogEntry.build().actor(sender).acted(holder)
                        .action("parent remove " + groupName)
                        .build().submit(plugin, sender);
            }

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_INHERIT.send(sender, holder.getFriendlyName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
