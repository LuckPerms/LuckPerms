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

import com.github.gustav9797.PowerfulPermsAPI.CachedGroup;
import com.github.gustav9797.PowerfulPermsAPI.Group;
import com.github.gustav9797.PowerfulPermsAPI.Permission;
import com.github.gustav9797.PowerfulPermsAPI.PermissionManager;
import com.github.gustav9797.PowerfulPermsAPI.PowerfulPermsPlugin;
import com.google.common.collect.ImmutableSet;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.api.context.DefaultContextKeys;
import me.lucko.luckperms.api.event.cause.CreationCause;
import me.lucko.luckperms.api.node.NodeBuilder;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SubCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.commands.migration.MigrationUtils;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.storage.StorageType;
import me.lucko.luckperms.common.util.Iterators;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.ProgressLogger;

import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

// Only supports the latest versions of the PP API. (it seems to change randomly almost every release)
public class MigrationPowerfulPerms extends SubCommand<Object> {
    public MigrationPowerfulPerms(LocaleManager locale) {
        super(CommandSpec.MIGRATION_POWERFULPERMS.localize(locale), "powerfulperms", CommandPermission.MIGRATION, Predicates.not(5));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, Object o, List<String> args, String label) {
        ProgressLogger log = new ProgressLogger(Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS, "PowerfulPerms");
        log.addListener(plugin.getConsoleSender());
        log.addListener(sender);

        log.log("Starting.");

        if (!Bukkit.getPluginManager().isPluginEnabled("PowerfulPerms")) {
            log.logError("Plugin not loaded.");
            return CommandResult.STATE_ERROR;
        }

        StorageType type = plugin.getConfiguration().get(ConfigKeys.STORAGE_METHOD);
        if (type != StorageType.MYSQL) {
            // We need to load the Hikari/MySQL stuff.
            plugin.getDependencyManager().loadStorageDependencies(ImmutableSet.of(StorageType.MYSQL));
        }

        String address = args.get(0);
        String database = args.get(1);
        String username = args.get(2);
        String password = args.get(3);
        String dbTable = args.get(4);

        // Find a list of UUIDs
        log.log("Getting a list of UUIDs to migrate.");
        Set<UUID> uuids = new HashSet<>();

        try (HikariSupplier hikari = new HikariSupplier(address, database, username, password)) {
            hikari.setup("powerfulperms-migrator-pool");

            try (Connection c = hikari.getConnection()) {
                DatabaseMetaData meta = c.getMetaData();
                try (ResultSet rs = meta.getTables(null, null, dbTable, null)) {
                    if (!rs.next()) {
                        log.log("Error - Couldn't find table.");
                        return CommandResult.FAILURE;
                    }
                }
            }

            try (Connection c = hikari.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?")) {
                    ps.setString(1, dbTable);
                    try (ResultSet rs = ps.executeQuery()) {
                        log.log("Found table: " + dbTable);
                        while (rs.next()) {
                            log.log("" + rs.getString("COLUMN_NAME") + " - " + rs.getString("COLUMN_TYPE"));
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT `uuid` FROM " + dbTable)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            uuids.add(UUID.fromString(rs.getString("uuid")));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (uuids.isEmpty()) {
            log.logError("Unable to find any UUIDs to migrate.");
            return CommandResult.FAILURE;
        }

        log.log("Found " + uuids.size() + " uuids. Starting migration.");

        PowerfulPermsPlugin ppPlugin = (PowerfulPermsPlugin) Bukkit.getPluginManager().getPlugin("PowerfulPerms");
        PermissionManager pm = ppPlugin.getPermissionManager();

        Collection<Group> groups = pm.getGroups().values();

        AtomicInteger maxWeight = new AtomicInteger(0);

        // Groups first.
        log.log("Starting group migration.");
        AtomicInteger groupCount = new AtomicInteger(0);
        Iterators.iterate(groups, g -> {
            maxWeight.set(Math.max(maxWeight.get(), g.getRank()));

            String groupName = MigrationUtils.standardizeName(g.getName());
            me.lucko.luckperms.common.model.Group group = plugin.getStorage().createAndLoadGroup(groupName, CreationCause.INTERNAL).join();

            MigrationUtils.setGroupWeight(group, g.getRank());

            for (Permission p : g.getOwnPermissions()) {
                applyPerm(group, p);
            }

            for (Group parent : g.getParents()) {
                group.setPermission(NodeFactory.buildGroupNode(parent.getName().toLowerCase()).build());
            }

            // server --> prefix afaik
            for (Map.Entry<String, String> prefix : g.getPrefixes().entrySet()) {
                if (prefix.getValue().isEmpty()) continue;

                String server = prefix.getKey().toLowerCase();
                if (prefix.getKey().equals("*") || prefix.getKey().equals("all")) {
                    server = null;
                }

                if (server != null) {
                    group.setPermission(NodeFactory.buildPrefixNode(g.getRank(), prefix.getValue()).withContext(DefaultContextKeys.SERVER_KEY, server).build());
                } else {
                    group.setPermission(NodeFactory.buildPrefixNode(g.getRank(), prefix.getValue()).build());
                }
            }

            for (Map.Entry<String, String> suffix : g.getSuffixes().entrySet()) {
                if (suffix.getValue().isEmpty()) continue;

                String server = suffix.getKey().toLowerCase();
                if (suffix.getKey().equals("*") || suffix.getKey().equals("all")) {
                    server = null;
                }

                if (server != null) {
                    group.setPermission(NodeFactory.buildSuffixNode(g.getRank(), suffix.getValue()).withContext(DefaultContextKeys.SERVER_KEY, server).build());
                } else {
                    group.setPermission(NodeFactory.buildSuffixNode(g.getRank(), suffix.getValue()).build());
                }
            }

            plugin.getStorage().saveGroup(group);
            log.logAllProgress("Migrated {} groups so far.", groupCount.incrementAndGet());
        });
        log.log("Migrated " + groupCount.get() + " groups");

        // Migrate all users
        log.log("Starting user migration.");
        AtomicInteger userCount = new AtomicInteger(0);

        // Increment the max weight from the group migrations. All user meta should override.
        maxWeight.addAndGet(5);

        // Migrate all users and their groups
        Iterators.iterate(uuids, uuid -> {

            // Create a LuckPerms user for the UUID
            User user = plugin.getStorage().loadUser(uuid, null).join();

            List<Permission> permissions = joinFuture(pm.getPlayerOwnPermissions(uuid));

            for (Permission p : permissions) {
                applyPerm(user, p);
            }

            // server --> list of groups
            Map<String, List<CachedGroup>> parents = joinFuture(pm.getPlayerOwnGroups(uuid));
            for (Map.Entry<String, List<CachedGroup>> parent : parents.entrySet()) {
                String server = parent.getKey().toLowerCase();
                if (parent.getKey().equals("*") || parent.getKey().equals("all")) {
                    server = null;
                }

                for (CachedGroup group : parent.getValue()) {
                    applyGroup(pm, user, group, server);
                }
            }

            String prefix = joinFuture(pm.getPlayerOwnPrefix(uuid));
            String suffix = joinFuture(pm.getPlayerOwnSuffix(uuid));

            if (prefix != null && !prefix.isEmpty()) {
                user.setPermission(NodeFactory.buildPrefixNode(maxWeight.get(), prefix).build());
            }

            if (suffix != null && !suffix.isEmpty()) {
                user.setPermission(NodeFactory.buildSuffixNode(maxWeight.get(), suffix).build());
            }

            Group primaryGroup = joinFuture(pm.getPlayerPrimaryGroup(uuid));
            if (primaryGroup != null && primaryGroup.getName() != null) {
                String primary = primaryGroup.getName().toLowerCase();
                if (!primary.equals(NodeFactory.DEFAULT_GROUP_NAME)) {
                    user.setPermission(NodeFactory.buildGroupNode(primary).build());
                    user.getPrimaryGroup().setStoredValue(primary);
                }
            }

            plugin.getUserManager().cleanup(user);
            plugin.getStorage().saveUser(user);
            log.logProgress("Migrated {} users so far.", userCount.incrementAndGet(), ProgressLogger.DEFAULT_NOTIFY_FREQUENCY);
        });

        log.log("Migrated " + userCount.get() + " users.");
        log.log("Success! Migration complete.");
        return CommandResult.SUCCESS;
    }

    private void applyPerm(PermissionHolder holder, Permission p) {
        String node = p.getPermissionString();
        boolean value = true;
        if (node.startsWith("!") || node.startsWith("-")) {
            node = node.substring(1);
            value = false;
        }

        if (node.isEmpty()) {
            return;
        }

        String server = p.getServer();
        if (server != null && (server.equalsIgnoreCase("all") || server.equalsIgnoreCase("*"))) {
            server = null;
        }

        String world = p.getWorld();
        if (world != null && (world.equalsIgnoreCase("all") || world.equalsIgnoreCase("*"))) {
            world = null;
        }

        long expireAt = 0L;
        if (p.willExpire()) {
            expireAt = p.getExpirationDate().getTime() / 1000L;
        }

        if (world != null && server == null) {
            server = "global";
        }

        NodeBuilder nb = NodeFactory.builder(node).value(value);
        if (expireAt != 0) nb.expiry(expireAt);
        if (server != null) nb.withContext(DefaultContextKeys.SERVER_KEY, server);
        if (world != null) nb.withContext(DefaultContextKeys.WORLD_KEY, world);

        holder.setPermission(nb.build());
    }

    private void applyGroup(PermissionManager pm, PermissionHolder holder, CachedGroup g, String server) {
        Group group = pm.getGroup(g.getGroupId());
        String node = NodeFactory.groupNode(MigrationUtils.standardizeName(group.getName()));

        long expireAt = 0L;
        if (g.willExpire()) {
            expireAt = g.getExpirationDate().getTime() / 1000L;
        }

        NodeBuilder nb = NodeFactory.builder(node);

        if (expireAt != 0) {
            nb.expiry(expireAt);
        }

        if (server != null) {
            nb.withContext(DefaultContextKeys.SERVER_KEY, server);
        }

        holder.setPermission(nb.build());
    }

    private static <T> T joinFuture(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A simple hikari wrapper
     */
    public static final class HikariSupplier implements AutoCloseable {

        private final String host;
        private final String port;
        private final String database;
        private final String username;
        private final String password;

        private HikariDataSource hikari;

        public HikariSupplier(String address, String database, String username, String password) {
            this.database = database;
            this.username = username;
            this.password = password;

            String[] split = address.split(":");
            if (split.length != 2) {
                throw new IllegalArgumentException("Address argument should be in the format hostname:port");
            }

            this.host = split[0];
            this.port = split[1];
        }

        public void setup(String poolName) {
            this.hikari = new HikariDataSource();
            this.hikari.setPoolName(poolName);
            this.hikari.setMaximumPoolSize(2);
            this.hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
            this.hikari.addDataSourceProperty("serverName", this.host);
            this.hikari.addDataSourceProperty("port", this.port);
            this.hikari.addDataSourceProperty("databaseName", this.database);
            this.hikari.addDataSourceProperty("user", this.username);
            this.hikari.addDataSourceProperty("password", this.password);
        }

        @Override
        public void close() {
            this.hikari.close();
        }

        public Connection getConnection() throws SQLException {
            return this.hikari.getConnection();
        }
    }


}
