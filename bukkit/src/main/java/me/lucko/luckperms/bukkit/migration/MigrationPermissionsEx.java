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

import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import org.bukkit.Bukkit;

import ru.tehkode.permissions.NativeInterface;
import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.spec(locale), "permissionsex", CommandPermission.MIGRATION, Predicates.alwaysFalse());
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

            // migrate data
            migrateEntity(group, lpGroup, groupWeight);

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
            User lpUser = plugin.getUserManager().getIfLoaded(u);

            // migrate data
            migrateEntity(user, lpUser, maxWeight);

            // Lowest rank is the highest group #logic
            String primary = null;
            int weight = Integer.MAX_VALUE;
            for (PermissionGroup group : user.getOwnParents()) {
                if (group.getRank() < weight) {
                    primary = group.getName();
                    weight = group.getRank();
                }
            }

            if (primary != null && !primary.isEmpty() && !primary.equalsIgnoreCase("default")) {
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

    private static void migrateEntity(PermissionEntity entity, PermissionHolder holder, int weight) {
        // migrate permissions
        Map<String, List<String>> permissions = entity.getAllPermissions();
        for (Map.Entry<String, List<String>> worldData : permissions.entrySet()) {
            String world = worldData.getKey();
            if (world != null && (world.equals("") || world.equals("*"))) {
                world = null;
            }
            if (world != null) {
                world = world.toLowerCase();
            }

            for (String node : worldData.getValue()) {
                if (node.isEmpty()) {
                    continue;
                }

                holder.setPermission(MigrationUtils.parseNode(node, true).setWorld(world).build());
            }
        }

        // migrate parents
        Map<String, List<PermissionGroup>> parents = entity.getAllParents();
        for (Map.Entry<String, List<PermissionGroup>> worldData : parents.entrySet()) {
            String world = worldData.getKey();
            if (world != null && (world.equals("") || world.equals("*"))) {
                world = null;
            }
            if (world != null) {
                world = world.toLowerCase();
            }

            for (PermissionGroup parent : worldData.getValue()) {
                holder.setPermission(NodeFactory.newBuilder("group." + MigrationUtils.standardizeName(parent.getName())).setWorld(world).build());
            }
        }

        // migrate prefix / suffix
        String prefix = entity.getOwnPrefix();
        String suffix = entity.getOwnSuffix();

        if (prefix != null && !prefix.isEmpty()) {
            holder.setPermission(NodeFactory.makePrefixNode(weight, prefix).build());
        }

        if (suffix != null && !suffix.isEmpty()) {
            holder.setPermission(NodeFactory.makeSuffixNode(weight, suffix).build());
        }

        // migrate options
        Map<String, Map<String, String>> options = entity.getAllOptions();
        for (Map.Entry<String, Map<String, String>> worldData : options.entrySet()) {
            String world = worldData.getKey();
            if (world != null && (world.isEmpty() || world.equals("*"))) {
                world = null;
            }
            if (world != null) {
                world = world.toLowerCase();
            }

            for (Map.Entry<String, String> opt : worldData.getValue().entrySet()) {
                if (opt.getKey() == null || opt.getKey().isEmpty() || opt.getValue() == null || opt.getValue().isEmpty()) {
                    continue;
                }

                String key = opt.getKey().toLowerCase();
                if (key.equals("prefix") || key.equals("suffix") || key.equals("weight") || key.equals("rank") || key.equals("name") || key.equals("username")) {
                    continue;
                }

                holder.setPermission(NodeFactory.makeMetaNode(opt.getKey(), opt.getValue()).setWorld(world).build());
            }
        }
    }
}
