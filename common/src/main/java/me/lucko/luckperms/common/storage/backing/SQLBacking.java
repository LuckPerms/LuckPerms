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

package me.lucko.luckperms.common.storage.backing;

import lombok.Getter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.backing.sqlprovider.SQLProvider;
import me.lucko.luckperms.common.storage.backing.utils.LegacySQLSchemaMigration;
import me.lucko.luckperms.common.storage.backing.utils.NodeDataHolder;
import me.lucko.luckperms.common.storage.holder.NodeHeldPermission;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLBacking extends AbstractBacking {
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_PERMISSIONS_SELECT = "SELECT permission, value, server, world, expiry, contexts FROM {prefix}user_permissions WHERE uuid=?";
    private static final String USER_PERMISSIONS_DELETE_SPECIFIC = "DELETE FROM {prefix}user_permissions WHERE uuid=? AND permission=? AND value=? AND server=? AND world=? AND expiry=? AND contexts=?";
    private static final String USER_PERMISSIONS_DELETE = "DELETE FROM {prefix}user_permissions WHERE uuid=?";
    private static final String USER_PERMISSIONS_INSERT = "INSERT INTO {prefix}user_permissions(uuid, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String USER_PERMISSIONS_SELECT_DISTINCT = "SELECT DISTINCT uuid FROM {prefix}user_permissions";
    private static final String USER_PERMISSIONS_SELECT_PERMISSION = "SELECT uuid, value, server, world, expiry, contexts FROM {prefix}user_permissions WHERE permission=?";

    private static final String PLAYER_SELECT = "SELECT username, primary_group FROM {prefix}players WHERE uuid=?";
    private static final String PLAYER_SELECT_UUID = "SELECT uuid FROM {prefix}players WHERE username=? LIMIT 1";
    private static final String PLAYER_SELECT_USERNAME = "SELECT username FROM {prefix}players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_INSERT = "INSERT INTO {prefix}players VALUES(?, ?, ?)";
    private static final String PLAYER_UPDATE = "UPDATE {prefix}players SET username=? WHERE uuid=?";
    private static final String PLAYER_DELETE = "DELETE FROM {prefix}players WHERE username=? AND NOT uuid=?";
    private static final String PLAYER_UPDATE_PRIMARY_GROUP = "UPDATE {prefix}players SET primary_group=? WHERE uuid=?";

    private static final String GROUP_PERMISSIONS_SELECT = "SELECT permission, value, server, world, expiry, contexts FROM {prefix}group_permissions WHERE name=?";
    private static final String GROUP_PERMISSIONS_DELETE = "DELETE FROM {prefix}group_permissions WHERE name=?";
    private static final String GROUP_PERMISSIONS_DELETE_SPECIFIC = "DELETE FROM {prefix}group_permissions WHERE name=? AND permission=? AND value=? AND server=? AND world=? AND expiry=? AND contexts=?";
    private static final String GROUP_PERMISSIONS_INSERT = "INSERT INTO {prefix}group_permissions(name, permission, value, server, world, expiry, contexts) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String GROUP_PERMISSIONS_SELECT_PERMISSION = "SELECT name, value, server, world, expiry, contexts FROM {prefix}group_permissions WHERE permission=?";

    private static final String GROUP_SELECT_ALL = "SELECT name FROM {prefix}groups";
    private static final String GROUP_INSERT = "INSERT INTO {prefix}groups VALUES(?)";
    private static final String GROUP_DELETE = "DELETE FROM {prefix}groups WHERE name=?";

    private static final String TRACK_INSERT = "INSERT INTO {prefix}tracks VALUES(?, ?)";
    private static final String TRACK_SELECT = "SELECT groups FROM {prefix}tracks WHERE name=?";
    private static final String TRACK_SELECT_ALL = "SELECT * FROM {prefix}tracks";
    private static final String TRACK_UPDATE = "UPDATE {prefix}tracks SET groups=? WHERE name=?";
    private static final String TRACK_DELETE = "DELETE FROM {prefix}tracks WHERE name=?";

    private static final String ACTION_INSERT = "INSERT INTO {prefix}actions(time, actor_uuid, actor_name, type, acted_uuid, acted_name, action) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTION_SELECT_ALL = "SELECT * FROM {prefix}actions";


    @Getter
    private final Gson gson;

    @Getter
    private final SQLProvider provider;
    @Getter
    private final Function<String, String> prefix;

    public SQLBacking(LuckPermsPlugin plugin, SQLProvider provider, String prefix) {
        super(plugin, provider.getName());
        this.provider = provider;
        this.prefix = s -> s.replace("{prefix}", prefix);
        gson = new Gson();
    }

    private boolean tableExists(String table) throws SQLException {
        try (Connection connection = provider.getConnection()) {
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
    public void init() {
        try {
            provider.init();

            // Init tables
            if (!tableExists(prefix.apply("{prefix}user_permissions"))) {
                String schemaFileName = "lp-schema-" + provider.getName().toLowerCase() + ".sql";
                try (InputStream is = plugin.getResourceStream(schemaFileName)) {
                    if (is == null) {
                        throw new Exception("Couldn't locate schema file for " + provider.getName());
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        try (Connection connection = provider.getConnection()) {
                            try (Statement s = connection.createStatement()) {
                                StringBuilder sb = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (line.startsWith("--") || line.startsWith("#")) continue;

                                    sb.append(line);

                                    // check for end of declaration
                                    if (line.endsWith(";")) {
                                        sb.deleteCharAt(sb.length() - 1);

                                        String result = prefix.apply(sb.toString().trim());
                                        if (!result.isEmpty()) s.addBatch(result);

                                        // reset
                                        sb = new StringBuilder();
                                    }
                                }
                                s.executeBatch();
                            }
                        }
                    }
                }

                // Try migration from legacy backing
                if (tableExists("lp_users")) {
                    plugin.getLog().severe("===== Legacy Schema Migration =====");
                    plugin.getLog().severe("Starting migration from legacy schema. This could take a while....");
                    plugin.getLog().severe("Please do not stop your server while the migration takes place.");

                    new LegacySQLSchemaMigration(this).run();
                }
            }

            setAcceptingLogins(true);
        } catch (Exception e) {
            e.printStackTrace();
            plugin.getLog().severe("Error occurred whilst initialising the database.");
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        try {
            provider.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean logAction(LogEntry entry) {
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(ACTION_INSERT))) {
                ps.setLong(1, entry.getTimestamp());
                ps.setString(2, entry.getActor().toString());
                ps.setString(3, entry.getActorName());
                ps.setString(4, Character.toString(entry.getType()));
                ps.setString(5, entry.getActed() == null ? "null" : entry.getActed().toString());
                ps.setString(6, entry.getActedName());
                ps.setString(7, entry.getAction());
                ps.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public Log getLog() {
        final Log.Builder log = Log.builder();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(ACTION_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        final String actedUuid = rs.getString("acted_uuid");
                        LogEntry e = new LogEntry(
                                rs.getLong("time"),
                                UUID.fromString(rs.getString("actor_uuid")),
                                rs.getString("actor_name"),
                                rs.getString("type").toCharArray()[0],
                                actedUuid.equals("null") ? null : UUID.fromString(actedUuid),
                                rs.getString("acted_name"),
                                rs.getString("action")
                        );
                        log.add(e);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return log.build();
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            List<NodeDataHolder> data = new ArrayList<>();
            AtomicReference<String> primaryGroup = new AtomicReference<>(null);
            AtomicReference<String> userName = new AtomicReference<>(null);

            // Collect user permissions
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_SELECT))) {
                    ps.setString(1, user.getUuid().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            // Collect user meta (username & primary group)
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_SELECT))) {
                    ps.setString(1, user.getUuid().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            userName.set(rs.getString("username"));
                            primaryGroup.set(rs.getString("primary_group"));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            // update username & primary group
            String pg = primaryGroup.get();
            if (pg == null) {
                pg = "default";
            }
            user.getPrimaryGroup().setStoredValue(pg);

            String name = userName.get();
            if (name == null) {
                name = "null";
            }

            // Update their username to what was in the storage if the one in the local instance is null
            if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                user.setName(name);
            }

            // If the user has any data in storage
            if (!data.isEmpty()) {
                Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                user.setNodes(nodes);

                // Save back to the store if data was changed
                if (plugin.getUserManager().giveDefaultIfNeeded(user, false)) {
                    // This should be fine, as the lock will be acquired by the same thread.
                    saveUser(user);
                }

            } else {
                // User has no data in storage.
                if (GenericUserManager.shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }

            return true;
        } finally {
            user.getIoLock().unlock();
            user.getRefreshBuffer().requestDirectly();
        }
    }

    @Override
    public boolean saveUser(User user) {
        user.getIoLock().lock();
        try {
            // Empty data - just delete from the DB.
            if (!GenericUserManager.shouldSave(user)) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_DELETE))) {
                        ps.setString(1, user.getUuid().toString());
                        ps.execute();
                    }
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_UPDATE_PRIMARY_GROUP))) {
                        ps.setString(1, "default");
                        ps.setString(2, user.getUuid().toString());
                        ps.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            // Get a snapshot of current data.
            Set<NodeDataHolder> remote = new HashSet<>();
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_SELECT))) {
                    ps.setString(1, user.getUuid().toString());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            remote.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            } catch (SQLException e) {
                return false;
            }

            Set<NodeDataHolder> local = user.getNodes().stream().map(NodeDataHolder::fromNode).collect(Collectors.toSet());

            Map.Entry<Set<NodeDataHolder>, Set<NodeDataHolder>> diff = compareSets(local, remote);

            Set<NodeDataHolder> toAdd = diff.getKey();
            Set<NodeDataHolder> toRemove = diff.getValue();

            if (!toAdd.isEmpty()) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_INSERT))) {
                        for (NodeDataHolder nd : toAdd) {
                            ps.setString(1, user.getUuid().toString());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.isValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, nd.serialiseContext());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            if (!toRemove.isEmpty()) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_DELETE_SPECIFIC))) {
                        for (NodeDataHolder nd : toRemove) {
                            ps.setString(1, user.getUuid().toString());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.isValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, nd.serialiseContext());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_UPDATE_PRIMARY_GROUP))) {
                    ps.setString(1, user.getPrimaryGroup().getStoredValue() == null ? "default" : user.getPrimaryGroup().getStoredValue());
                    ps.setString(2, user.getUuid().toString());
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return true; // TODO
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_SELECT_DISTINCT))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String uuid = rs.getString("uuid");
                        uuids.add(UUID.fromString(uuid));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return uuids;
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<UUID>> held = ImmutableList.builder();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(USER_PERMISSIONS_SELECT_PERMISSION))) {
                ps.setString(1, permission);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID holder = UUID.fromString(rs.getString("uuid"));
                        boolean value = rs.getBoolean("value");
                        String server = rs.getString("server");
                        String world = rs.getString("world");
                        long expiry = rs.getLong("expiry");
                        String contexts = rs.getString("contexts");

                        NodeDataHolder data = NodeDataHolder.of(permission, value, server, world, expiry, contexts);
                        held.add(NodeHeldPermission.of(holder, data));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return held.build();
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        List<String> groups = new ArrayList<>();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (!groups.contains(name)) {
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_INSERT))) {
                    ps.setString(1, name);
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return loadGroup(name);
    }

    @Override
    public boolean loadGroup(String name) {
        // Check the group actually exists
        List<String> groups = new ArrayList<>();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (!groups.contains(name)) {
            return false;
        }

        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            List<NodeDataHolder> data = new ArrayList<>();

            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_SELECT))) {
                    ps.setString(1, group.getName());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            if (!data.isEmpty()) {
                Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                group.setNodes(nodes);
            } else {
                group.clearNodes();
            }

            return true;
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllGroups() {
        List<String> groups = new ArrayList<>();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        boolean success = true;
        for (String g : groups) {
            if (!loadGroup(g)) {
                success = false;
            }
        }

        if (success) {
            GroupManager gm = plugin.getGroupManager();
            gm.getAll().values().stream()
                    .filter(g -> !groups.contains(g.getName()))
                    .forEach(gm::unload);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            // Empty data, just delete.
            if (group.getNodes().isEmpty()) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_DELETE))) {
                        ps.setString(1, group.getName());
                        ps.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

            // Get a snapshot of current data
            Set<NodeDataHolder> remote = new HashSet<>();
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_SELECT))) {
                    ps.setString(1, group.getName());

                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            remote.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            Set<NodeDataHolder> local = group.getNodes().stream().map(NodeDataHolder::fromNode).collect(Collectors.toSet());

            Map.Entry<Set<NodeDataHolder>, Set<NodeDataHolder>> diff = compareSets(local, remote);

            Set<NodeDataHolder> toAdd = diff.getKey();
            Set<NodeDataHolder> toRemove = diff.getValue();

            if (!toAdd.isEmpty()) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_INSERT))) {
                        for (NodeDataHolder nd : toAdd) {
                            ps.setString(1, group.getName());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.isValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, nd.serialiseContext());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            if (!toRemove.isEmpty()) {
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_DELETE_SPECIFIC))) {
                        for (NodeDataHolder nd : toRemove) {
                            ps.setString(1, group.getName());
                            ps.setString(2, nd.getPermission());
                            ps.setBoolean(3, nd.isValue());
                            ps.setString(4, nd.getServer());
                            ps.setString(5, nd.getWorld());
                            ps.setLong(6, nd.getExpiry());
                            ps.setString(7, nd.serialiseContext());
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            return true;
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_DELETE))) {
                    ps.setString(1, group.getName());
                    ps.execute();
                }

                try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_DELETE))) {
                    ps.setString(1, group.getName());
                    ps.execute();
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;

        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        ImmutableList.Builder<HeldPermission<String>> held = ImmutableList.builder();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(GROUP_PERMISSIONS_SELECT_PERMISSION))) {
                ps.setString(1, permission);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String holder = rs.getString("name");
                        boolean value = rs.getBoolean("value");
                        String server = rs.getString("server");
                        String world = rs.getString("world");
                        long expiry = rs.getLong("expiry");
                        String contexts = rs.getString("contexts");

                        NodeDataHolder data = NodeDataHolder.of(permission, value, server, world, expiry, contexts);
                        held.add(NodeHeldPermission.of(holder, data));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return held.build();
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            AtomicBoolean exists = new AtomicBoolean(false);
            AtomicReference<String> groups = new AtomicReference<>(null);

            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_SELECT))) {
                    ps.setString(1, track.getName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            exists.set(true);
                            groups.set(rs.getString("groups"));
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            if (exists.get()) {
                // Track exists, let's load.
                track.setGroups(gson.fromJson(groups.get(), LIST_STRING_TYPE));
                return true;
            } else {
                String json = gson.toJson(track.getGroups());
                try (Connection c = provider.getConnection()) {
                    try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_INSERT))) {
                        ps.setString(1, track.getName());
                        ps.setString(2, json);
                        ps.execute();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }

        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            AtomicReference<String> groups = new AtomicReference<>(null);
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_SELECT))) {
                    ps.setString(1, track.getName());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            groups.set(rs.getString("groups"));
                        } else {
                            return false;
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }

            track.setGroups(gson.fromJson(groups.get(), LIST_STRING_TYPE));
            return true;

        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        List<String> tracks = new ArrayList<>();
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_SELECT_ALL))) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tracks.add(rs.getString("name").toLowerCase());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        boolean success = true;
        for (String t : tracks) {
            if (!loadTrack(t)) {
                success = false;
            }
        }

        if (success) {
            TrackManager tm = plugin.getTrackManager();
            tm.getAll().values().stream()
                    .filter(t -> !tracks.contains(t.getName()))
                    .forEach(tm::unload);
        }
        return success;
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            String s = gson.toJson(track.getGroups());
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_UPDATE))) {
                    ps.setString(1, s);
                    ps.setString(2, track.getName());
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(TRACK_DELETE))) {
                    ps.setString(1, track.getName());
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            track.getIoLock().unlock();
        }

        plugin.getTrackManager().unload(track);
        return true;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        final String u = username.toLowerCase();
        AtomicReference<String> remoteUserName = new AtomicReference<>(null);

        // cleanup any old values
        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_DELETE))) {
                ps.setString(1, u);
                ps.setString(2, uuid.toString());
                ps.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_SELECT_USERNAME))) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        remoteUserName.set(rs.getString("username"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (remoteUserName.get() != null) {
            // the value is already correct
            if (remoteUserName.get().equals(u)) {
                return true;
            }

            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_UPDATE))) {
                    ps.setString(1, u);
                    ps.setString(2, uuid.toString());
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            // first time we've seen this uuid
            try (Connection c = provider.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_INSERT))) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, u);
                    ps.setString(3, "default");
                    ps.execute();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    @Override
    public UUID getUUID(String username) {
        final String u = username.toLowerCase();
        final AtomicReference<UUID> uuid = new AtomicReference<>(null);

        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_SELECT_UUID))) {
                ps.setString(1, u);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        uuid.set(UUID.fromString(rs.getString("uuid")));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return uuid.get();
    }

    @Override
    public String getName(UUID uuid) {
        final AtomicReference<String> name = new AtomicReference<>(null);

        try (Connection c = provider.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(prefix.apply(PLAYER_SELECT_USERNAME))) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        name.set(rs.getString("username"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        return name.get();
    }

    /**
     * Compares two sets
     * @param local the local set
     * @param remote the remote set
     * @return the entries to add to remote, and the entries to remove from remote
     */
    private static Map.Entry<Set<NodeDataHolder>, Set<NodeDataHolder>> compareSets(Set<NodeDataHolder> local, Set<NodeDataHolder> remote) {
        // entries in local but not remote need to be added
        // entries in remote but not local need to be removed

        Set<NodeDataHolder> toAdd = new HashSet<>(local);
        toAdd.removeAll(remote);

        Set<NodeDataHolder> toRemove = new HashSet<>(remote);
        toRemove.removeAll(local);

        return Maps.immutableEntry(toAdd, toRemove);
    }
}
