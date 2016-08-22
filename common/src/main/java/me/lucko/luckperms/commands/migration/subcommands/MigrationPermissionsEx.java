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

package me.lucko.luckperms.commands.migration.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import ru.tehkode.permissions.NativeInterface;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MigrationPermissionsEx extends SubCommand<Object> {

    private static Class<?> bukkitPlayer = null;
    private static Method getUniqueIdMethod = null;
    private static Method getPlayerMethod = null;

    static {
        try {
            bukkitPlayer = Class.forName("org.bukkit.entity.Player");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (bukkitPlayer != null) {
            try {
                getUniqueIdMethod = bukkitPlayer.getMethod("getUniqueId");
                getUniqueIdMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        try {
            getPlayerMethod = PermissionUser.class.getMethod("getPlayer");
            getPlayerMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", "/%s migration permissionsex [world names]",
                Permission.MIGRATION, Predicate.alwaysFalse());
    }

    @SuppressWarnings("deprecation")
    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        final Logger log = plugin.getLog();
        if (!plugin.isPluginLoaded("PermissionsEx")) {
            log.severe("PermissionsEx Migration: Error -> PermissionsEx is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        if (plugin.getType() != LuckPermsPlugin.Type.BUKKIT) {
            // Sponge uses a completely different version of PEX.
            log.severe("PEX import is not supported on this platform.");
            return CommandResult.STATE_ERROR;
        }

        final List<String> worlds = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        PermissionsEx pex = (PermissionsEx) plugin.getPlugin("PermissionsEx");
        PermissionManager manager; // The compiler complains if you call the method directly, as Bukkit is not in this module.
        try {
            manager = (PermissionManager) PermissionsEx.class.getMethod("getPermissionsManager").invoke(pex);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        NativeInterface ni;
        try {
            Field f = manager.getClass().getDeclaredField("nativeI");
            f.setAccessible(true);
            ni = (NativeInterface) f.get(manager);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }

        // Migrate all users
        log.info("PermissionsEx Migration: Starting user migration.");
        for (PermissionUser user : manager.getUsers()) {
            UUID u = null;

            try {
                u = UUID.fromString(user.getIdentifier());
            } catch (IllegalArgumentException e) {

                u = ni.nameToUUID(user.getIdentifier());

                if (u == null) {
                    if (bukkitPlayer != null) {
                        try {
                            Object playerObj = getPlayerMethod.invoke(user);
                            u = (UUID) getUniqueIdMethod.invoke(playerObj);

                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                }
            }

            if (u == null) {
                log.severe("Unable to get a UUID for user identifier: " + user.getIdentifier());
                continue;
            }

            plugin.getDatastore().loadOrCreateUser(u, "null");
            User lpUser = plugin.getUserManager().get(u);

            try {
                for (String node : user.getOwnPermissions(null)) {
                    boolean value = true;
                    if (node.startsWith("!")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpUser.setPermission(node, value);
                    } catch (ObjectAlreadyHasException ignored) {}
                }
            } catch (NullPointerException ignored) {
                // Probably won't happen. I have no API docs on getOwnPermissions#null though.
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String node : user.getOwnPermissions(world)) {
                        boolean value = true;
                        if (node.startsWith("!")) {
                            node = node.substring(1);
                            value = false;
                        }

                        try {
                            lpUser.setPermission(node, value, "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            for (String s : user.getGroupNames()) {
                try {
                    lpUser.setPermission("group." + s.toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String s : user.getGroupNames(world)) {
                        try {
                            lpUser.setPermission("group." + s.toLowerCase(), true, "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            plugin.getUserManager().cleanup(lpUser);
            plugin.getDatastore().saveUser(lpUser);
        }

        // Migrate all groups.
        log.info("PermissionsEx Migration: Starting group migration.");
        for (PermissionGroup group : manager.getGroupList()) {
            final String name = group.getName().toLowerCase();
            plugin.getDatastore().createAndLoadGroup(name);
            Group lpGroup = plugin.getGroupManager().get(name);

            try {
                for (String node : group.getOwnPermissions(null)) {
                    boolean value = true;
                    if (node.startsWith("!")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpGroup.setPermission(node, value);
                    } catch (ObjectAlreadyHasException ignored) {}
                }
            } catch (NullPointerException ignored) {
                // Probably won't happen. I have no API docs on getOwnPermissions#null though.
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String node : group.getOwnPermissions(world)) {
                        boolean value = true;
                        if (node.startsWith("!")) {
                            node = node.substring(1);
                            value = false;
                        }

                        try {
                            lpGroup.setPermission(node, value, "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }

            for (PermissionGroup g : group.getParents()) {
                try {
                    lpGroup.setPermission("group." + g.getName().toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (PermissionGroup g : group.getParents(world)) {
                        try {
                            lpGroup.setPermission("group." + g.getName().toLowerCase(), true, "global", world);
                        } catch (ObjectAlreadyHasException ignored) {}
                    }
                }
            }
        }

        log.info("PermissionsEx Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
