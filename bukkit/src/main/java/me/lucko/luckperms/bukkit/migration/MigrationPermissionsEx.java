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

import com.google.common.base.Strings;

import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.factory.NodeTypes;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;

import org.bukkit.Bukkit;

import ru.tehkode.permissions.PermissionEntity;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.PermissionsData;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MigrationPermissionsEx extends SubCommand<Object> {
    private static final Method GET_DATA_METHOD;
    private static final Field TIMED_PERMISSIONS_FIELD;
    private static final Field TIMED_PERMISSIONS_TIME_FIELD;
    static {
        try {
            GET_DATA_METHOD = PermissionEntity.class.getDeclaredMethod("getData");
            GET_DATA_METHOD.setAccessible(true);

            TIMED_PERMISSIONS_FIELD = PermissionEntity.class.getDeclaredField("timedPermissions");
            TIMED_PERMISSIONS_FIELD.setAccessible(true);

            TIMED_PERMISSIONS_TIME_FIELD = PermissionEntity.class.getDeclaredField("timedPermissionsTime");
            TIMED_PERMISSIONS_TIME_FIELD.setAccessible(true);
        } catch (NoSuchMethodException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MigrationPermissionsEx(LocaleManager locale) {
        super(CommandSpec.MIGRATION_COMMAND.localize(locale), "permissionsex", CommandPermission.MIGRATION, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "PermissionsEx");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")) {
            log.logError("Plugin not loaded.");
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
        Iterators.iterate(manager.getGroupList(), group -> {
            int groupWeight = maxWeight - group.getRank();

            final String groupName = MigrationUtils.standardizeName(group.getName());
            Group lpGroup = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

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
            Track track = plugin.getStorage().createAndLoadTrack(rankLadder, CreationCause.INTERNAL).join();

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

        Iterators.iterate(manager.getUsers(), user -> {
            UUID u = BukkitUuids.lookupUuid(log, user.getIdentifier());
            if (u == null) {
                return;
            }

            // load in a user instance
            User lpUser = plugin.getStorage().loadUser(u, user.getName()).join();

            // migrate data
            migrateEntity(user, lpUser, userWeight);

            plugin.getUserManager().cleanup(lpUser);
            plugin.getStorage().saveUser(lpUser);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private static Map<String, List<String>> getPermanentPermissions(PermissionEntity entity) {
        try {
            PermissionsData data = (PermissionsData) GET_DATA_METHOD.invoke(entity);
            return data.getPermissionsMap();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private static void migrateEntity(PermissionEntity entity, PermissionHolder holder, int weight) {
        // migrate permanent permissions
        for (Map.Entry<String, List<String>> worldData : getPermanentPermissions(entity).entrySet()) {
            String world = standardizeWorld(worldData.getKey());
            for (String node : worldData.getValue()) {
                if (node.isEmpty()) continue;
                holder.setPermission(MigrationUtils.parseNode(node, true).withContext(DefaultContextKeys.WORLD_KEY, world).build());
            }
        }

        // migrate temporary permissions
        Map<String, List<String>> timedPermissions;
        Map<String, Long> timedPermissionsTime;

        try {
            //noinspection unchecked
            timedPermissions = (Map<String, List<String>>) TIMED_PERMISSIONS_FIELD.get(entity);
            //noinspection unchecked
            timedPermissionsTime = (Map<String, Long>) TIMED_PERMISSIONS_TIME_FIELD.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, List<String>> worldData : timedPermissions.entrySet()) {
            String world = standardizeWorld(worldData.getKey());
            for (String node : worldData.getValue()) {
                if (node.isEmpty()) continue;
                long expiry = timedPermissionsTime.getOrDefault(Strings.nullToEmpty(world) + ":" + node, 0L);
                holder.setPermission(MigrationUtils.parseNode(node, true).withContext(DefaultContextKeys.WORLD_KEY, world).expiry(expiry).build());
            }
        }

        // migrate parents
        for (Map.Entry<String, List<PermissionGroup>> worldData : entity.getAllParents().entrySet()) {
            String world = standardizeWorld(worldData.getKey());

            // keep track of primary group
            String primary = null;
            int primaryWeight = Integer.MAX_VALUE;

            for (PermissionGroup parent : worldData.getValue()) {
                String parentName = parent.getName();
                long expiry = 0L;

                // check for temporary parent
                if (entity instanceof PermissionUser) {
                    String expiryOption = entity.getOption("group-" + parentName + "-until", world);
                    if (expiryOption != null) {
                        try {
                            expiry = Long.parseLong(expiryOption);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }

                holder.setPermission(NodeFactory.buildGroupNode(MigrationUtils.standardizeName(parentName)).withContext(DefaultContextKeys.WORLD_KEY, world).expiry(expiry).build());

                // migrate primary groups
                if (world == null && holder instanceof User && expiry == 0) {
                    if (parent.getRank() < primaryWeight) {
                        primary = parent.getName();
                        primaryWeight = parent.getRank();
                    }
                }
            }

            if (primary != null && !primary.isEmpty() && !primary.equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
                User user = ((User) holder);
                user.getPrimaryGroup().setStoredValue(primary);
                user.unsetPermission(NodeFactory.buildGroupNode(NodeFactory.DEFAULT_GROUP_NAME).build());
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
        for (Map.Entry<String, Map<String, String>> worldData : entity.getAllOptions().entrySet()) {
            String world = standardizeWorld(worldData.getKey());
            for (Map.Entry<String, String> opt : worldData.getValue().entrySet()) {
                if (opt.getKey() == null || opt.getKey().isEmpty() || opt.getValue() == null || opt.getValue().isEmpty()) {
                    continue;
                }

                String key = opt.getKey().toLowerCase();
                boolean ignore = key.equals(NodeTypes.PREFIX_KEY) ||
                        key.equals(NodeTypes.SUFFIX_KEY) ||
                        key.equals(NodeTypes.WEIGHT_KEY) ||
                        key.equals("rank") ||
                        key.equals("rank-ladder") ||
                        key.equals("name") ||
                        key.equals("username") ||
                        (key.startsWith("group-") && key.endsWith("-until"));

                if (ignore) {
                    continue;
                }

                holder.setPermission(NodeFactory.buildMetaNode(opt.getKey(), opt.getValue()).withContext(DefaultContextKeys.WORLD_KEY, world).build());
            }
        }
    }

    private static String standardizeWorld(String world) {
        if (world != null && (world.isEmpty() || world.equals("*"))) {
            world = null;
        }
        if (world != null) {
            world = world.toLowerCase();
        }
        return world;
    }
}
