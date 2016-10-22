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

package me.lucko.luckperms.common.commands.migration.subcommands;

import com.github.cheesesoftware.PowerfulPermsAPI.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.commands.*;
import me.lucko.luckperms.common.commands.migration.subcommands.utils.LPResultRunnable;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.core.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import static me.lucko.luckperms.common.constants.Permission.MIGRATION;

@SuppressWarnings("unchecked")
public class MigrationPowerfulPerms extends SubCommand<Object> {
    /* <sadness>
        The PowerfulPerms API is a complete joke. Seriously, it would probably be easier reflecting into the actual plugin.
        Methods move about randomly every version...

        What kind of API requires reflection to function with multiple versions...
        Doesn't that just defeat the whole god damn point of having an API in the first place?
        Whatever happened to depreciation?
    </sadness> */

    private static Method getPlayerPermissionsMethod = null;
    private static Method getPlayerGroupsMethod = null;
    private static Method getGroupMethod = null;

    // lol
    private static boolean superLegacy = false;
    private static boolean legacy = false;

    static {
        try {
            Class.forName("com.github.cheesesoftware.PowerfulPermsAPI.ResponseRunnable");
            legacy = true;
        } catch (ClassNotFoundException ignored) {}

        if (legacy) {
            try {
                getPlayerPermissionsMethod = PermissionManager.class.getMethod("getPlayerOwnPermissions", UUID.class, ResultRunnable.class);
                getPlayerPermissionsMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
        } else {
            try {
                getPlayerPermissionsMethod = PermissionManager.class.getMethod("getPlayerOwnPermissions", UUID.class);
                getPlayerPermissionsMethod.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
        }

        try {
            getGroupMethod = CachedGroup.class.getMethod("getGroup");
            getGroupMethod.setAccessible(true);
            superLegacy = true;
        } catch (NoSuchMethodException ignored) {}

        if (!legacy) {
            try {
                getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerOwnGroups", UUID.class);
                getPlayerGroupsMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        } else {
            try {
                getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerOwnGroups", UUID.class, ResultRunnable.class);
                getPlayerGroupsMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                try {
                    getPlayerGroupsMethod = PermissionManager.class.getMethod("getPlayerGroups", UUID.class, ResultRunnable.class);
                    getPlayerGroupsMethod.setAccessible(true);
                } catch (NoSuchMethodException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }


    public MigrationPowerfulPerms() {
        super("powerfulperms", "Migration from PowerfulPerms", MIGRATION, Predicate.not(5),
                Arg.list(
                        Arg.create("address", true, "the address of the PP database"),
                        Arg.create("database", true, "the name of the PP database"),
                        Arg.create("username", true, "the username to log into the DB"),
                        Arg.create("password", true, "the password to log into the DB"),
                        Arg.create("db table", true, "the name of the PP table where player data is stored")
                )
        );
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        try {
            return run(plugin, args);
        } catch (Throwable t) {
            t.printStackTrace();
            return CommandResult.FAILURE;
        }
    }

    private static void getPlayerPermissions(PermissionManager manager, UUID uuid, Callback<List<Permission>> callback) {
        if (legacy) {
            try {
                getPlayerPermissionsMethod.invoke(manager, uuid, new LPResultRunnable<List<Permission>>() {
                    @Override
                    public void run() {
                        callback.onComplete(getResult());
                    }
                });
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            try {
                ListenableFuture<List<Permission>> lf = (ListenableFuture<List<Permission>>) getPlayerPermissionsMethod.invoke(manager, uuid);
                try {
                    if (lf.isDone()) {
                        callback.onComplete(lf.get());
                    } else {
                        lf.addListener(() -> {
                            try {
                                callback.onComplete(lf.get());
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }, Runnable::run);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private CommandResult run(LuckPermsPlugin plugin, List<String> args) {
        final Logger log = plugin.getLog();
        if (!plugin.isPluginLoaded("PowerfulPerms")) {
            log.severe("PowerfulPerms Migration: Error -> PowerfulPerms is not loaded.");
            return CommandResult.STATE_ERROR;
        }

        final String address = args.get(0);
        final String database = args.get(1);
        final String username = args.get(2);
        final String password = args.get(3);
        final String dbTable = args.get(4);

        // Find a list of UUIDs
        log.info("PowerfulPerms Migration: Getting a list of UUIDs to migrate.");

        @Cleanup HikariDataSource hikari = new HikariDataSource();
        hikari.setMaximumPoolSize(2);
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", address.split(":")[0]);
        hikari.addDataSourceProperty("port", address.split(":")[1]);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);

        Set<UUID> uuids = new HashSet<>();

        try {
            @Cleanup Connection connection = hikari.getConnection();
            DatabaseMetaData meta = connection.getMetaData();

            @Cleanup ResultSet tables = meta.getTables(null, null, dbTable, null);
            if (!tables.next()) {
                log.severe("PowerfulPerms Migration: Error - Couldn't find table.");
                return CommandResult.FAILURE;

            } else {
                @Cleanup PreparedStatement columnPs = connection.prepareStatement("SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?");
                columnPs.setString(1, dbTable);
                @Cleanup ResultSet columnRs = columnPs.executeQuery();

                log.info("Found table: " + dbTable);
                while (columnRs.next()) {
                    log.info("" + columnRs.getString("COLUMN_NAME") + " - " + columnRs.getString("COLUMN_TYPE"));
                }

                @Cleanup PreparedStatement preparedStatement = connection.prepareStatement("SELECT `uuid` FROM " + dbTable);
                @Cleanup ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    uuids.add(UUID.fromString(resultSet.getString("uuid")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CommandResult.FAILURE;
        }

        if (uuids.isEmpty()) {
            log.severe("PowerfulPerms Migration: Error - Unable to find any UUIDs to migrate.");
            return CommandResult.FAILURE;
        }

        log.info("PowerfulPerms Migration: Found " + uuids.size() + " uuids. Starting migration.");

        PowerfulPermsPlugin ppPlugin = (PowerfulPermsPlugin) plugin.getPlugin("PowerfulPerms");
        PermissionManager pm = ppPlugin.getPermissionManager();

        // Groups first.
        log.info("PowerfulPerms Migration: Starting group migration.");
        Map<Integer, Group> groups = pm.getGroups(); // All versions
        for (Group g : groups.values()) {
            plugin.getDatastore().createAndLoadGroup(g.getName().toLowerCase()).getOrDefault(false);
            final me.lucko.luckperms.common.groups.Group group = plugin.getGroupManager().get(g.getName().toLowerCase());
            try {
                LogEntry.build()
                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                        .acted(group).action("create")
                        .build().submit(plugin);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            for (Permission p : g.getOwnPermissions()) { // All versions
                applyPerm(group, p, plugin);
            }

            for (Group parent : g.getParents()) { // All versions
                try {
                    group.setPermission("group." + parent.getName().toLowerCase(), true);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(group).action("setinherit " + parent.getName().toLowerCase()) // All versions
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

            plugin.getDatastore().saveGroup(group);
        }
        log.info("PowerfulPerms Migration: Group migration complete.");

        // Now users.
        log.info("PowerfulPerms Migration: Starting user migration.");
        final Map<UUID, CountDownLatch> progress = new HashMap<>();

        // Migrate all users and their groups
        for (UUID uuid : uuids) {
            progress.put(uuid, new CountDownLatch(2));

            // Create a LuckPerms user for the UUID
            plugin.getDatastore().loadUser(uuid, "null").getOrDefault(false);
            User user = plugin.getUserManager().get(uuid);

            // Get a list of Permissions held by the user from the PP API.
            getPlayerPermissions(pm, uuid, perms -> { // Changes each version
                perms.forEach(p -> applyPerm(user, p, plugin));

                // Update the progress so the user can be saved and unloaded.
                synchronized (progress) {
                    progress.get(uuid).countDown();
                    if (progress.get(uuid).getCount() == 0) {
                        plugin.getDatastore().saveUser(user);
                        plugin.getUserManager().cleanup(user);
                    }
                }
            });

            // Migrate the user's groups to LuckPerms from PP.
            Callback<Map<String, List<CachedGroup>>> callback = groups1 -> {
                for (Map.Entry<String, List<CachedGroup>> e : groups1.entrySet()) {
                    final String server;
                    if (e.getKey() != null && (e.getKey().equals("") || e.getKey().equalsIgnoreCase("all"))) {
                        server = null;
                    } else {
                        server = e.getKey();
                    }

                    if (superLegacy) {
                        e.getValue().stream()
                                .filter(cg -> !cg.isNegated())
                                .map(cg -> {
                                    try {
                                        return (Group) getGroupMethod.invoke(cg);
                                    } catch (IllegalAccessException | InvocationTargetException e1) {
                                        e1.printStackTrace();
                                        return null;
                                    }
                                })
                                .forEach(g -> {
                                    if (g != null) {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + g.getName().toLowerCase(), true);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addgroup " + g.getName().toLowerCase())
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + g.getName().toLowerCase(), true, server);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addgroup " + g.getName().toLowerCase() + " " + server)
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                });
                    } else {
                        e.getValue().stream()
                                .filter(g -> !g.hasExpired() && !g.isNegated())
                                .forEach(g -> {
                                    final Group group = pm.getGroup(g.getGroupId());
                                    if (g.willExpire()) {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, g.getExpirationDate().getTime() / 1000L);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addtempgroup " + group.getName().toLowerCase() + " " + g.getExpirationDate().getTime() / 1000L)
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, server, g.getExpirationDate().getTime() / 1000L);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addtempgroup " + group.getName().toLowerCase() + " " + g.getExpirationDate().getTime() / 1000L + " " + server)
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        }

                                    } else {
                                        if (server == null) {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addgroup " + group.getName().toLowerCase())
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        } else {
                                            try {
                                                user.setPermission("group." + group.getName().toLowerCase(), true, server);
                                                LogEntry.build()
                                                        .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                                                        .acted(user).action("addgroup " + group.getName().toLowerCase() + " " + server)
                                                        .build().submit(plugin);
                                            } catch (Exception ex) {
                                                if (!(ex instanceof ObjectAlreadyHasException)) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                });
                    }
                }

                // Update the progress so the user can be saved and unloaded.
                synchronized (progress) {
                    progress.get(uuid).countDown();
                    if (progress.get(uuid).getCount() == 0) {
                        plugin.getDatastore().saveUser(user);
                        plugin.getUserManager().cleanup(user);
                    }
                }
            };

            if (!legacy) {
                try {
                    ListenableFuture<LinkedHashMap<String, List<CachedGroup>>> future = (ListenableFuture<LinkedHashMap<String, List<CachedGroup>>>) getPlayerGroupsMethod.invoke(pm, uuid);
                    try {
                        if (future.isDone()) {
                            callback.onComplete(future.get());
                        } else {
                            future.addListener(() -> {
                                try {
                                    callback.onComplete(future.get());
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            }, Runnable::run);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.info("PowerfulPerms Migration: Error");
                    e.printStackTrace();
                }
            } else {
                try {
                    getPlayerGroupsMethod.invoke(pm, uuid, new LPResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {
                        @Override
                        public void run() {
                            callback.onComplete(getResult());
                        }
                    });
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.info("PowerfulPerms Migration: Error");
                    e.printStackTrace();
                }
            }
        }

        // All groups are migrated, but there may still be some users being migrated.
        // This block will wait for all users to be completed.
        log.info("PowerfulPerms Migration: Waiting for user migration to complete. This may take some time");
        boolean sleep = true;
        while (sleep) {
            sleep = false;

            for (Map.Entry<UUID, CountDownLatch> e : progress.entrySet()) {
                if (e.getValue().getCount() != 0) {
                    sleep = true;
                    break;
                }
            }

            if (sleep) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        // We done.
        log.info("PowerfulPerms Migration: Success! Completed without any errors.");
        return CommandResult.SUCCESS;
    }

    private void applyPerm(PermissionHolder holder, Permission p, LuckPermsPlugin plugin) {
        String node = p.getPermissionString();
        boolean value = true;
        if (node.startsWith("!")) {
            node = node.substring(1);
            value = false;
        }

        String server = p.getServer();
        if (server != null && server.equalsIgnoreCase("all")) {
            server = null;
        }

        String world = p.getWorld();
        if (world != null && world.equalsIgnoreCase("all")) {
            world = null;
        }

        long expireAt = 0L;
        if (!superLegacy) {
            if (p.willExpire()) {
                expireAt = p.getExpirationDate().getTime() / 1000L;
            }
        }

        if (world != null && server == null) {
            server = "global";
        }

        if (world != null) {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value, server, world);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("set " + node + " " + value + " " + server + " " + world)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    holder.setPermission(node, value, server, world, expireAt);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("settemp " + node + " " + value + " " + expireAt + " " + server + " " + world)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }

        } else if (server != null) {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value, server);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("set " + node + " " + value + " " + server)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    holder.setPermission(node, value, server, expireAt);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("settemp " + node + " " + value + " " + expireAt + " " + server)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("set " + node + " " + value)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            } else {
                try {
                    holder.setPermission(node, value, expireAt);
                    LogEntry.build()
                            .actor(Constants.getConsoleUUID()).actorName(Constants.getConsoleName())
                            .acted(holder).action("settemp " + node + " " + value + " " + expireAt)
                            .build().submit(plugin);
                } catch (Exception ex) {
                    if (!(ex instanceof ObjectAlreadyHasException)) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
