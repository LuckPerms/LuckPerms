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

package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.data.LogEntryBuilder;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;

import java.util.List;

public class GroupUnsetTempInherit extends SubCommand<Group> {
    public GroupUnsetTempInherit() {
        super("unsettempinherit", "Unsets another group for this group to inherit permissions from",
                "/%s group <group> unsettempinherit <group> [server] [world]", Permission.GROUP_UNSET_TEMP_INHERIT,
                Predicate.notInRange(1, 3));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (ArgumentChecker.checkNode(groupName)) {
            sendUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return CommandResult.INVALID_ARGS;
                }

                if (args.size() == 2) {
                    group.unsetPermission("group." + groupName, server, true);
                    Message.GROUP_UNSET_TEMP_INHERIT_SERVER_SUCCESS.send(sender, group.getName(), groupName, server);
                    LogEntryBuilder.get().actor(sender).acted(group)
                            .action("unsettempinherit " + groupName + " " + server)
                            .submit(plugin);
                } else {
                    final String world = args.get(2).toLowerCase();
                    group.unsetPermission("group." + groupName, server, world, true);
                    Message.GROUP_UNSET_TEMP_INHERIT_SERVER_WORLD_SUCCESS.send(sender, group.getName(), groupName, server, world);
                    LogEntryBuilder.get().actor(sender).acted(group)
                            .action("unsettempinherit " + groupName + " " + server + " " + world)
                            .submit(plugin);
                }

            } else {
                group.unsetPermission("group." + groupName, true);
                Message.GROUP_UNSET_TEMP_INHERIT_SUCCESS.send(sender, group.getName(), groupName);
                LogEntryBuilder.get().actor(sender).acted(group)
                        .action("unsettempinherit " + groupName)
                        .submit(plugin);
            }

            save(group, sender, plugin);
            return CommandResult.SUCCESS;
        } catch (ObjectLacksException e) {
            Message.GROUP_DOES_NOT_TEMP_INHERIT.send(sender, group.getName(), groupName);
            return CommandResult.STATE_ERROR;
        }
    }

    @Override
    public List<String> onTabComplete(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        return getGroupTabComplete(args, plugin);
    }
}
