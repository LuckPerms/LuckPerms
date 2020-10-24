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

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.ChildCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.UserIdentifier;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;
import me.lucko.luckperms.common.util.Uuids;

import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;

import org.anjocaido.groupmanager.GlobalGroups;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.WorldDataHolder;
import org.anjocaido.groupmanager.dataholder.worlds.WorldsHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MigrationGroupManager extends ChildCommand<Object> {

    final String DEFAULT_CONTEXT_VALUE = "global";

    public MigrationGroupManager() {
        super(CommandSpec.MIGRATION_GROUPMANAGER, "groupmanager", CommandPermission.MIGRATION, Predicates.is(0));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object ignored, ArgumentList args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "GroupManager");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!args.get(0).equalsIgnoreCase("true") && !args.get(0).equalsIgnoreCase("false")) {
            log.logError("Was expecting true/false, but got " + args.get(0) + " instead.");
            return CommandResult.STATE_ERROR;
        }

        final boolean migrateAsGlobal = Boolean.parseBoolean(args.get(0));
        final Function<String, String> worldMappingFunc = s -> migrateAsGlobal || s == null ? DEFAULT_CONTEXT_VALUE : s;

        if (!args.get(1).equalsIgnoreCase("true") && !args.get(1).equalsIgnoreCase("false")) {
            log.logError("Was expecting true/false, but got " + args.get(1) + " instead.");
            return CommandResult.STATE_ERROR;
        }

        final boolean migrateServerContext = Boolean.parseBoolean(args.get(1));
        ImmutableContextSet defaultContexts = plugin.getConfiguration().getContextsFile().getDefaultContexts();
        final Optional<String> serverContext = defaultContexts.getAnyValue(DefaultContextKeys.SERVER_KEY);
        final Function<String, String> contextMappingFunc = s -> migrateServerContext || s == null ? DEFAULT_CONTEXT_VALUE : s;

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
        Iterators.tryIterate(gg.getGroupList(), g -> {
            String groupName = MigrationUtils.standardizeName(g.getName());
            Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

            for (String node : g.getPermissionList()) {
                if (node.isEmpty()) continue;
                group.setNode(DataType.NORMAL, MigrationUtils.parseNode(node, true).build(), true);
            }
            for (String s : g.getInherits()) {
                if (s.isEmpty()) continue;
                group.setNode(DataType.NORMAL, Inheritance.builder(MigrationUtils.standardizeName(s)).build(), true);
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
        Iterators.tryIterate(worlds, String::toLowerCase, world -> {
            log.log("Querying world " + world);

            WorldDataHolder wdh = wh.getWorldData(world);

            AtomicInteger groupWorldCount = new AtomicInteger(0);
            Iterators.tryIterate(wdh.getGroupList(), group -> {
                String groupName = MigrationUtils.standardizeName(group.getName());

                groups.putIfAbsent(groupName, new HashSet<>());

                for (String node : group.getPermissionList()) {
                    if (node.isEmpty()) continue;
                    groups.get(groupName).add(
                            MigrationUtils.parseNode(node, true)
                                    .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                    .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                    .build()
                    );
                }
                for (String s : group.getInherits()) {
                    if (s.isEmpty()) continue;
                    groups.get(groupName).add(
                            Inheritance.builder(MigrationUtils.standardizeName(s)).value(true)
                                    .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                    .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                    .build()
                    );
                }

                String[] metaKeys = group.getVariables().getVarKeyList();
                for (String key : metaKeys) {
                    String value = group.getVariables().getVarString(key);
                    key = key.toLowerCase();
                    if (key.isEmpty() || value.isEmpty()) continue;
                    if (key.equals("build")) continue;

                    if (key.equals("prefix")) {
                        groups.get(groupName).add(
                                Prefix.builder(value, 50)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
                    } else if (key.equals("suffix")) {
                        groups.get(groupName).add(
                                Suffix.builder(value, 50)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
                    } else {
                        groups.get(groupName).add(
                                Meta.builder(key, value)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
                    }
                }

                log.logAllProgress("Migrated {} groups so far in world " + world, groupWorldCount.incrementAndGet());
            });
            log.log("Migrated " + groupWorldCount.get() + " groups in world " + world);

            AtomicInteger userWorldCount = new AtomicInteger(0);
            Iterators.tryIterate(wdh.getUserList(), user -> {
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
                    users.get(id).add(
                            MigrationUtils.parseNode(node, true)
                                    .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                    .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                    .build()
                    );
                }

                // Collect sub groups
                String finalWorld = worldMappingFunc.apply(world);
                users.get(id).addAll(user.subGroupListStringCopy().stream()
                        .filter(n -> !n.isEmpty())
                        .map(MigrationUtils::standardizeName)
                        .map(n -> Inheritance.builder(n).value(true)
                                .withContext(DefaultContextKeys.WORLD_KEY, finalWorld)
                                .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                .build()
                        )
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

                    if (key.equals("prefix")) {
                        users.get(id).add(
                                Prefix.builder(value, 100)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
                    } else if (key.equals("suffix")) {
                        users.get(id).add(
                                Suffix.builder(value, 100)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
                    } else {
                        users.get(id).add(
                                Meta.builder(key, value)
                                        .withContext(DefaultContextKeys.WORLD_KEY, worldMappingFunc.apply(world))
                                        .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                        .build()
                        );
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
        Iterators.tryIterate(groups.entrySet(), e -> {
            Group group = plugin.getStorage().createAndLoadGroup(e.getKey(), CreationCause.INTERNAL).join();

            for (Node node : e.getValue()) {
                group.setNode(DataType.NORMAL, node, true);
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);
        Iterators.tryIterate(users.entrySet(), e -> {
            User user = plugin.getStorage().loadUser(e.getKey().getUniqueId(), e.getKey().getUsername().orElse(null)).join();

            for (Node node : e.getValue()) {
                user.setNode(DataType.NORMAL, node, true);
            }

            String primaryGroup = primaryGroups.get(e.getKey().getUniqueId());
            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                user.setNode(DataType.NORMAL,
                        Inheritance.builder(primaryGroup)
                                .withContext(DefaultContextKeys.SERVER_KEY, contextMappingFunc.apply(serverContext.orElse(DEFAULT_CONTEXT_VALUE)))
                                .build(),
                        true
                );
                user.getPrimaryGroup().setStoredValue(primaryGroup);
                user.unsetNode(DataType.NORMAL, Inheritance.builder(me.lucko.luckperms.common.model.manager.group.GroupManager.DEFAULT_GROUP_NAME).build());
            }

            plugin.getStorage().saveUser(user);
            plugin.getUserManager().getHouseKeeper().cleanup(user.getUniqueId());
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        log.log("Don't forget to remove the GroupManager jar from your plugins folder & restart the server. " +
                "LuckPerms may not take over as the server permission handler until this is done.");
        return CommandResult.SUCCESS;
    }
}
