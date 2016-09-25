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

package me.lucko.luckperms.commands.usersbulkedit.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.users.User;

import java.util.*;

public class BulkEditGroup extends SubCommand<Datastore> {
    public BulkEditGroup() {
        super("group", "Bulk edit group memberships", "<group|null> <server|world> <from> <to>", Permission.USER_BULKCHANGE,
                Predicate.not(4));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Datastore datastore, List<String> args, String label) {
        String group = args.get(0);
        String type = args.get(1).toLowerCase();
        String from = args.get(2);
        String to = args.get(3);
        if (to.equals("null")) {
            to = null;
        }

        if (!type.equals("world") && !type.equals("server")) {
            Message.BULK_CHANGE_TYPE_ERROR.send(sender);
            return CommandResult.FAILURE;
        }

        Set<UUID> uuids = datastore.getUniqueUsers();

        for (UUID u : uuids) {
            plugin.getDatastore().loadUser(u, "null");
            User user = plugin.getUserManager().get(u);
            if (user == null) {
                continue;
            }

            Set<Node> toAdd = new HashSet<>();
            Iterator<Node> iterator = user.getNodes().iterator();
            if (type.equals("world")) {
                while (iterator.hasNext()) {
                    Node element = iterator.next();

                    if (!element.isGroupNode()) {
                        continue;
                    }

                    if (element.getGroupName().equals(user.getPrimaryGroup())) {
                        continue;
                    }

                    if (!group.equals("null") && !element.getGroupName().equals(group)) {
                        continue;
                    }

                    String world = element.getWorld().orElse("null");
                    if (!world.equals(from)) {
                        continue;
                    }

                    iterator.remove();
                    toAdd.add(me.lucko.luckperms.core.Node.builderFromExisting(element).setWorld(to).build());
                }
            } else {
                while (iterator.hasNext()) {
                    Node element = iterator.next();

                    if (!element.isGroupNode()) {
                        continue;
                    }

                    if (element.getGroupName().equals(user.getPrimaryGroup())) {
                        continue;
                    }

                    if (!group.equals("null") && !element.getGroupName().equals(group)) {
                        continue;
                    }

                    String server = element.getServer().orElse("global");
                    if (!server.equals(from)) {
                        continue;
                    }

                    iterator.remove();
                    toAdd.add(me.lucko.luckperms.core.Node.builderFromExisting(element).setServer(to).build());
                }
            }

            user.getNodes().addAll(toAdd);
            plugin.getUserManager().cleanup(user);
            plugin.getDatastore().saveUser(user);
        }

        Message.BULK_CHANGE_SUCCESS.send(sender, uuids.size());
        return CommandResult.SUCCESS;
    }
}
