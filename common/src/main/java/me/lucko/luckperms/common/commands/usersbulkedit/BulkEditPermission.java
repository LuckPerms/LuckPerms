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

package me.lucko.luckperms.common.commands.usersbulkedit;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.Storage;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BulkEditPermission extends SubCommand<Storage> {
    public BulkEditPermission() {
        super("permission", "Bulk edit permissions", Permission.USER_BULKCHANGE, Predicates.not(4),
                Arg.list(
                        Arg.create("node|null", true, "the node to edit ('null' to select and edit all nodes)"),
                        Arg.create("server|world", true, "if the bulk change is modifying a 'server' or a 'world'"),
                        Arg.create("from", true, "the server/world to be changed from. can be 'global' or 'null' respectively"),
                        Arg.create("to", true, "the server/world to replace 'from' (can be 'null')")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Storage storage, List<String> args, String label) throws CommandException {
        String node = args.get(0);
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

        Set<UUID> uuids = storage.getUniqueUsers().join();

        for (UUID u : uuids) {
            plugin.getStorage().loadUser(u, "null").join();
            User user = plugin.getUserManager().get(u);
            if (user == null) {
                continue;
            }

            Set<Node> toAdd = new HashSet<>();
            Set<Node> toRemove = new HashSet<>();
            Iterator<Node> iterator = user.getNodes().iterator();
            if (type.equals("world")) {
                while (iterator.hasNext()) {
                    Node element = iterator.next();

                    if (element.isGroupNode()) {
                        continue;
                    }

                    if (!node.equals("null") && !element.getPermission().equals(node)) {
                        continue;
                    }

                    String world = element.getWorld().orElse("null");
                    if (!world.equals(from)) {
                        continue;
                    }

                    toRemove.add(element);
                    toAdd.add(NodeFactory.builderFromExisting(element).setWorld(to).build());
                }
            } else {
                while (iterator.hasNext()) {
                    Node element = iterator.next();

                    if (element.isGroupNode()) {
                        continue;
                    }

                    if (!node.equals("null") && !element.getPermission().equals(node)) {
                        continue;
                    }

                    String server = element.getServer().orElse("global");
                    if (!server.equals(from)) {
                        continue;
                    }

                    toRemove.add(element);
                    toAdd.add(NodeFactory.builderFromExisting(element).setServer(to).build());
                }
            }

            toRemove.forEach(n -> {
                try {
                    user.unsetPermission(n);
                } catch (ObjectLacksException ignored) {
                }
            });

            toAdd.forEach(n -> {
                try {
                    user.setPermission(n);
                } catch (ObjectAlreadyHasException ignored) {
                }
            });

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        Message.BULK_CHANGE_SUCCESS.send(sender, uuids.size());
        return CommandResult.SUCCESS;
    }
}
