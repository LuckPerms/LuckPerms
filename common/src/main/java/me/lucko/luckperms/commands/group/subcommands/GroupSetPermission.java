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
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupSetPermission extends SubCommand<Group> {
    public GroupSetPermission() {
        super("set", "Sets a permission for a group", "/%s group <group> set <node> <true|false> [server] [world]",
                Permission.GROUP_SETPERMISSION, Predicate.notInRange(2, 4));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (node.contains("/") || node.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.GROUP_USE_INHERIT.send(sender);
            return;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender, label);
            return;
        }

        boolean b = Boolean.parseBoolean(bool);

        try {
            if (args.size() >= 3) {
                final String server = args.get(2).toLowerCase();
                if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                if (args.size() == 3) {
                    group.setPermission(node, b, server);
                    Message.SETPERMISSION_SERVER_SUCCESS.send(sender, node, bool, group.getName(), server);
                } else {
                    final String world = args.get(3).toLowerCase();
                    group.setPermission(node, b, server, world);
                    Message.SETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, bool, group.getName(), server, world);
                }

            } else {
                group.setPermission(node, b);
                Message.SETPERMISSION_SUCCESS.send(sender, node, bool, group.getName());
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HASPERMISSION.send(sender, group.getName());
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getBoolTabComplete(args);
    }
}
