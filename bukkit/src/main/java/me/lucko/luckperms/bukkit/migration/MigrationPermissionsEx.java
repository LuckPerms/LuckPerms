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
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SubCommand;
import me.lucko.luckperms.common.commands.impl.migration.MigrationUtils;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.CommandSpec;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.logging.ProgressLogger;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.SafeIterator;

import org.bukkit.Bukkit;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

        log.log("Calculating group weightings.");
        int i = 0;
        for (PermissionGroup group : manager.getGroupList()) {
            i = Math.max(i, group.getRank());
        }
        int maxWeight = i + 5;

        // Migrate all groups.
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        Set<String> ladders = new HashSet<>();
        SafeIterator.iterate(manager.getGroupList(), group -> {
            int groupWeight = maxWeight - group.getRank();

            final String groupName = MigrationUtils.standardizeName(group.getName());
            plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();
            Group lpGroup = plugin.getGroupManager().getIfLoaded(groupName);

            MigrationUtils.setGroupWeight(lpGroup, groupWeight);

            // migrate data
            migrateEntity(group, lpGroup, groupWeight);

            // remember known ladders
            if (group.isRanked()) {
                ladders.add(group.getRankLadder().toLowerCase());
            }

            plugin.getStorage().saveGroup(lpGroup).join();
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all ladders/tracks.
        log.log("Starting tracks migration.");
        for (String rankLadder : ladders) {
            plugin.getStorage().createAndLoadTrack(rankLadder, CreationCause.INTERNAL).join();
            Track track = plugin.getTrackManager().getIfLoaded(rankLadder);

            // Get a list of all groups in a ladder
            List<String> ladder = manager.getRankLadder(rankLadder).entrySet().stream()
                    .sorted(Comparator.<Map.Entry<Integer, PermissionGroup>>comparingInt(Map.Entry::getKey).reversed())
                    .map(e -> MigrationUtils.standardizeName(e.getValue().getName()))
                    .collect(Collectors.toList());

            track.setGroups(ladder);
            plugin.getStorage().saveTrack(track);
        }
        log.log("Migrated " + ladders.size() + " tracks");

        // Migrate all users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        // Increment the max weight from the group migrations. All user meta should override.
        int userWeight = maxWeight + 5;

        SafeIterator.iterate(manager.getUsers(), user -> {
            UUID u = BukkitMigrationUtils.lookupUuid(log, user.getIdentifier());
            if (u == null) {
                return;
            }

            // load in a user instance
            plugin.getStorage().loadUser(u, user.getName()).join();
            User lpUser = plugin.getUserManager().getIfLoaded(u);

            // migrate data
            migrateEntity(user, lpUser, userWeight);

            // Lowest rank is the highest group #logic
            String primary = null;
            int weight = Integer.MAX_VALUE;
            for (PermissionGroup group : user.getOwnParents()) {
                if (group.getRank() < weight) {
                    primary = group.getName();
                    weight = group.getRank();
                }
            }

            if (primary != null && !primary.isEmpty() && !primary.equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
                lpUser.setPermission(NodeFactory.buildGroupNode(primary.toLowerCase()).build());
                lpUser.getPrimaryGroup().setStoredValue(primary);
                lpUser.unsetPermission(NodeFactory.buildGroupNode(NodeFactory.DEFAULT_GROUP_NAME).build());
            }

            plugin.getUserManager().cleanup(lpUser);
            plugin.getStorage().saveUser(lpUser);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet());
        });

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
                if (node.isEmpty()) continue;
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
                holder.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(parent.getName())).setWorld(world).build());
            }
        }

        // migrate prefix / suffix
        String prefix = entity.getOwnPrefix();
        String suffix = entity.getOwnSuffix();

        if (prefix != null && !prefix.isEmpty()) {
            holder.setPermission(NodeFactory.buildPrefixNode(weight, prefix).build());
        }

        if (suffix != null && !suffix.isEmpty()) {
            holder.setPermission(NodeFactory.buildSuffixNode(weight, suffix).build());
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
                if (key.equals(NodeFactory.PREFIX_KEY) || key.equals(NodeFactory.SUFFIX_KEY) || key.equals(NodeFactory.WEIGHT_KEY) || key.equals("rank") || key.equals("rank-ladder") || key.equals("name") || key.equals("username")) {
                    continue;
                }

                holder.setPermission(NodeFactory.buildMetaNode(opt.getKey(), opt.getValue()).setWorld(world).build());
            }
        }
    }
}
