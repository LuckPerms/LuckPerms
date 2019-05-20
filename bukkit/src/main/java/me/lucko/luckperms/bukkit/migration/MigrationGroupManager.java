/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.node.ChatMetaType;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.UserIdentifier;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.factory.NodeTypes;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;
import me.lucko.luckperms.common.util.Uuids;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MigrationGroupManager extends SubCommand<Object> {
    public MigrationGroupManager(LocaleManager locale) {
        super(CommandSpec.MIGRATION_GROUPMANAGER.localize(locale), "groupmanager", CommandPermission.MIGRATION, Predicates.is(0));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "GroupManager");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!args.get(0).equalsIgnoreCase("true") && !args.get(0).equalsIgnoreCase("false")) {
            log.logError("Was expecting true/false, but got " + args.get(0) + " instead.");
            return CommandResult.STATE_ERROR;
        }
        final boolean migrateAsGlobal = Boolean.parseBoolean(args.get(0));
        final Function<String, String> worldMappingFunc = s -> migrateAsGlobal ? null : s;
        
        if (!Bukkit.getPluginManager().isPluginEnabled("GroupManager")) {
            log.logError("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).map(String::toLowerCase).collect(Collectors.toList());
        GroupManager gm = (GroupManager) Bukkit.getPluginManager().getPlugin("GroupManager");

        // Migrate Global Groups
        log.log("Starting global group migration.");
        GlobalGroups gg = GroupManager.getGlobalGroups();

        AtomicInteger globalGroupCount = new AtomicInteger(0);
        Iterators.iterate(gg.getGroupList(), g -> {
            String groupName = MigrationUtils.standardizeName(g.getName());
            Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

            for (String node : g.getPermissionList()) {
                if (node.isEmpty()) continue;
                group.setPermission(MigrationUtils.parseNode(node, true).build());
            }
            for (String s : g.getInherits()) {
                if (s.isEmpty()) continue;
                group.setPermission(NodeFactory.make(NodeFactory.groupNode(MigrationUtils.standardizeName(s))));
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", globalGroupCount.incrementAndGet());
        });
        log.log("Migrated " + globalGroupCount.get() + " global groups");

        // Collect data
        Map<UserIdentifier, Set<Node>> users = new HashMap<>();
        Map<UUID, String> primaryGroups = new HashMap<>();
        Map<String, Set<Node>> groups = new HashMap<>();

        WorldsHolder wh = gm.getWorldsHolder();

        // Collect data for all users and groups.
        log.log("Collecting user and group data.");
        Iterators.iterate(worlds, String::toLowerCase, world -> {
            log.log("Querying world " + world);

            WorldDataHolder wdh = wh.getWorldData(world);

            AtomicInteger groupWorldCount = new AtomicInteger(0);
            Iterators.iterate(wdh.getGroupList(), group -> {
                String groupName = MigrationUtils.standardizeName(group.getName());

                groups.putIfAbsent(groupName, new HashSet<>());

                for (String node : group.getPermissionList()) {
                    if (node.isEmpty()) continue;
                    groups.get(groupName).add(MigrationUtils.parseNode(node, true).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                }
                for (String s : group.getInherits()) {
                    if (s.isEmpty()) continue;
                    groups.get(groupName).add(NodeFactory.make(NodeFactory.groupNode(MigrationUtils.standardizeName(s)), true, null, worldMappingFunc.apply(world)));
                }

                String[] metaKeys = group.getVariables().getVarKeyList();
                for (String key : metaKeys) {
                    String value = group.getVariables().getVarString(key);
                    key = key.toLowerCase();
                    if (key.isEmpty() || value.isEmpty()) continue;
                    if (key.equals("build")) continue;

                    if (key.equals(NodeTypes.PREFIX_KEY) || key.equals(NodeTypes.SUFFIX_KEY)) {
                        ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());
                        groups.get(groupName).add(NodeFactory.buildChatMetaNode(type, 50, value).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                    } else {
                        groups.get(groupName).add(NodeFactory.buildMetaNode(key, value).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                    }
                }

                log.logAllProgress("Migrated {} groups so far in world " + world, groupWorldCount.incrementAndGet());
            });
            log.log("Migrated " + groupWorldCount.get() + " groups in world " + world);

            AtomicInteger userWorldCount = new AtomicInteger(0);
            Iterators.iterate(wdh.getUserList(), user -> {
                UUID uuid = BukkitUuids.lookupUuid(log, user.getUUID());
                if (uuid == null) {
                    return;
                }

                String lastName = user.getLastName();
                if (lastName != null && Uuids.parse(lastName) != null) {
                    lastName = null;
                }

                UserIdentifier id = UserIdentifier.of(uuid, lastName);

                users.putIfAbsent(id, new HashSet<>());

                for (String node : user.getPermissionList()) {
                    if (node.isEmpty()) continue;
                    users.get(id).add(MigrationUtils.parseNode(node, true).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                }

                // Collect sub groups
                String finalWorld = worldMappingFunc.apply(world);
                users.get(id).addAll(user.subGroupListStringCopy().stream()
                        .filter(n -> !n.isEmpty())
                        .map(n -> NodeFactory.groupNode(MigrationUtils.standardizeName(n)))
                        .map(n -> NodeFactory.make(n, true, null, finalWorld))
                        .collect(Collectors.toSet())
                );

                // Get primary group
                primaryGroups.put(uuid, MigrationUtils.standardizeName(user.getGroupName()));

                String[] metaKeys = user.getVariables().getVarKeyList();
                for (String key : metaKeys) {
                    String value = user.getVariables().getVarString(key);
                    key = key.toLowerCase();
                    if (key.isEmpty() || value.isEmpty()) continue;
                    if (key.equals("build")) continue;

                    if (key.equals(NodeTypes.PREFIX_KEY) || key.equals(NodeTypes.SUFFIX_KEY)) {
                        ChatMetaType type = ChatMetaType.valueOf(key.toUpperCase());
                        users.get(id).add(NodeFactory.buildChatMetaNode(type, 100, value).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                    } else {
                        users.get(id).add(NodeFactory.buildMetaNode(key, value).withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world)).build());
                    }
                }

                log.logProgress("Migrated {} users so far in world " + world, userWorldCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
            });
            log.log("Migrated " + userWorldCount.get() + " users in world " + world);
        });

        log.log("All data has now been processed, now starting the import process.");
        log.log("Found a total of " + users.size() + " users and " + groups.size() + " groups.");

        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        Iterators.iterate(groups.entrySet(), e -> {
            Group group = plugin.getStorage().createAndLoadGroup(e.getKey(), CreationCause.INTERNAL).join();

            for (Node node : e.getValue()) {
                group.setPermission(node);
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);
        Iterators.iterate(users.entrySet(), e -> {
            User user = plugin.getStorage().loadUser(e.getKey().getUuid(), e.getKey().getUsername().orElse(null)).join();

            for (Node node : e.getValue()) {
                user.setPermission(node);
            }

            String primaryGroup = primaryGroups.get(e.getKey().getUuid());
            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                user.setPermission(NodeFactory.buildGroupNode(primaryGroup).build());
                user.getPrimaryGroup().setStoredValue(primaryGroup);
                user.unsetPermission(NodeFactory.buildGroupNode(NodeFactory.DEFAULT_GROUP_NAME).build());
            }

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().cleanup(user);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
