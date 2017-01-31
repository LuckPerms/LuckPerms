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
import me.lucko.luckperms.api.PlatformType;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.SubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import org.bukkit.Bukkit;

import ru.tehkode.permissions.NativeInterface;
import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MigrationPermissionsEx extends SubCommand<Object> {
    public MigrationPermissionsEx() {
        super("permissionsex", "Migration from PermissionsEx", Permission.MIGRATION, Predicates.alwaysFalse(),
                Arg.list(Arg.create("world names...", false, "a list of worlds to migrate permissions from"))
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) throws CommandException {
        Consumer<String> log = s -> {
            Message.MIGRATION_LOG.send(sender, s);
            Message.MIGRATION_LOG.send(plugin.getConsoleSender(), s);
        };
        log.accept("Starting PermissionsEx migration.");
        
        if (!Bukkit.getPluginManager().isPluginEnabled("PermissionsEx")) {
            log.accept("Error -> PermissionsEx is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        if (plugin.getServerType() != PlatformType.BUKKIT) {
            // Sponge uses a completely different version of PEX.
            log.accept("PEX import is not supported on this platform.");
            return CommandResult.STATE_ERROR;
        }

        final List<String> worlds = args.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

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

        // Migrate all groups.
        log.accept("Starting group migration.");

        int maxGroupWeight = 0;
        int groupCount = 0;

        for (PermissionGroup group : manager.getGroupList()) {
            int groupWeight = group.getWeight() * -1;
            groupCount++;
            maxGroupWeight = Math.max(maxGroupWeight, groupWeight);

            final String name = group.getName().toLowerCase();
            plugin.getStorage().createAndLoadGroup(name).join();
            Group lpGroup = plugin.getGroupManager().getIfLoaded(name);
            try {
                LogEntry.build()
                        .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                        .acted(lpGroup).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                for (String node : group.getOwnPermissions(null)) {
                    boolean value = true;
                    if (node.startsWith("-")) {
                        node = node.substring(1);
                        value = false;
                    }

                    try {
                        lpGroup.setPermission(node, value);
                        LogEntry.build()
                                .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                .acted(lpGroup).action("set " + node + " " + value)
                                .build().submit(plugin);
                    } catch (Exception ex) {
                        if (!(ex instanceof ObjectAlreadyHasException)) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (NullPointerException ignored) {
                // Probably won't happen. I have no API docs on getOwnPermissions#null though.
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String node : group.getOwnPermissions(world)) {
                        boolean value = true;
                        if (node.startsWith("-")) {
                            node = node.substring(1);
                            value = false;
                        }

                        try {
                            lpGroup.setPermission(node, value, "global", world);
                            LogEntry.build()
                                    .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                    .acted(lpGroup).action("set " + node + " " + value + " global " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            for (PermissionGroup g : group.getParents()) {
                try {
                    lpGroup.setPermission("group." + g.getName().toLowerCase(), true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpGroup).action("setinherit " + g.getName().toLowerCase())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (PermissionGroup g : group.getParents(world)) {
                        try {
                            lpGroup.setPermission("group." + g.getName().toLowerCase(), true, "global", world);
                            LogEntry.build()
                                    .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                    .acted(lpGroup).action("setinherit " + g.getName().toLowerCase() + " global " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            String prefix = group.getOwnPrefix();
            String suffix = group.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    lpGroup.setPermission("prefix." + groupWeight + "." + prefix, true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpGroup).action("set prefix." + groupWeight + "." + prefix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = MetaUtils.escapeCharacters(suffix);
                try {
                    lpGroup.setPermission("suffix." + groupWeight + "." + suffix, true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpGroup).action("set suffix." + groupWeight + "." + suffix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getStorage().saveGroup(lpGroup);

        }

        log.accept("Migrated " + groupCount + " groups");

        // Migrate all users
        log.accept("Starting user migration.");
        int userCount = 0;
        maxGroupWeight++;
        for (PermissionUser user : manager.getUsers()) {
            UUID u = null;

            try {
                u = UUID.fromString(user.getIdentifier());
            } catch (IllegalArgumentException e) {
                u = ni.nameToUUID(user.getIdentifier());
                if (u == null) {
                    u = plugin.getUuidFromUsername(user.getIdentifier());
                }
            }

            if (u == null) {
                log.accept("Unable to get a UUID for user identifier: " + user.getIdentifier());
                continue;
            }

            userCount++;
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
                        LogEntry.build()
                                .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                .acted(lpUser).action("set " + node + " " + value)
                                .build().submit(plugin);
                    } catch (Exception ex) {
                        if (!(ex instanceof ObjectAlreadyHasException)) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (NullPointerException ignored) {
                // Probably won't happen. I have no API docs on getOwnPermissions#null though.
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String node : user.getOwnPermissions(world)) {
                        boolean value = true;
                        if (node.startsWith("-")) {
                            node = node.substring(1);
                            value = false;
                        }

                        try {
                            lpUser.setPermission(node, value, "global", world);
                            LogEntry.build()
                                    .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                    .acted(lpUser).action("set " + node + " " + value + " global " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            for (String s : user.getGroupNames()) {
                try {
                    lpUser.setPermission("group." + s.toLowerCase(), true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpUser).action("addgroup " + s.toLowerCase())
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (worlds != null && !worlds.isEmpty()) {
                for (String world : worlds) {
                    for (String s : user.getGroupNames(world)) {
                        try {
                            lpUser.setPermission("group." + s.toLowerCase(), true, "global", world);
                            LogEntry.build()
                                    .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                                    .acted(lpUser).action("addgroup " + s.toLowerCase() + " global " + world)
                                    .build().submit(plugin);
                        } catch (Exception ex) {
                            if (!(ex instanceof ObjectAlreadyHasException)) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            String prefix = user.getOwnPrefix();
            String suffix = user.getOwnSuffix();

            if (prefix != null && !prefix.equals("")) {
                prefix = MetaUtils.escapeCharacters(prefix);
                try {
                    lpUser.setPermission("prefix." + maxGroupWeight + "." + prefix, true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpUser).action("set prefix." + maxGroupWeight + "." + prefix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            if (suffix != null && !suffix.equals("")) {
                suffix = MetaUtils.escapeCharacters(suffix);
                try {
                    lpUser.setPermission("suffix." + maxGroupWeight + "." + suffix, true);
                    LogEntry.build()
                            .actor(Constants.CONSOLE_UUID).actorName(Constants.CONSOLE_NAME)
                            .acted(lpUser).action("set suffix." + maxGroupWeight + "." + suffix + " true")
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            String primary = null;
            int weight = -100;
            for (PermissionGroup group : user.getOwnParents()) {
                if (group.getRank() > weight) {
                    primary = group.getName();
                    weight = group.getWeight();
                }
            }

            if (primary != null) {
                try {
                    lpUser.setPermission("group." + primary.toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {
                }
                lpUser.setPrimaryGroup(primary);
            }

            plugin.getUserManager().cleanup(lpUser);
            plugin.getStorage().saveUser(lpUser);
        }

        log.accept("Migrated " + userCount + " users.");
        log.accept("Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }
}
