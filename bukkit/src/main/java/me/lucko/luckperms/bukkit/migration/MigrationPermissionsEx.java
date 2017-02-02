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

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.ProgressLogger;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

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

            final String name = group.getName().toLowerCase();
            plugin.getStorage().createAndLoadGroup(name).join();
            Group lpGroup = plugin.getGroupManager().getIfLoaded(name);

            try {
                for (String node : group.getOwnPermissions(null)) {
                    boolean value = true;
                    if (node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpGroup.setPermission(node, value);
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            } catch (NullPointerException ignored) {
                // No docs on if #getOwnPermissions(null) is ok. Should be fine though.
            }

            for (String world : worlds) {
                for (String node : group.getOwnPermissions(world)) {
                    boolean value = true;
                    if (node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpGroup.setPermission(node, value, "global", world.toLowerCase());
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            }

            for (PermissionGroup g : group.getParents()) {
                try {
                    lpGroup.setPermission("group." + g.getName().toLowerCase(), true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            for (String world : worlds) {
                for (PermissionGroup g : group.getParents(world)) {
                    try {
                        lpGroup.setPermission("group." + g.getName().toLowerCase(), true, "global", world.toLowerCase());
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            }

            String prefix = group.getOwnPrefix();
            String suffix = group.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    lpGroup.setPermission("prefix." + groupWeight + "." + prefix, true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = MetaUtils.escapeCharacters(suffix);
                try {
                    lpGroup.setPermission("suffix." + groupWeight + "." + suffix, true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
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
                    u = plugin.getUuidFromUsername(user.getIdentifier());
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
                    boolean value = true;
                    if (node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpUser.setPermission(node, value);
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            } catch (NullPointerException ignored) {
                // No docs on if #getOwnPermissions(null) is ok. Should be fine though.
            }

            for (String world : worlds) {
                for (String node : user.getOwnPermissions(world)) {
                    boolean value = true;
                    if (node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpUser.setPermission(node, value, "global", world.toLowerCase());
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            }

            for (String s : user.getGroupNames()) {
                try {
                    lpUser.setPermission("group." + s.toLowerCase(), true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            for (String world : worlds) {
                for (String s : user.getGroupNames(world)) {
                    try {
                        lpUser.setPermission("group." + s.toLowerCase(), true, "global", world.toLowerCase());
                    } catch (Exception ex) {
                        log.handleException(ex);
                    }
                }
            }

            String prefix = user.getOwnPrefix();
            String suffix = user.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    lpUser.setPermission("prefix." + maxWeight + "." + prefix, true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = MetaUtils.escapeCharacters(suffix);
                try {
                    lpUser.setPermission("suffix." + maxWeight + "." + suffix, true);
                } catch (Exception ex) {
                    log.handleException(ex);
                }
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
                try {
                    lpUser.setPermission("group." + primary.toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {}
                lpUser.setPrimaryGroup(primary);
                try {
                    lpUser.unsetPermission("group.default");
                } catch (ObjectLacksException ignored) {}
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
