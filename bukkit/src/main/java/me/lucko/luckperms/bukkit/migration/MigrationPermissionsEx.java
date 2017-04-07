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

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;

import org.bukkit.Bukkit;
import org.bukkit.World;

import ru.tehkode.permissions.NativeInterface;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", Permission.MIGRATION, Predicates.alwaysFalse(), null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        ProgressLogger log = new ProgressLogger("PermissionsEx");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")) {
            log.logErr("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        List<String> worlds = Bukkit.getWorlds().stream().map(World::getName).map(String::toLowerCase).collect(Collectors.toList());

        PermissionsEx pex = (PermissionsEx) Bukkit.getPluginManager().getPlugin("PermissionsEx");
        PermissionManager manager = pex.getPermissionsManager();

        NativeInterface ni;
        try {
            Field f = manager.getClass().getDeclaredField("nativeI");
            f.setAccessible(true);
            ni = (NativeInterface) f.get(manager);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }

        log.log("Calculating group weightings.");
        int maxWeight = 0;
        for (PermissionGroup group : manager.getGroupList()) {
            maxWeight = Math.max(maxWeight, group.getRank());
        }
        maxWeight += 5;

        // Migrate all groups.
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        for (PermissionGroup group : manager.getGroupList()) {
            int groupWeight = maxWeight - group.getRank();

            final String groupName = MigrationUtils.standardizeName(group.getName());
            plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
            Group lpGroup = plugin.getGroupManager().getIfLoaded(groupName);

            MigrationUtils.setGroupWeight(lpGroup, groupWeight);

            try {
                for (String node : group.getOwnPermissions(null)) {
                    if (node.isEmpty()) {
                        continue;
                    }

                    lpGroup.setPermission(MigrationUtils.parseNode(node, true).build());
                }
            } catch (NullPointerException ignored) {
                // No docs on if #getOwnPermissions(null) is ok. Should be fine though.
            }

            for (String world : worlds) {
                if (world.isEmpty()) {
                    continue;
                }

                for (String node : group.getOwnPermissions(world)) {
                    if (node.isEmpty()) {
                        continue;
                    }

                    lpGroup.setPermission(MigrationUtils.parseNode(node, true).setWorld(world.toLowerCase()).build());
                }
            }

            for (PermissionGroup g : group.getParents()) {
                lpGroup.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(g.getName())));
            }

            for (String world : worlds) {
                if (world.isEmpty()) {
                    continue;
                }

                for (PermissionGroup g : group.getParents(world)) {
                    lpGroup.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(g.getName()), true, "global", world.toLowerCase()));
                }
            }

            String prefix = group.getOwnPrefix();
            String suffix = group.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                lpGroup.setPermission(NodeFactory.makePrefixNode(groupWeight, prefix).build());
            }

            if (suffix != null && !suffix.equals("")) {
                lpGroup.setPermission(NodeFactory.makeSuffixNode(groupWeight, suffix).build());
            }

            plugin.getStorage().saveGroup(lpGroup);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        }
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        // Increment the max weight from the group migrations. All user meta should override.
        maxWeight += 5;

        for (PermissionUser user : manager.getUsers()) {
            UUID u;
            try {
                u = UUID.fromString(user.getIdentifier());
            } catch (IllegalArgumentException e) {
                u = ni.nameToUUID(user.getIdentifier());
                if (u == null) {
                    try {
                        u = Bukkit.getOfflinePlayer(user.getIdentifier()).getUniqueId();
                    } catch (Exception ex) {
                        e.printStackTrace();
                    }
                }
            }

            if (u == null) {
                log.logErr("Unable to get a UUID for user identifier: " + user.getIdentifier());
                continue;
            }

            plugin.getStorage().loadUser(u, user.getName()).join();
            User lpUser = plugin.getUserManager().get(u);

            try {
                for (String node : user.getOwnPermissions(null)) {
                    if (node.isEmpty()) {
                        continue;
                    }

                    lpUser.setPermission(MigrationUtils.parseNode(node, true).build());
                }
            } catch (NullPointerException ignored) {
                // No docs on if #getOwnPermissions(null) is ok. Should be fine though.
            }

            for (String world : worlds) {
                if (world.isEmpty()) {
                    continue;
                }

                for (String node : user.getOwnPermissions(world)) {
                    if (node.isEmpty()) {
                        continue;
                    }

                    lpUser.setPermission(MigrationUtils.parseNode(node, true).setWorld(world.toLowerCase()).build());
                }
            }

            for (String g : user.getGroupNames()) {
                if (g.isEmpty()) {
                    continue;
                }

                lpUser.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(g)));
            }

            for (String world : worlds) {
                if (world.isEmpty()) {
                    continue;
                }

                for (String g : user.getGroupNames(world)) {
                    if (g.isEmpty()) {
                        continue;
                    }

                    lpUser.setPermission(NodeFactory.make("group." + MigrationUtils.standardizeName(g), true, "global", world.toLowerCase()));
                }
            }

            String prefix = user.getOwnPrefix();
            String suffix = user.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                lpUser.setPermission(NodeFactory.makePrefixNode(maxWeight, prefix).build());
            }

            if (suffix != null && !suffix.equals("")) {
                lpUser.setPermission(NodeFactory.makeSuffixNode(maxWeight, suffix).build());
            }

            // Lowest rank is the highest group #logic
            String primary = null;
            int weight = Integer.MAX_VALUE;
            for (PermissionGroup group : user.getOwnParents()) {
                if (group.getRank() < weight) {
                    primary = group.getName();
                    weight = group.getRank();
                }
            }

            if (primary != null && !primary.equalsIgnoreCase("default")) {
                lpUser.setPermission(NodeFactory.make("group." + primary.toLowerCase()));
                lpUser.getPrimaryGroup().setStoredValue(primary);
                lpUser.unsetPermission(NodeFactory.make("group.default"));
            }

            plugin.getUserManager().cleanup(lpUser);
            plugin.getStorage().saveUser(lpUser);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        }

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }
}
