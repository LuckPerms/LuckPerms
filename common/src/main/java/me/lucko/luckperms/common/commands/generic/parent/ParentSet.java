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
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.utils.ArgumentChecker;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.util.List;

import static me.lucko.luckperms.common.commands.SubCommand.getGroupTabComplete;

public class ParentSet extends SecondarySubCommand {
    public ParentSet() {
        super("set", "Removes all other groups the object inherits already and adds them to the one given",
                Permission.USER_PARENT_SET, Permission.GROUP_PARENT_SET, Predicate.notInRange(1, 3),
                Arg.list(
                        Arg.create("group", true, "the group to set to"),
                        Arg.create("server", false, "the server to set the group on"),
                        Arg.create("world", false, "the world to set the group on")
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

        if (!plugin.getDatastore().loadGroup(groupName).getUnchecked()) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.INVALID_ARGS;
        }

        Group group = plugin.getGroupManager().get(groupName);
        if (group == null) {
            Message.GROUP_DOES_NOT_EXIST.send(sender);
            return CommandResult.LOADING_ERROR;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 2) {

                    holder.clearParents(server, null);
                    holder.setInheritGroup(group, server);

                    Message.SET_PARENT_SERVER_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(), server);
                    LogEntry.build().actor(sender).acted(holder)
                            .action("parent set " + group.getName() + " " + server)
                            .build().submit(plugin, sender);
                } else {
                    final String world = args.get(2).toLowerCase();

                    holder.clearParents(server, world);
                    holder.setInheritGroup(group, server, world);

                    Message.SET_PARENT_SERVER_WORLD_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName(), server, world);
                    LogEntry.build().actor(sender).acted(holder)
                            .action("parent set " + group.getName() + " " + server + " " + world)
                            .build().submit(plugin, sender);
                }

            } else {
                holder.clearParents(null, null);
                holder.setInheritGroup(group);
                if (holder instanceof User) {
                    ((User) holder).setPrimaryGroup(group.getName());
                }

                Message.SET_PARENT_SUCCESS.send(sender, holder.getFriendlyName(), group.getDisplayName());
                LogEntry.build().actor(sender).acted(holder)
                        .action("parent set " + group.getName())
                        .build().submit(plugin, sender);
            }

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_INHERITS.send(sender, holder.getFriendlyName(), group.getDisplayName());
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
