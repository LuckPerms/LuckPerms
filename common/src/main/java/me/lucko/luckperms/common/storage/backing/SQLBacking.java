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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;
import me.lucko.luckperms.common.managers.impl.GenericUserManager;
import me.lucko.luckperms.common.storage.backing.sqlprovider.SQLProvider;
import me.lucko.luckperms.common.storage.backing.utils.NodeDataHolder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLBacking extends AbstractBacking {
    private static final Type LIST_STRING_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_PERMISSIONS_SELECT = "SELECT permission, value, server, world, expiry, contexts FROM {prefix}user_permissions WHERE uuid=?";
    private static final String USER_PERMISSIONS_DELETE = "DELETE FROM {prefix}user_permissions WHERE uuid=?";
    private static final String USER_PERMISSIONS_INSERT = "INSERT INTO {prefix}user_permissions VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String USER_PERMISSIONS_SELECT_DISTINCT = "SELECT DISTINCT uuid FROM {prefix}user_permissions";

    private static final String PLAYER_SELECT = "SELECT username, primary_group FROM {prefix}players WHERE uuid=?";
    private static final String PLAYER_SELECT_UUID = "SELECT uuid FROM {prefix}players WHERE username=? LIMIT 1";
    private static final String PLAYER_SELECT_USERNAME = "SELECT username FROM {prefix}players WHERE uuid=? LIMIT 1";
    private static final String PLAYER_INSERT = "INSERT INTO {prefix}players VALUES(?, ?, ?)";
    private static final String PLAYER_UPDATE = "UPDATE {prefix}players SET username=? WHERE uuid=?";
    private static final String PLAYER_UPDATE_FULL = "UPDATE {prefix}players SET username=?, primary_group=? WHERE uuid=?";

    private static final String GROUP_PERMISSIONS_SELECT = "SELECT permission, value, server, world, expiry, contexts FROM {prefix}group_permissions WHERE name=?";
    private static final String GROUP_PERMISSIONS_DELETE = "DELETE FROM {prefix}group_permissions WHERE name=?";
    private static final String GROUP_PERMISSIONS_INSERT = "INSERT INTO {prefix}group_permissions VALUES(?, ?, ?, ?, ?, ?, ?)";

    private static final String GROUP_SELECT = "SELECT name FROM {prefix}groups";
    private static final String GROUP_INSERT = "INSERT INTO {prefix}groups VALUES(?)";
    private static final String GROUP_DELETE = "DELETE FROM {prefix}groups WHERE name=?";

    private static final String TRACK_INSERT = "INSERT INTO {prefix}tracks VALUES(?, ?)";
    private static final String TRACK_SELECT = "SELECT groups FROM {prefix}tracks WHERE name=?";
    private static final String TRACK_SELECT_ALL = "SELECT * FROM {prefix}tracks";
    private static final String TRACK_UPDATE = "UPDATE {prefix}tracks SET groups=? WHERE name=?";
    private static final String TRACK_DELETE = "DELETE FROM {prefix}tracks WHERE name=?";

    private static final String ACTION_INSERT = "INSERT INTO {prefix}actions(`time`, `actor_uuid`, `actor_name`, `type`, `acted_uuid`, `acted_name`, `action`) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTION_SELECT_ALL = "SELECT * FROM {prefix}actions";


    private final Gson gson;
    private final SQLProvider provider;
    private final Function<String, String> prefix;

    public SQLBacking(LuckPermsPlugin plugin, SQLProvider provider, String prefix) {
        super(plugin, provider.getName());
        this.provider = provider;
        this.prefix = s -> s.replace("{prefix}", prefix);
        gson = new Gson();
    }

    private boolean runQuery(String query, SQLProvider.QueryPS queryPS) {
        return provider.runQuery(query, queryPS);
    }

    private boolean runQuery(String query, SQLProvider.QueryPS queryPS, SQLProvider.QueryRS queryRS) {
        return provider.runQuery(query, queryPS, queryRS);
    }

    private boolean runQuery(String query) {
        return provider.runQuery(query);
    }

    private boolean runQuery(String query, SQLProvider.QueryRS queryRS) {
        return provider.runQuery(query, queryRS);
    }

    private boolean tableExists(String table) throws SQLException {
        try (Connection connection = provider.getConnection()) {
            return connection.getMetaData().getTables(null, null, table.toUpperCase(), null).next();
        }
    }

    @Override
    public void init() {
        try {
            provider.init();

            // Init tables
            if (!tableExists(prefix + "user_permissions")) {
                String schemaFileName = "lp-schema-" + provider.getName().toLowerCase() + ".sql";
                try (InputStream is = plugin.getClass().getResourceAsStream("sql/" + schemaFileName)) {
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
        return runQuery(prefix.apply(ACTION_INSERT), ps -> {
            ps.setLong(1, entry.getTimestamp());
            ps.setString(2, entry.getActor().toString());
            ps.setString(3, entry.getActorName());
            ps.setString(4, Character.toString(entry.getType()));
            ps.setString(5, entry.getActed() == null ? "null" : entry.getActed().toString());
            ps.setString(6, entry.getActedName());
            ps.setString(7, entry.getAction());
        });
    }

    @Override
    public Log getLog() {
        final Log.Builder log = Log.builder();
        boolean success = runQuery(prefix.apply(ACTION_SELECT_ALL), rs -> {
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
            return true;
        });
        return success ? log.build() : null;
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            List<NodeDataHolder> data = new ArrayList<>();
            AtomicReference<String> primaryGroup = new AtomicReference<>(null);
            AtomicReference<String> userName = new AtomicReference<>();
            boolean s = runQuery(
                    prefix.apply(USER_PERMISSIONS_SELECT),
                    ps -> ps.setString(1, user.getUuid().toString()),
                    rs -> {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                        return true;
                    }
            );

            boolean s2 = runQuery(
                    prefix.apply(PLAYER_SELECT),
                    ps -> ps.setString(1, user.getUuid().toString()),
                    rs -> {
                        if (rs.next()) {
                            userName.set(rs.getString("username"));
                            primaryGroup.set(rs.getString("primary_group"));
                        }
                        return true;
                    }
            );

            if (!s || !s2) {
                return false;
            }

            if (!data.isEmpty()) {
                Set<Node> nodes = data.stream().map(NodeDataHolder::toNode).collect(Collectors.toSet());
                user.setNodes(nodes);

                String pg = primaryGroup.get();
                if (pg == null) {
                    pg = "default";
                }
                user.setPrimaryGroup(pg);

                String name = userName.get();
                if (name == null) {
                    name = "null";
                }

                boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                    user.setName(name);
                } else {
                    if (!name.equals(user.getName())) {
                        save = true;
                    }
                }

                // Save back to the store if there was a username change
                if (save) {
                    boolean s3 = runQuery(prefix.apply(USER_PERMISSIONS_DELETE), ps -> {
                        ps.setString(1, user.getUuid().toString());
                    });

                    if (!s3) return false;

                    List<NodeDataHolder> newData = user.getNodes().stream().map(NodeDataHolder::fromNode).collect(Collectors.toList());
                    if (newData.isEmpty()) return true;

                    try (Connection connection = provider.getConnection()) {
                        try (PreparedStatement ps = connection.prepareStatement(prefix.apply(USER_PERMISSIONS_INSERT))) {
                            for (NodeDataHolder nd : newData) {
                                ps.setString(1, user.getUuid().toString());
                                ps.setString(2, nd.getPermission());
                                ps.setBoolean(3, nd.isValue());
                                ps.setString(4, nd.getServer());
                                ps.setString(5, nd.getWorld());
                                ps.setLong(6, nd.getExpiry());
                                ps.setString(7, nd.getContexts());
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    AtomicBoolean exists = new AtomicBoolean(false);
                    boolean success = runQuery(
                            prefix.apply(PLAYER_SELECT_USERNAME),
                            ps -> ps.setString(1, uuid.toString()),
                            rs -> {
                                if (rs.next()) {
                                    exists.set(true);
                                }
                                return true;
                            }
                    );
                    if (!success) {
                        return false;
                    }

                    if (exists.get()) {
                        return runQuery(prefix.apply(PLAYER_UPDATE_FULL), ps -> {
                            ps.setString(1, user.getName().toLowerCase());
                            ps.setString(2, user.getPrimaryGroup() == null ? "default" : user.getPrimaryGroup());
                            ps.setString(3, uuid.toString());
                        });
                    } else {
                        return runQuery(prefix.apply(PLAYER_INSERT), ps -> {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, user.getName().toLowerCase());
                            ps.setString(3, user.getPrimaryGroup() == null ? "default" : user.getPrimaryGroup());
                        });
                    }
                }

            } else {
                if (GenericUserManager.shouldSave(user)) {
                    user.clearNodes();
                    user.setPrimaryGroup(null);
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
        boolean shouldSave = GenericUserManager.shouldSave(user);

        user.getIoLock().lock();
        try {
            boolean s = runQuery(prefix.apply(USER_PERMISSIONS_DELETE), ps -> {
                ps.setString(1, user.getUuid().toString());
            });

            if (!s) return false;
            if (!shouldSave) return true;

            List<NodeDataHolder> data = user.getNodes().stream().map(NodeDataHolder::fromNode).collect(Collectors.toList());
            if (data.isEmpty()) return true;

            try (Connection connection = provider.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(prefix.apply(USER_PERMISSIONS_INSERT))) {
                    for (NodeDataHolder nd : data) {
                        ps.setString(1, user.getUuid().toString());
                        ps.setString(2, nd.getPermission());
                        ps.setBoolean(3, nd.isValue());
                        ps.setString(4, nd.getServer());
                        ps.setString(5, nd.getWorld());
                        ps.setLong(6, nd.getExpiry());
                        ps.setString(7, nd.getContexts());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            AtomicBoolean exists = new AtomicBoolean(false);
            boolean success = runQuery(
                    prefix.apply(PLAYER_SELECT_USERNAME),
                    ps -> ps.setString(1, user.getUuid().toString()),
                    rs -> {
                        if (rs.next()) {
                            exists.set(true);
                        }
                        return true;
                    }
            );
            if (!success) {
                return false;
            }

            if (exists.get()) {
                return runQuery(prefix.apply(PLAYER_UPDATE_FULL), ps -> {
                    ps.setString(1, user.getName().toLowerCase());
                    ps.setString(2, user.getPrimaryGroup() == null ? "default" : user.getPrimaryGroup());
                    ps.setString(3, user.getUuid().toString());
                });
            } else {
                return runQuery(prefix.apply(PLAYER_INSERT), ps -> {
                    ps.setString(1, user.getUuid().toString());
                    ps.setString(2, user.getName().toLowerCase());
                    ps.setString(3, user.getPrimaryGroup() == null ? "default" : user.getPrimaryGroup());
                });
            }
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

        boolean success = runQuery(prefix.apply(USER_PERMISSIONS_SELECT_DISTINCT), rs -> {
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                uuids.add(UUID.fromString(uuid));
            }
            return true;
        });

        return success ? uuids : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        List<String> groups = new ArrayList<>();
        runQuery(
                prefix.apply(GROUP_SELECT),
                rs -> {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                    return true;
                }
        );

        if (!groups.contains(name)) {
            runQuery(
                    prefix.apply(GROUP_INSERT),
                    ps -> {
                        ps.setString(1, name);
                    }
            );
        }

        return loadGroup(name);
    }

    @Override
    public boolean loadGroup(String name) {
        List<String> groups = new ArrayList<>();
        runQuery(
                prefix.apply(GROUP_SELECT),
                rs -> {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                    return true;
                }
        );

        if (!groups.contains(name)) {
            return false;
        }

        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            List<NodeDataHolder> data = new ArrayList<>();
            boolean s = runQuery(
                    prefix.apply(GROUP_PERMISSIONS_SELECT),
                    ps -> ps.setString(1, group.getName()),
                    rs -> {
                        while (rs.next()) {
                            String permission = rs.getString("permission");
                            boolean value = rs.getBoolean("value");
                            String server = rs.getString("server");
                            String world = rs.getString("world");
                            long expiry = rs.getLong("expiry");
                            String contexts = rs.getString("contexts");
                            data.add(NodeDataHolder.of(permission, value, server, world, expiry, contexts));
                        }
                        return true;
                    }
            );

            if (!s) {
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
        boolean b = runQuery(
                prefix.apply(GROUP_SELECT),
                rs -> {
                    while (rs.next()) {
                        groups.add(rs.getString("name").toLowerCase());
                    }
                    return true;
                }
        );

        if (!b) {
            return false;
        }

        for (String g : groups) {
            if (!loadGroup(g)) {
                b = false;
            }
        }

        if (b) {
            GroupManager gm = plugin.getGroupManager();
            gm.getAll().values().stream()
                    .filter(g -> !groups.contains(g.getName()))
                    .forEach(gm::unload);
        }
        return b;
    }

    @Override
    public boolean saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            boolean s = runQuery(prefix.apply(GROUP_PERMISSIONS_DELETE), ps -> {
                ps.setString(1, group.getName());
            });
            if (!s) {
                return false;
            }

            List<NodeDataHolder> data = group.getNodes().stream().map(NodeDataHolder::fromNode).collect(Collectors.toList());

            if (data.isEmpty()) {
                return true;
            }

            try (Connection connection = provider.getConnection()) {
                try (PreparedStatement ps = connection.prepareStatement(prefix.apply(GROUP_PERMISSIONS_INSERT))) {
                    for (NodeDataHolder nd : data) {
                        ps.setString(1, group.getName());
                        ps.setString(2, nd.getPermission());
                        ps.setBoolean(3, nd.isValue());
                        ps.setString(4, nd.getServer());
                        ps.setString(5, nd.getWorld());
                        ps.setLong(6, nd.getExpiry());
                        ps.setString(7, nd.getContexts());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
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

            boolean s = runQuery(
                    prefix.apply(GROUP_PERMISSIONS_DELETE),
                    ps -> {
                        ps.setString(1, group.getName());
                    }
            );
            return s && runQuery(prefix.apply(GROUP_DELETE), ps -> {
                ps.setString(1, group.getName());
            });

        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            AtomicBoolean exists = new AtomicBoolean(false);
            AtomicReference<String> groups = new AtomicReference<>(null);

            boolean s = runQuery(
                    prefix.apply(TRACK_SELECT),
                    ps -> ps.setString(1, track.getName()),
                    rs -> {
                        if (rs.next()) {
                            exists.set(true);
                            groups.set(rs.getString("groups"));
                        }
                        return true;
                    }
            );

            if (!s) {
                return false;
            }

            if (exists.get()) {
                // Track exists, let's load.
                track.setGroups(gson.fromJson(groups.get(), LIST_STRING_TYPE));
                return true;
            } else {
                String json = gson.toJson(track.getGroups());
                return runQuery(prefix.apply(TRACK_INSERT), ps -> {
                    ps.setString(1, track.getName());
                    ps.setString(2, json);
                });
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
            boolean s = runQuery(
                    prefix.apply(TRACK_SELECT),
                    ps -> ps.setString(1, name),
                    rs -> {
                        if (rs.next()) {
                            groups.set(rs.getString("groups"));
                            return true;
                        }
                        return false;
                    }
            );

            if (!s) {
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
        boolean b = runQuery(
                prefix.apply(TRACK_SELECT_ALL),
                rs -> {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        tracks.add(name);
                    }
                    return true;
                }
        );

        if (!b) {
            return false;
        }

        for (String t : tracks) {
            if (!loadTrack(t)) {
                b = false;
            }
        }

        if (b) {
            TrackManager tm = plugin.getTrackManager();
            tm.getAll().values().stream()
                    .filter(t -> !tracks.contains(t.getName()))
                    .forEach(tm::unload);
        }
        return b;
    }

    @Override
    public boolean saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            String s = gson.toJson(track.getGroups());
            return runQuery(
                    prefix.apply(TRACK_UPDATE),
                    ps -> {
                        ps.setString(1, s);
                        ps.setString(2, track.getName());
                    }
            );
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        boolean success;
        try {
            success = runQuery(
                    prefix.apply(TRACK_DELETE),
                    ps -> {
                        ps.setString(1, track.getName());
                    }
            );
        } finally {
            track.getIoLock().unlock();
        }

        if (success) plugin.getTrackManager().unload(track);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        final String u = username.toLowerCase();
        AtomicBoolean exists = new AtomicBoolean(false);

        boolean success = runQuery(
                prefix.apply(PLAYER_SELECT_USERNAME),
                ps -> ps.setString(1, uuid.toString()),
                rs -> {
                    if (rs.next()) {
                        exists.set(true);
                    }
                    return true;
                }
        );

        if (!success) {
            return false;
        }

        if (exists.get()) {
            return runQuery(prefix.apply(PLAYER_UPDATE), ps -> {
                ps.setString(1, u);
                ps.setString(2, uuid.toString());
            });
        } else {
            return runQuery(prefix.apply(PLAYER_INSERT), ps -> {
                ps.setString(1, uuid.toString());
                ps.setString(2, u);
                ps.setString(3, "default");
            });
        }
    }

    @Override
    public UUID getUUID(String username) {
        final String u = username.toLowerCase();
        final AtomicReference<UUID> uuid = new AtomicReference<>(null);

        boolean success = runQuery(
                prefix.apply(PLAYER_SELECT_UUID),
                ps -> ps.setString(1, u),
                rs -> {
                    if (rs.next()) {
                        uuid.set(UUID.fromString(rs.getString("uuid")));
                        return true;
                    }
                    return false;
                }
        );

        return success ? uuid.get() : null;
    }

    @Override
    public String getName(UUID uuid) {
        final AtomicReference<String> name = new AtomicReference<>(null);

        boolean success = runQuery(
                prefix.apply(PLAYER_SELECT_USERNAME),
                ps -> ps.setString(1, uuid.toString()),
                rs -> {
                    if (rs.next()) {
                        name.set(rs.getString("username"));
                        return true;
                    }
                    return false;
                }
        );

        return success ? name.get() : null;
    }
}
