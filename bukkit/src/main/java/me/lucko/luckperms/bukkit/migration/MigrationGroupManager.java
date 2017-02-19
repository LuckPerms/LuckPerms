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

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MigrationGroupManager extends SubCommand<Object> {
    public MigrationGroupManager() {
        super("groupmanager", "Migration from GroupManager", Permission.MIGRATION, Predicates.is(0),
                Arg.list(Arg.create("migrate as global", true, "if world permissions should be ignored, and just migrated as global"))
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("GroupManager");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!args.get(0).equalsIgnoreCase("true") && !args.get(0).equalsIgnoreCase("false")) {
            log.logErr("Was expecting true/false, but got " + args.get(0) + " instead.");
            return CommandResult.STATE_ERROR;
        }
        boolean migrateAsGlobal = Boolean.parseBoolean(args.get(0));
        final Function<String, String> worldMappingFunc = s -> migrateAsGlobal ? "" : s;
        
        if (!Bukkit.getPluginManager().isPluginEnabled("GroupManager")) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).map(String::toLowerCase).collect(Collectors.toList());

        GroupManager gm = (GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager");

        // Migrate Global Groups
        log.log("Starting global group migration.");
        GlobalGroups gg = GroupManager.getGlobalGroups();

        AtomicInteger globalGroupCount = new AtomicInteger(0);
        for (Group g : gg.getGroupList()) {
            plugin.getStorage().createAndLoadGroup(g.getName().toLowerCase(), CreationCause.INTERNAL).join();
            me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(g.getName().toLowerCase());

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
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            for (String s : g.getInherits()) {
                try {
                    group.setPermission("group." + s.toLowerCase(), true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", globalGroupCount.incrementAndGet());
        }
        log.log("Migrated " + globalGroupCount.get() + " global groups");

        // Collect data

        // UUID --> Map<Entry<String, String>, Boolean> where k=world, v = node
        Map<UUID, Map<Map.Entry<String, String>, Boolean>> users = new HashMap<>();
        // UUID --> primary group name
        Map<UUID, String> primaryGroups = new HashMap<>();

        // String --> Map<Entry<String, String>, Boolean> where k=world, v = node
        Map<String, Map<Map.Entry<String, String>, Boolean>> groups = new HashMap<>();

        WorldsHolder wh = gm.getWorldsHolder();

        // Collect data for all users and groups.
        log.log("Collecting user and group data.");
        for (String world : worlds) {
            world = world.toLowerCase();
            log.log("Querying world " + world);

            WorldDataHolder wdh = wh.getWorldData(world);

            AtomicInteger groupWorldCount = new AtomicInteger(0);
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

                    groups.get(g.getName().toLowerCase()).put(Maps.immutableEntry(worldMappingFunc.apply(world), node), value);
                }

                for (String s : g.getInherits()) {
                    groups.get(g.getName().toLowerCase()).put(Maps.immutableEntry(worldMappingFunc.apply(world), "group." + s.toLowerCase()), true);
                }
                log.logAllProgress("Migrated {} groups so far in world " + world, groupWorldCount.incrementAndGet());
            }
            log.log("Migrated " + groupWorldCount.get() + " groups in world " + world);

            AtomicInteger userWorldCount = new AtomicInteger(0);
            for (User user : wdh.getUserList()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(user.getUUID());
                } catch (IllegalArgumentException e) {
                    log.logErr("Could not parse UUID for user: " + user.getUUID());
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

                    users.get(uuid).put(Maps.immutableEntry(worldMappingFunc.apply(world), node), value);
                }

                String finalWorld = worldMappingFunc.apply(world);

                // Collect sub groups
                users.get(uuid).putAll(user.subGroupListStringCopy().stream()
                        .map(n -> "group." + n)
                        .map(n -> Maps.immutableEntry(finalWorld, n))
                        .collect(Collectors.toMap(n -> n, n -> true))
                );
                primaryGroups.put(uuid, user.getGroupName());

                log.logProgress("Migrated {} users so far in world " + world, userWorldCount.incrementAndGet());
            }
            log.log("Migrated " + userWorldCount.get() + " users in world " + world);

        }

        log.log("All data has now been processed, now starting the import process.");
        log.log("Found a total of " + users.size() + " users and " + groups.size() + " groups.");

        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        for (Map.Entry<String, Map<Map.Entry<String, String>, Boolean>> e : groups.entrySet()) {
            plugin.getStorage().createAndLoadGroup(e.getKey(), CreationCause.INTERNAL).join();
            me.lucko.luckperms.common.core.model.Group group = plugin.getGroupManager().getIfLoaded(e.getKey());

            for (Map.Entry<Map.Entry<String, String>, Boolean> n : e.getValue().entrySet()) {
                // n.key.key = world
                // n.key.value = node
                // n.value = true/false
                try {
                    if (n.getKey().getKey().equals("")) {
                        group.setPermission(n.getKey().getValue(), n.getValue());
                    } else {
                        group.setPermission(n.getKey().getValue(), n.getValue(), "global", n.getKey().getKey());
                    }
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);
        for (Map.Entry<UUID, Map<Map.Entry<String, String>, Boolean>> e : users.entrySet()) {
            plugin.getStorage().loadUser(e.getKey(), "null").join();
            me.lucko.luckperms.common.core.model.User user = plugin.getUserManager().get(e.getKey());

            for (Map.Entry<Map.Entry<String, String>, Boolean> n : e.getValue().entrySet()) {
                // n.key.key = world
                // n.key.value = node
                // n.value = true/false
                try {
                    if (n.getKey().getKey().equals("")) {
                        user.setPermission(n.getKey().getValue(), n.getValue());
                    } else {
                        user.setPermission(n.getKey().getValue(), n.getValue(), "global", n.getKey().getKey());
                    }
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            String primaryGroup = primaryGroups.get(e.getKey());
            if (primaryGroup != null) {
                try {
                    user.setPermission("group." + primaryGroup, true);
                } catch (ObjectAlreadyHasException ignored) {}
                user.setPrimaryGroup(primaryGroup);
                try {
                    user.unsetPermission("group.default");
                } catch (ObjectLacksException ignored) {}
            }

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
