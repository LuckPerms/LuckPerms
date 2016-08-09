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
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.ArgumentChecker;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupUnsetTempPermission extends SubCommand<Group> {
    public GroupUnsetTempPermission() {
        super("unsettemp", "Unsets a temporary permission for a group",
                "/%s group <group> unsettemp <node> [server] [world]", Permission.GROUP_UNSET_TEMP_PERMISSION,
                Predicate.notInRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0);

        if (!ArgumentChecker.checkNode(node)) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.GROUP_USE_UNINHERIT.send(sender);
            return;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (!ArgumentChecker.checkServer(server)) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                if (args.size() == 2) {
                    group.unsetPermission(node, server);
                    Message.UNSET_TEMP_PERMISSION_SERVER_SUCCESS.send(sender, node, group.getName(), server);
                } else {
                    final String world = args.get(2).toLowerCase();
                    group.unsetPermission(node, server, world);
                    Message.UNSET_TEMP_PERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, group.getName(), server, world);
                }

            } else {
                group.unsetPermission(node, true);
                Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, group.getName());
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, group.getName());
        }
    }
}
