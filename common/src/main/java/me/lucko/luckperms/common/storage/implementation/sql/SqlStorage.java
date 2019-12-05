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

package me.lucko.luckperms.common.storage.implementation.sql;

import com.google.common.collect.Maps;
import com.google.gson.reflect.TypeToken;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.bulkupdate.PreparedStatementBuilder;
import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;
import me.lucko.luckperms.common.context.ContextSetJsonSerializer;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.node.model.HeldNodeImpl;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.sql.connection.ConnectionFactory;
import me.lucko.luckperms.common.storage.misc.PlayerSaveResultImpl;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.HeldNode;
import net.luckperms.api.node.Node;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SqlStorage implements StorageImplementation {
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_PERMISSIONS_SELECT = "SELECT id, permission, value, server, world, expiry, contexts FROM '{prefix}user_permissions' WHERE uuid=?";
    private static final String USER_PERMISSIONS_DELETE_SPECIFIC = "DELETE FROM '{prefix}user_permissions' WHERE id=?";
    private static final String USER_PERMISSIONS_DELETE = "DELETE FROM '{prefix}user_permissions' WHERE uuid=?";
    private static final String USER_PERMISSIONS_INSERT = "INSERT INTO '{prefix}user_permissions' (uuid, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String USER_PERMISSIONS_SELECT_DISTINCT = "SELECT DISTINCT uuid FROM '{prefix}user_permissions'";
    private static final String USER_PERMISSIONS_SELECT_PERMISSION = "SELECT uuid, permission, value, server, world, expiry, contexts FROM '{prefix}user_permissions' WHERE ";

    private static final String PLAYER_SELECT_UUID_BY_USERNAME = "SELECT uuid FROM '{prefix}players' WHERE username=? LIMIT 1";
    private static final String PLAYER_SELECT_USERNAME_BY_UUID = "SELECT username FROM '{prefix}players' WHERE uuid=? LIMIT 1";
    private static final String PLAYER_UPDATE_USERNAME_FOR_UUID = "UPDATE '{prefix}players' SET username=? WHERE uuid=?";
    private static final String PLAYER_INSERT = "INSERT INTO '{prefix}players' (uuid, username, primary_group) VALUES(?, ?, ?)";
    private static final String PLAYER_SELECT_ALL_UUIDS_BY_USERNAME = "SELECT uuid FROM '{prefix}players' WHERE username=? AND NOT uuid=?";
    private static final String PLAYER_DELETE_ALL_UUIDS_BY_USERNAME = "DELETE FROM '{prefix}players' WHERE username=? AND NOT uuid=?";
    private static final String PLAYER_SELECT_BY_UUID = "SELECT username, primary_group FROM '{prefix}players' WHERE uuid=?";
    private static final String PLAYER_SELECT_PRIMARY_GROUP_BY_UUID = "SELECT primary_group FROM '{prefix}players' WHERE uuid=? LIMIT 1";
    private static final String PLAYER_UPDATE_PRIMARY_GROUP_BY_UUID = "UPDATE '{prefix}players' SET primary_group=? WHERE uuid=?";

    private static final String GROUP_PERMISSIONS_SELECT = "SELECT id, permission, value, server, world, expiry, contexts FROM '{prefix}group_permissions' WHERE name=?";
    private static final String GROUP_PERMISSIONS_DELETE_SPECIFIC = "DELETE FROM '{prefix}group_permissions' WHERE id=?";
    private static final String GROUP_PERMISSIONS_DELETE = "DELETE FROM '{prefix}group_permissions' WHERE name=?";
    private static final String GROUP_PERMISSIONS_INSERT = "INSERT INTO '{prefix}group_permissions' (name, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String GROUP_PERMISSIONS_SELECT_PERMISSION = "SELECT name, permission, value, server, world, expiry, contexts FROM '{prefix}group_permissions' WHERE ";

    private static final String GROUP_SELECT_ALL = "SELECT name FROM '{prefix}groups'";
    private static final String MYSQL_GROUP_INSERT = "INSERT INTO '{prefix}groups' (name) VALUES(?) ON DUPLICATE KEY UPDATE name=name";
    private static final String H2_GROUP_INSERT = "MERGE INTO '{prefix}groups' (name) VALUES(?)";
    private static final String SQLITE_GROUP_INSERT = "INSERT OR IGNORE INTO '{prefix}groups' (name) VALUES(?)";
    private static final String POSTGRESQL_GROUP_INSERT = "INSERT INTO '{prefix}groups' (name) VALUES(?) ON CONFLICT (name) DO NOTHING";
    private static final String GROUP_DELETE = "DELETE FROM '{prefix}groups' WHERE name=?";

    private static final String TRACK_INSERT = "INSERT INTO '{prefix}tracks' (name, 'groups') VALUES(?, ?)";
    private static final String TRACK_SELECT = "SELECT 'groups' FROM '{prefix}tracks' WHERE name=?";
    private static final String TRACK_SELECT_ALL = "SELECT * FROM '{prefix}tracks'";
    private static final String TRACK_UPDATE = "UPDATE '{prefix}tracks' SET 'groups'=? WHERE name=?";
    private static final String TRACK_DELETE = "DELETE FROM '{prefix}tracks' WHERE name=?";

    private static final String ACTION_INSERT = "INSERT INTO '{prefix}actions' (time, actor_uuid, actor_name, type, acted_uuid, acted_name, action) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTION_SELECT_ALL = "SELECT * FROM '{prefix}actions'";

    private final LuckPermsPlugin plugin;
    
    private final ConnectionFactory connectionFactory;
    private final Function<String, String> statementProcessor;

    public SqlStorage(LuckPermsPlugin plugin, ConnectionFactory connectionFactory, String tablePrefix) {
        this.plugin = plugin;
        this.connectionFactory = connectionFactory;
        this.statementProcessor = connectionFactory.getStatementProcessor().compose(s -> s.replace("{prefix}", tablePrefix));
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return this.connectionFactory.getImplementationName();
    }

    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    public Function<String, String> getStatementProcessor() {
        return this.statementProcessor;
    }

    private boolean tableExists(String table) throws SQLException {
        try (Connection connection = this.connectionFactory.getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", null)) {
                while (rs.next()) {
                    if (rs.getString(3).equalsIgnoreCase(table)) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    @Override
    public void init() throws Exception {
        this.connectionFactory.init();

        // Init tables
        if (!tableExists(this.statementProcessor.apply("{prefix}user_permissions"))) {
            String schemaFileName = "me/lucko/luckperms/schema/" + this.connectionFactory.getImplementationName().toLowerCase() + ".sql";
            try (InputStream is = this.plugin.getBootstrap().getResourceStream(schemaFileName)) {
                if (is == null) {
                    throw new Exception("Couldn't locate schema file for " + this.connectionFactory.getImplementationName());
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    List<String> queries = new LinkedList<>();
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--") || line.startsWith("#")) {
                            continue;
                        }

                        sb.append(line);

                        // check for end of declaration
                        if (line.endsWith(";")) {
                            sb.deleteCharAt(sb.length() - 1);

                            String result = this.statementProcessor.apply(sb.toString().trim());
                            if (!result.isEmpty()) {
                                queries.add(result);
                            }

                            // reset
                            sb = new StringBuilder();
                        }
                    }

                    try (Connection connection = this.connectionFactory.getConnection()) {
                        boolean utf8mb4Unsupported = false;

                        try (Statement s = connection.createStatement()) {
                            for (String query : queries) {
                                s.addBatch(query);
                            }

                            try {
                                s.executeBatch();
                            } catch (BatchUpdateException e) {
                                if (e.getMessage().contains("Unknown character set")) {
                                    utf8mb4Unsupported = true;
                                } else {
                                    throw e;
                                }
                            }
                        }

                        // try again
                        if (utf8mb4Unsupported) {
                            try (Statement s = connection.createStatement()) {
                                for (String query : queries) {
                                    s.addBatch(query.replace("utf8mb4", "utf8"));
                                }

                                s.executeBatch();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            this.connectionFactory.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getMeta() {
        return this.connectionFactory.getMeta();
    }

    @Override
    public void logAction(Action entry) throws SQLException {
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(ACTION_INSERT))) {
                ps.setLong(1, entry.getTimestamp().getEpochSecond());
                ps.setString(2, entry.getSource().getUniqueId().toString());
                ps.setString(3, entry.getSource().getName());
                ps.setString(4, Character.toString(LoggedAction.getTypeCharacter(entry.getTarget().getType())));
                ps.setString(5, entry.getTarget().getUniqueId().map(UUID::toString).orElse("null"));
                ps.setString(6, entry.getTarget().getName());
                ps.setString(7, entry.getDescription());
                ps.execute();
            }
        }
    }

    @Override
    public Log getLog() throws SQLException {
        final Log.Builder log = Log.builder();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(ACTION_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String actedUuid = rs.getString("acted_uuid");
                        LoggedAction e = LoggedAction.build()
                                .timestamp(Instant.ofEpochSecond(rs.getLong("time")))
                                .source(UUID.fromString(rs.getString("actor_uuid")))
                                .sourceName(rs.getString("actor_name"))
                                .targetType(LoggedAction.parseTypeCharacter(rs.getString("type").toCharArray()[0]))
                                .target(actedUuid.equals("null") ? null : UUID.fromString(actedUuid))
                                .targetName(rs.getString("acted_name"))
                                .description(rs.getString("action"))
                                .build();
                        
                        log.add(e);
                    }
                }
            }
        }
        return log.build();
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) throws SQLException {
        try (Connection c = this.connectionFactory.getConnection()) {
            if (bulkUpdate.getDataType().isIncludingUsers()) {
                String table = this.statementProcessor.apply("{prefix}user_permissions");
                try (PreparedStatement ps = bulkUpdate.buildAsSql().build(c, q -> q.replace("{table}", table))) {
                    ps.execute();
                }
            }

            if (bulkUpdate.getDataType().isIncludingGroups()) {
                String table = this.statementProcessor.apply("{prefix}group_permissions");
                try (PreparedStatement ps = bulkUpdate.buildAsSql().build(c, q -> q.replace("{table}", table))) {
                    ps.execute();
                }
            }
        }
    }

    @Override
    public User loadUser(UUID uniqueId, String username) throws SQLException {
        User user = this.plugin.getUserManager().getOrMake(uniqueId, username);
        user.getIoLock().lock();
        try {
            List<SqlNode> data = new ArrayList<>();
            String primaryGroup = null;
            String userName = null;

            // Collect user permissions
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_SELECT))) {
                    ps.setString(1, user.getUniqueId().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(SqlNode.fromSqlFields(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            }

            // Collect user meta (username & primary group)
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_SELECT_BY_UUID))) {
                    ps.setString(1, user.getUniqueId().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userName = rs.getString("username");
                            primaryGroup = rs.getString("primary_group");
                        }
                    }
                }
            }

            // update username & primary group
            if (primaryGroup == null) {
                primaryGroup = GroupManager.DEFAULT_GROUP_NAME;
            }
            user.getPrimaryGroup().setStoredValue(primaryGroup);

            // Update their username to what was in the storage if the one in the local instance is null
            user.setUsername(userName, true);

            // If the user has any data in storage
            if (!data.isEmpty()) {
                Set<Node> nodes = data.stream().map(SqlNode::toNode).collect(Collectors.toSet());
                user.setNodes(DataType.NORMAL, nodes);

                // Save back to the store if data they were given any defaults or had permissions expire
                if (this.plugin.getUserManager().giveDefaultIfNeeded(user, false) | user.auditTemporaryNodes()) {
                    // This should be fine, as the lock will be acquired by the same thread.
                    saveUser(user);
                }

            } else {
                // User has no data in storage.
                if (this.plugin.getUserManager().shouldSave(user)) {
                    user.clearNodes(DataType.NORMAL, null, true);
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } finally {
            user.getIoLock().unlock();
        }
        return user;
    }

    @Override
    public void saveUser(User user) throws SQLException {
        user.getIoLock().lock();
        try {
            // Empty data - just delete from the DB.
            if (!this.plugin.getUserManager().shouldSave(user)) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_DELETE))) {
                        ps.setString(1, user.getUniqueId().toString());
                        ps.execute();
                    }
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_UPDATE_PRIMARY_GROUP_BY_UUID))) {
                        ps.setString(1, GroupManager.DEFAULT_GROUP_NAME);
                        ps.setString(2, user.getUniqueId().toString());
                        ps.execute();
                    }
                }
                return;
            }

            // Get a snapshot of current data.
            Set<SqlNode> remote = new HashSet<>();
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_SELECT))) {
                    ps.setString(1, user.getUniqueId().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            remote.add(SqlNode.fromSqlFields(id, permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            }

            Set<SqlNode> local = user.normalData().immutable().values().stream().map(SqlNode::fromNode).collect(Collectors.toSet());

            Map.Entry<Set<SqlNode>, Set<SqlNode>> diff = compareSets(local, remote);

            Set<SqlNode> toAdd = diff.getKey();
            Set<SqlNode> toRemove = diff.getValue();

            if (!toRemove.isEmpty()) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_DELETE_SPECIFIC))) {
                        for (SqlNode nd : toRemove) {
                            ps.setLong(1, nd.getSqlId());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }

            if (!toAdd.isEmpty()) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_INSERT))) {
                        for (SqlNode nd : toAdd) {
                            ps.setString(1, user.getUniqueId().toString());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.getValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, GsonProvider.normal().toJson(ContextSetJsonSerializer.serializeContextSet(nd.getContexts())));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }

            try (Connection c = this.connectionFactory.getConnection()) {
                boolean hasPrimaryGroupSaved;

                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_SELECT_PRIMARY_GROUP_BY_UUID))) {
                    ps.setString(1, user.getUniqueId().toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        hasPrimaryGroupSaved = rs.next();
                    }
                }

                if (hasPrimaryGroupSaved) {
                    // update
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_UPDATE_PRIMARY_GROUP_BY_UUID))) {
                        ps.setString(1, user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME));
                        ps.setString(2, user.getUniqueId().toString());
                        ps.execute();
                    }
                } else {
                    // insert
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_INSERT))) {
                        ps.setString(1, user.getUniqueId().toString());
                        ps.setString(2, user.getUsername().orElse("null").toLowerCase());
                        ps.setString(3, user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME));
                        ps.execute();
                    }
                }

            }
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() throws SQLException {
        Set<UUID> uuids = new HashSet<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(USER_PERMISSIONS_SELECT_DISTINCT))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        uuids.add(UUID.fromString(uuid));
                    }
                }
            }
        }
        return uuids;
    }

    @Override
    public List<HeldNode<UUID>> getUsersWithPermission(Constraint constraint) throws SQLException {
        PreparedStatementBuilder builder = new PreparedStatementBuilder().append(USER_PERMISSIONS_SELECT_PERMISSION);
        constraint.appendSql(builder, "permission");

        List<HeldNode<UUID>> held = new ArrayList<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = builder.build(c, this.statementProcessor)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID holder = UUID.fromString(rs.getString("uuid"));
                        String perm = rs.getString("permission");
                        boolean value = rs.getBoolean("value");
                        String server = rs.getString("server");
                        String world = rs.getString("world");
                        long expiry = rs.getLong("expiry");
                        String contexts = rs.getString("contexts");

                        SqlNode data = SqlNode.fromSqlFields(perm, value, server, world, expiry, contexts);
                        held.add(HeldNodeImpl.of(holder, data.toNode()));
                    }
                }
            }
        }
        return held;
    }

    @Override
    public Group createAndLoadGroup(String name) throws SQLException {
        String query;
        switch (this.connectionFactory.getImplementationName()) {
            case "H2":
                query = H2_GROUP_INSERT;
                break;
            case "SQLite":
                query = SQLITE_GROUP_INSERT;
                break;
            case "PostgreSQL":
                query = POSTGRESQL_GROUP_INSERT;
                break;
            default:
                query = MYSQL_GROUP_INSERT;
                break;
        }

        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(query))) {
                ps.setString(1, name);
                ps.execute();
            }
        }

        return loadGroup(name).get();
    }

    @Override
    public Optional<Group> loadGroup(String name) throws SQLException {
        // Check the group actually exists
        List<String> groups = new ArrayList<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                }
            }

        }

        if (!groups.contains(name)) {
            return Optional.empty();
        }

        Group group = this.plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            List<SqlNode> data = new ArrayList<>();

            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_SELECT))) {
                    ps.setString(1, group.getName());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(SqlNode.fromSqlFields(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            }

            if (!data.isEmpty()) {
                Set<Node> nodes = data.stream().map(SqlNode::toNode).collect(Collectors.toSet());
                group.setNodes(DataType.NORMAL, nodes);
            } else {
                group.clearNodes(DataType.NORMAL, null, false);
            }
        } finally {
            group.getIoLock().unlock();
        }
        return Optional.of(group);
    }

    @Override
    public void loadAllGroups() throws SQLException {
        List<String> groups = new ArrayList<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                }
            }
        }

        boolean success = true;
        for (String g : groups) {
            try {
                loadGroup(g);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("Exception occurred whilst loading a group");
        }

        GroupManager<?> gm = this.plugin.getGroupManager();
        gm.getAll().values().stream()
                .map(Group::getName)
                .filter(g -> !groups.contains(g))
                .forEach(gm::unload);
    }

    @Override
    public void saveGroup(Group group) throws SQLException {
        group.getIoLock().lock();
        try {
            // Empty data, just delete.
            if (group.normalData().immutable().isEmpty()) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_DELETE))) {
                        ps.setString(1, group.getName());
                        ps.execute();
                    }
                }
                return;
            }

            // Get a snapshot of current data
            Set<SqlNode> remote = new HashSet<>();
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_SELECT))) {
                    ps.setString(1, group.getName());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            long id = rs.getLong("id");
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            remote.add(SqlNode.fromSqlFields(id, permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            }

            Set<SqlNode> local = group.normalData().immutable().values().stream().map(SqlNode::fromNode).collect(Collectors.toSet());

            Map.Entry<Set<SqlNode>, Set<SqlNode>> diff = compareSets(local, remote);

            Set<SqlNode> toAdd = diff.getKey();
            Set<SqlNode> toRemove = diff.getValue();

            if (!toRemove.isEmpty()) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_DELETE_SPECIFIC))) {
                        for (SqlNode nd : toRemove) {
                            ps.setLong(1, nd.getSqlId());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }

            if (!toAdd.isEmpty()) {
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_INSERT))) {
                        for (SqlNode nd : toAdd) {
                            ps.setString(1, group.getName());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.getValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, GsonProvider.normal().toJson(ContextSetJsonSerializer.serializeContextSet(nd.getContexts())));
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
            }
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public void deleteGroup(Group group) throws SQLException {
        group.getIoLock().lock();
        try {
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_PERMISSIONS_DELETE))) {
                    ps.setString(1, group.getName());
                    ps.execute();
                }

                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(GROUP_DELETE))) {
                    ps.setString(1, group.getName());
                    ps.execute();
                }
            }
        } finally {
            group.getIoLock().unlock();
        }

        this.plugin.getGroupManager().unload(group.getName());
    }

    @Override
    public List<HeldNode<String>> getGroupsWithPermission(Constraint constraint) throws SQLException {
        PreparedStatementBuilder builder = new PreparedStatementBuilder().append(GROUP_PERMISSIONS_SELECT_PERMISSION);
        constraint.appendSql(builder, "permission");

        List<HeldNode<String>> held = new ArrayList<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = builder.build(c, this.statementProcessor)) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String holder = rs.getString("name");
                        String perm = rs.getString("permission");
                        boolean value = rs.getBoolean("value");
                        String server = rs.getString("server");
                        String world = rs.getString("world");
                        long expiry = rs.getLong("expiry");
                        String contexts = rs.getString("contexts");

                        SqlNode data = SqlNode.fromSqlFields(perm, value, server, world, expiry, contexts);
                        held.add(HeldNodeImpl.of(holder, data.toNode()));
                    }
                }
            }
        }
        return held;
    }

    @Override
    public Track createAndLoadTrack(String name) throws SQLException {
        Track track = this.plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            boolean exists = false;
            String groups = null;
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_SELECT))) {
                    ps.setString(1, track.getName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            exists = true;
                            groups = rs.getString("groups");
                        }
                    }
                }
            }

            if (exists) {
                // Track exists, let's load.
                track.setGroups(GsonProvider.normal().fromJson(groups, LIST_STRING_TYPE));
            } else {
                String json = GsonProvider.normal().toJson(track.getGroups());
                try (Connection c = this.connectionFactory.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_INSERT))) {
                        ps.setString(1, track.getName());
                        ps.setString(2, json);
                        ps.execute();
                    }
                }
            }
        } finally {
            track.getIoLock().unlock();
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) throws SQLException {
        Track track = this.plugin.getTrackManager().getIfLoaded(name);
        if (track != null) {
            track.getIoLock().lock();
        }
        try {
            String groups;
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_SELECT))) {
                    ps.setString(1, name);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            groups = rs.getString("groups");
                        } else {
                            return Optional.empty();
                        }
                    }
                }
            }

            if (track == null) {
                track = this.plugin.getTrackManager().getOrMake(name);
                track.getIoLock().lock();
            }

            track.setGroups(GsonProvider.normal().fromJson(groups, LIST_STRING_TYPE));
            return Optional.of(track);

        } finally {
            if (track != null) {
                track.getIoLock().unlock();
            }
        }
    }

    @Override
    public void loadAllTracks() throws SQLException {
        List<String> tracks = new ArrayList<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tracks.add(rs.getString("name").toLowerCase());
                    }
                }
            }
        }

        boolean success = true;
        for (String t : tracks) {
            try {
                loadTrack(t);
            } catch (Exception e) {
                e.printStackTrace();
                success = false;
            }
        }

        if (!success) {
            throw new RuntimeException("Exception occurred whilst loading a track");
        }

        TrackManager<?> tm = this.plugin.getTrackManager();
        tm.getAll().values().stream()
                .map(Track::getName)
                .filter(t -> !tracks.contains(t))
                .forEach(tm::unload);
    }

    @Override
    public void saveTrack(Track track) throws SQLException {
        track.getIoLock().lock();
        try {
            String s = GsonProvider.normal().toJson(track.getGroups());
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_UPDATE))) {
                    ps.setString(1, s);
                    ps.setString(2, track.getName());
                    ps.execute();
                }
            }
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void deleteTrack(Track track) throws SQLException {
        track.getIoLock().lock();
        try {
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(TRACK_DELETE))) {
                    ps.setString(1, track.getName());
                    ps.execute();
                }
            }
        } finally {
            track.getIoLock().unlock();
        }

        this.plugin.getTrackManager().unload(track.getName());
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) throws SQLException {
        username = username.toLowerCase();

        // find any existing mapping
        String oldUsername = getPlayerName(uniqueId);

        // do the insert
        if (!username.equals(oldUsername)) {
            try (Connection c = this.connectionFactory.getConnection()) {
                if (oldUsername != null) {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_UPDATE_USERNAME_FOR_UUID))) {
                        ps.setString(1, username);
                        ps.setString(2, uniqueId.toString());
                        ps.execute();
                    }
                } else {
                    try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_INSERT))) {
                        ps.setString(1, uniqueId.toString());
                        ps.setString(2, username);
                        ps.setString(3, GroupManager.DEFAULT_GROUP_NAME);
                        ps.execute();
                    }
                }
            }
        }

        PlayerSaveResultImpl result = PlayerSaveResultImpl.determineBaseResult(username, oldUsername);

        Set<UUID> conflicting = new HashSet<>();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_SELECT_ALL_UUIDS_BY_USERNAME))) {
                ps.setString(1, username);
                ps.setString(2, uniqueId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        conflicting.add(UUID.fromString(rs.getString("uuid")));
                    }
                }
            }
        }

        if (!conflicting.isEmpty()) {
            // remove the mappings for conflicting uuids
            try (Connection c = this.connectionFactory.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_DELETE_ALL_UUIDS_BY_USERNAME))) {
                    ps.setString(1, username);
                    ps.setString(2, uniqueId.toString());
                    ps.execute();
                }
            }
            result = result.withOtherUuidsPresent(conflicting);
        }

        return result;
    }

    @Override
    public UUID getPlayerUniqueId(String username) throws SQLException {
        username = username.toLowerCase();
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_SELECT_UUID_BY_USERNAME))) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return UUID.fromString(rs.getString("uuid"));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getPlayerName(UUID uniqueId) throws SQLException {
        try (Connection c = this.connectionFactory.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(this.statementProcessor.apply(PLAYER_SELECT_USERNAME_BY_UUID))) {
                ps.setString(1, uniqueId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                }
            }
        }
        return null;
    }

    /**
     * Compares two sets
     * @param local the local set
     * @param remote the remote set
     * @return the entries to add to remote, and the entries to remove from remote
     */
    private static Map.Entry<Set<SqlNode>, Set<SqlNode>> compareSets(Set<SqlNode> local, Set<SqlNode> remote) {
        // entries in local but not remote need to be added
        // entries in remote but not local need to be removed

        Set<SqlNode> toAdd = new HashSet<>(local);
        toAdd.removeAll(remote);

        Set<SqlNode> toRemove = new HashSet<>(remote);
        toRemove.removeAll(local);

        return Maps.immutableEntry(toAdd, toRemove);
    }

}
