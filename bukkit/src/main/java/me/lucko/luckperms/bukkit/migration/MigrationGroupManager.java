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

package me.lucko.luckperms.bukkit.migration;

import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MigrationGroupManager extends SubCommand<Object> {
    public MigrationGroupManager() {
        super("groupmanager", "Migration from GroupManager", Permission.MIGRATION, Predicates.is(0),
                Arg.list(Arg.create("world names...", false, "a list of worlds to migrate permissions from"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        final Logger log = plugin.getLog();
        if (!plugin.isPluginLoaded("GroupManager")) {
            log.severe("GroupManager Migration: Error -> GroupManager is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        final List<String> worlds = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        GroupManager gm = (GroupManager) plugin.getPlugin("GroupManager");

        // Migrate Global Groups
        log.info("GroupManager Migration: Starting Global Group migration.");
        GlobalGroups gg = GroupManager.getGlobalGroups();

        for (Group g : gg.getGroupList()) {
            plugin.getStorage().createAndLoadGroup(g.getName().toLowerCase()).join();
            me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(g.getName().toLowerCase());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(group).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (String node : g.getPermissionList()) {
                boolean value = true;
                if (node.startsWith("!") || node.startsWith("-")) {
                    node = node.substring(1);
                    value = false;
                } else if (node.startsWith("+")) {
                    node = node.substring(1);
                    value = true;
                }

                try {
                    group.setPermission(node, value);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("set " + node + " " + value)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            for (String s : g.getInherits()) {
                try {
                    group.setPermission("group." + s.toLowerCase(), true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("setinherit " + s.toLowerCase())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getStorage().saveGroup(group);
        }

        Map<UUID, Map<Map.Entry<String, String>, Boolean>> users = new HashMap<>();
        Map<UUID, String> primaryGroups = new HashMap<>();
        Map<String, Map<Map.Entry<String, String>, Boolean>> groups = new HashMap<>();

        WorldsHolder wh = gm.getWorldsHolder();

        // Collect data for all users and groups.
        log.info("GroupManager Migration: Starting user and group migration.");
        for (String world : worlds) {
            world = world.toLowerCase();

            WorldDataHolder wdh = wh.getWorldData(world);

            for (Group g : wdh.getGroupList()) {
                groups.putIfAbsent(g.getName().toLowerCase(), new HashMap<>());

                for (String node : g.getPermissionList()) {
                    boolean value = true;
                    if (node.startsWith("!") || node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    } else if (node.startsWith("+")) {
                        node = node.substring(1);
                        value = true;
                    }

                    groups.get(g.getName().toLowerCase()).put(new AbstractMap.SimpleEntry<>(world, node), value);
                }

                for (String s : g.getInherits()) {
                    groups.get(g.getName().toLowerCase()).put(new AbstractMap.SimpleEntry<>(world, "group." + s.toLowerCase()), true);
                }
            }

            for (User user : wdh.getUserList()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(user.getUUID());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                users.putIfAbsent(uuid, new HashMap<>());

                for (String node : user.getPermissionList()) {
                    boolean value = true;
                    if (node.startsWith("!") || node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    } else if (node.startsWith("+")) {
                        node = node.substring(1);
                        value = true;
                    }

                    users.get(uuid).put(new AbstractMap.SimpleEntry<>(world, node), value);
                }

                String finalWorld = world;
                users.get(uuid).putAll(user.subGroupListStringCopy().stream()
                        .map(n -> "group." + n)
                        .map(n -> new AbstractMap.SimpleEntry<>(finalWorld, n))
                        .collect(Collectors.toMap(n -> n, n -> true))
                );
                primaryGroups.put(uuid, user.getGroupName());
            }

        }

        log.info("GroupManager Migration: All existing GroupManager data has been processed. Now beginning the import process.");
        log.info("GroupManager Migration: Found a total of " + users.size() + " users and " + groups.size() + " groups.");

        for (Map.Entry<String, Map<Map.Entry<String, String>, Boolean>> e : groups.entrySet()) {
            plugin.getStorage().createAndLoadGroup(e.getKey()).join();
            me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(e.getKey());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(group).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            for (Map.Entry<Map.Entry<String, String>, Boolean> n : e.getValue().entrySet()) {
                // n.key.key = world
                // n.key.value = node
                // n.value = true/false
                try {
                    group.setPermission(NodeFactory.fromSerialisedNode("global-" + n.getKey().getKey() + "/" + n.getKey().getValue(), n.getValue()));

                    if (n.getKey().getValue().startsWith("group.")) {
                        String groupName = n.getKey().getValue().substring(6);
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(group).action("setinherit " + groupName + " global " + n.getKey().getKey())
                                .build().submit(plugin);
                    } else {
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(group).action("set " + n.getKey().getValue() + " " + n.getValue() + " global " + n.getKey().getKey())
                                .build().submit(plugin);
                    }

                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getStorage().saveGroup(group);
        }

        for (Map.Entry<UUID, Map<Map.Entry<String, String>, Boolean>> e : users.entrySet()) {
            plugin.getStorage().loadUser(e.getKey(), "null").join();
            me.lucko.luckperms.common.core.model.User user = plugin.getUserManager().get(e.getKey());

            for (Map.Entry<Map.Entry<String, String>, Boolean> n : e.getValue().entrySet()) {
                // n.key.key = world
                // n.key.value = node
                // n.value = true/false
                try {
                    user.setPermission(NodeFactory.fromSerialisedNode("global-" + n.getKey().getKey() + "/" + n.getKey().getValue(), n.getValue()));

                    if (n.getKey().getValue().startsWith("group.")) {
                        String group = n.getKey().getValue().substring(6);
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(user).action("addgroup " + group + " global " + n.getKey().getKey())
                                .build().submit(plugin);
                    } else {
                        LogEntry.build()
                                .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                .acted(user).action("set " + n.getKey().getValue() + " " + n.getValue() + " global " + n.getKey().getKey())
                                .build().submit(plugin);
                    }

                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            String primaryGroup = primaryGroups.get(e.getKey());
            if (primaryGroup != null) {
                try {
                    user.setPermission("group." + primaryGroup, true);
                } catch (ObjectAlreadyHasException ignored) {
                }
                user.setPrimaryGroup(primaryGroup);
                try {
                    user.unsetPermission("group.default");
                } catch (ObjectLacksException ignored) {
                }
            }

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
        }

        log.info("GroupManager Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
