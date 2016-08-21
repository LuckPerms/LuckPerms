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

import com.github.cheesesoftware.PowerfulPermsAPI.*;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.Logger;
import me.lucko.luckperms.commands.CommandResult;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.core.PermissionHolder;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static me.lucko.luckperms.constants.Permission.MIGRATION;

public class MigrationPowerfulPerms extends SubCommand<Object> {
    /* <sadness>
        What kind of API requires reflection to function with multiple versions...
        Doesn't that just defeat the whole god damn point of having an API in the first place?
        Whatever happened to the concept of depreciation and responsible API creation?
        I tried to keep reflection to a minimum, but in some places there's no other option.
        This class is a complete fucking mess for that reason. I sad now :(
       </sadness> */
    private static Method getPlayerGroupsMethod = null;
    private static Method getGroupMethod = null;
    private static boolean legacy = false;

    static {
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

        try {
            getGroupMethod = CachedGroup.class.getMethod("getGroup");
            getGroupMethod.setAccessible(true);
            legacy = true;
        } catch (NoSuchMethodException ignored) {
        }
    }


    public MigrationPowerfulPerms() {
        super("powerfulperms", "Migration from PowerfulPerms",
                "/%s migration powerfulperms <address> <database> <username> <password> <db table>", MIGRATION, Predicate.not(5));
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

        HikariDataSource hikari = new HikariDataSource();
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
                @Cleanup PreparedStatement preparedStatement = connection.prepareStatement("SELECT `uuid` FROM " + dbTable);
                ResultSet resultSet = preparedStatement.executeQuery();

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

        final Map<UUID, CountDownLatch> progress = new HashMap<>();

        // Migrate all users and their groups
        for (UUID uuid : uuids) {
            progress.put(uuid, new CountDownLatch(2));

            // Create a LuckPerms user for the UUID
            plugin.getDatastore().loadOrCreateUser(uuid, "null");
            User user = plugin.getUserManager().get(uuid);

            // Get a list of Permissions held by the user from the PP API.
            pm.getPlayerOwnPermissions(uuid, new LPResultRunnable<List<Permission>>() {
                @Override
                public void run() {
                    List<Permission> perms = this.getResult();
                    perms.forEach(p -> applyPerm(user, p));

                    // Update the progress so the user can be saved and unloaded.
                    synchronized (progress) {
                        progress.get(uuid).countDown();
                        if (progress.get(uuid).getCount() == 0) {
                            plugin.getDatastore().saveUser(user);
                            plugin.getUserManager().cleanup(user);
                        }
                    }

                }
            });

            // Migrate the user's groups to LuckPerms from PP.
            try {
                getPlayerGroupsMethod.invoke(pm, uuid, new LPResultRunnable<LinkedHashMap<String, List<CachedGroup>>>() {
                    @Override
                    public void run() {
                        Map<String, List<CachedGroup>> groups = getResult();

                        for (Map.Entry<String, List<CachedGroup>> e : groups.entrySet()) {
                            final String server;
                            if (e.getKey() != null && (e.getKey().equals("") || e.getKey().equalsIgnoreCase("all"))) {
                                server = null;
                            } else {
                                server = e.getKey();
                            }

                            // This is horrible. So many random API changes through versions, no depreciation.
                            if (legacy) {
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
                                                    } catch (ObjectAlreadyHasException ignored) {}
                                                } else {
                                                    try {
                                                        user.setPermission("group." + g.getName().toLowerCase(), true, server);
                                                    } catch (ObjectAlreadyHasException ignored) {}
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
                                                    } catch (ObjectAlreadyHasException ignored) {}
                                                } else {
                                                    try {
                                                        user.setPermission("group." + group.getName().toLowerCase(), true, server, g.getExpirationDate().getTime() / 1000L);
                                                    } catch (ObjectAlreadyHasException ignored) {}
                                                }

                                            } else {
                                                if (server == null) {
                                                    try {
                                                        user.setPermission("group." + group.getName().toLowerCase(), true);
                                                    } catch (ObjectAlreadyHasException ignored) {}
                                                } else {
                                                    try {
                                                        user.setPermission("group." + group.getName().toLowerCase(), true, server);
                                                    } catch (ObjectAlreadyHasException ignored) {}
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
                    }
                });
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.info("PowerfulPerms Migration: Error");
                e.printStackTrace();
            }
        }

        // The user processes will run individually in separate threads.
        // In the meantime, it's should be safe to load in the groups on this thread.
        log.info("PowerfulPerms Migration: User migration is now running. Starting group migration.");

        // Let's import groups. yay
        Map<Integer, Group> groups = pm.getGroups();
        for (Group g : groups.values()) {
            plugin.getDatastore().createAndLoadGroup(g.getName().toLowerCase());
            final me.lucko.luckperms.groups.Group group = plugin.getGroupManager().get(g.getName().toLowerCase());

            for (Permission p : g.getOwnPermissions()) {
                applyPerm(group, p);
            }

            for (Group parent : g.getParents()) {
                try {
                    group.setPermission("group." + parent.getName().toLowerCase(), true);
                } catch (ObjectAlreadyHasException ignored) {}
            }

            plugin.getDatastore().saveGroup(group);
        }

        // All groups are now migrated, but there may still be some users being migrated.
        // This block will wait for all users to be completed.
        log.info("PowerfulPerms Migration: All groups are now migrated. Waiting for user migration to complete.");
        log.info("PowerfulPerms Migration: This may take some time.");
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

    private void applyPerm(PermissionHolder holder, Permission p) {
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
        if (!legacy) {
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
                } catch (ObjectAlreadyHasException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    holder.setPermission(node, value, server, world, expireAt);
                } catch (ObjectAlreadyHasException e) {
                    e.printStackTrace();
                }
            }

        } else if (server != null) {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value, server);
                } catch (ObjectAlreadyHasException ignored) {}
            } else {
                try {
                    holder.setPermission(node, value, server, expireAt);
                } catch (ObjectAlreadyHasException ignored) {}
            }
        } else {
            if (expireAt == 0L) {
                try {
                    holder.setPermission(node, value);
                } catch (ObjectAlreadyHasException ignored) {}
            } else {
                try {
                    holder.setPermission(node, value, expireAt);
                } catch (ObjectAlreadyHasException ignored) {}
            }
        }
    }

    /**
     * Overrides the default ResultRunnable, callbacks will always run in the same thread. (an async one, hopefully.)
     * @param <T> type
     */
    @SuppressWarnings("WeakerAccess")
    public abstract class LPResultRunnable<T> extends ResultRunnable<T> {

        public LPResultRunnable() {
            super();
            super.sameThread = true;
        }

        public T getResult() {
            return super.result;
        }

    }
}
