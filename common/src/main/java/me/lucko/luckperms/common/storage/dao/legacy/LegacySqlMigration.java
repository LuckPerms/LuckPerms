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

package me.lucko.luckperms.common.storage.dao.legacy;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;

import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;
import me.lucko.luckperms.common.node.LegacyNodeFactory;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.storage.dao.sql.SqlDao;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class LegacySqlMigration implements Runnable {
    private static final Type NODE_MAP_TYPE = new TypeToken<Map<String, Boolean>>() {}.getType();
    private final SqlDao backing;

    @Override
    public void run() {
        backing.getPlugin().getLog().warn("Collecting UUID data from the old tables.");

        Map<UUID, String> uuidData = new HashMap<>();
        try (Connection c = backing.getProvider().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT uuid, name FROM lp_uuid")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            uuidData.put(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        backing.getPlugin().getLog().warn("Found " + uuidData.size() + " uuid data entries. Copying to new tables...");

        List<Map.Entry<UUID, String>> uuidEntries = new ArrayList<>(uuidData.entrySet());
        List<List<Map.Entry<UUID, String>>> partitionedUuidEntries = Lists.partition(uuidEntries, 100);

        for (List<Map.Entry<UUID, String>> l : partitionedUuidEntries) {
            try (Connection c = backing.getProvider().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("INSERT INTO {prefix}players VALUES(?, ?, ?)"))) {
                    for (Map.Entry<UUID, String> e : l) {
                        ps.setString(1, e.getKey().toString());
                        ps.setString(2, e.getValue().toLowerCase());
                        ps.setString(3, NodeFactory.DEFAULT_GROUP_NAME);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        uuidData.clear();
        uuidEntries.clear();
        partitionedUuidEntries.clear();

        backing.getPlugin().getLog().warn("Migrated all uuid data.");
        backing.getPlugin().getLog().warn("Starting user data migration.");

        Set<UUID> users = new HashSet<>();
        try (Connection c = backing.getProvider().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT uuid FROM lp_users")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            users.add(UUID.fromString(rs.getString("uuid")));
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        backing.getPlugin().getLog().warn("Found " + users.size() + " user data entries. Copying to new tables...");

        AtomicInteger userCounter = new AtomicInteger(0);
        for (UUID uuid : users) {
            String permsJson = null;
            String primaryGroup = null;

            try (Connection c = backing.getProvider().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement("SELECT primary_group, perms FROM lp_users WHERE uuid=?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            permsJson = rs.getString("perms");
                            primaryGroup = rs.getString("primary_group");
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (permsJson == null || primaryGroup == null) {
                new Throwable().printStackTrace();
                continue;
            }

            Map<String, Boolean> convertedPerms = backing.getGson().fromJson(permsJson, NODE_MAP_TYPE);
            if (convertedPerms == null) {
                new Throwable().printStackTrace();
                continue;
            }

            Set<NodeModel> nodes = convertedPerms.entrySet().stream()
                    .map(e -> LegacyNodeFactory.fromLegacyString(e.getKey(), e.getValue()))
                    .map(NodeModel::fromNode)
                    .collect(Collectors.toSet());

            try (Connection c = backing.getProvider().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("INSERT INTO {prefix}user_permissions(uuid, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)"))) {
                    for (NodeModel nd : nodes) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, nd.getPermission());
                        ps.setBoolean(3, nd.getValue());
                        ps.setString(4, nd.getServer());
                        ps.setString(5, nd.getWorld());
                        ps.setLong(6, nd.getExpiry());
                        ps.setString(7, backing.getGson().toJson(ContextSetJsonSerializer.serializeContextSet(nd.getContexts())));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (!primaryGroup.equalsIgnoreCase(NodeFactory.DEFAULT_GROUP_NAME)) {
                try (Connection c = backing.getProvider().getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("UPDATE {prefix}players SET primary_group=? WHERE uuid=?"))) {
                        ps.setString(1, primaryGroup);
                        ps.setString(2, uuid.toString());
                        ps.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            int i = userCounter.incrementAndGet();
            if (i % 100 == 0) {
                backing.getPlugin().getLog().warn("Migrated " + i + " users so far...");
            }
        }

        users.clear();

        backing.getPlugin().getLog().warn("Migrated all user data.");
        backing.getPlugin().getLog().warn("Starting group data migration.");

        Map<String, String> groupData = new HashMap<>();
        try (Connection c = backing.getProvider().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT name, perms FROM lp_groups")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groupData.put(rs.getString("name"), rs.getString("perms"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        backing.getPlugin().getLog().warn("Found " + groupData.size() + " group data entries. Copying to new tables...");
        for (Map.Entry<String, String> e : groupData.entrySet()) {
            String name = e.getKey();
            String permsJson = e.getValue();

            try (Connection c = backing.getProvider().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("INSERT INTO {prefix}groups VALUES(?)"))) {
                    ps.setString(1, name);
                    ps.execute();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            Map<String, Boolean> convertedPerms = backing.getGson().fromJson(permsJson, NODE_MAP_TYPE);
            if (convertedPerms == null) {
                new Throwable().printStackTrace();
                continue;
            }

            Set<NodeModel> nodes = convertedPerms.entrySet().stream()
                    .map(ent -> LegacyNodeFactory.fromLegacyString(ent.getKey(), ent.getValue()))
                    .map(NodeModel::fromNode)
                    .collect(Collectors.toSet());

            try (Connection c = backing.getProvider().getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("INSERT INTO {prefix}group_permissions(name, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)"))) {
                    for (NodeModel nd : nodes) {
                        ps.setString(1, name);
                        ps.setString(2, nd.getPermission());
                        ps.setBoolean(3, nd.getValue());
                        ps.setString(4, nd.getServer());
                        ps.setString(5, nd.getWorld());
                        ps.setLong(6, nd.getExpiry());
                        ps.setString(7, backing.getGson().toJson(ContextSetJsonSerializer.serializeContextSet(nd.getContexts())));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        groupData.clear();
        backing.getPlugin().getLog().warn("Migrated all group data.");

        backing.getPlugin().getLog().warn("Renaming action and track tables.");
        try (Connection c = backing.getProvider().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("DROP TABLE {prefix}actions"))) {
                ps.execute();
            }
            try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("ALTER TABLE lp_actions RENAME TO {prefix}actions"))) {
                ps.execute();
            }

            try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("DROP TABLE {prefix}tracks"))) {
                ps.execute();
            }
            try (PreparedStatement ps = c.prepareStatement(backing.getPrefix().apply("ALTER TABLE lp_tracks RENAME TO {prefix}tracks"))) {
                ps.execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        backing.getPlugin().getLog().warn("Legacy schema migration complete.");
    }
}
