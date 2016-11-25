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
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.core.UserIdentifier;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.Track;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.data.Log;
import me.lucko.luckperms.common.managers.GroupManager;
import me.lucko.luckperms.common.managers.TrackManager;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static me.lucko.luckperms.common.core.model.PermissionHolder.exportToLegacy;

abstract class SQLBacking extends AbstractBacking {
    private static final QueryPS EMPTY_PS = preparedStatement -> {};

    private static final Type NM_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();
    private static final Type T_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_INSERT = "INSERT INTO lp_users VALUES(?, ?, ?, ?)";
    private static final String USER_SELECT = "SELECT * FROM lp_users WHERE uuid=?";
    private static final String USER_SELECT_ALL = "SELECT uuid FROM lp_users";
    private static final String USER_UPDATE = "UPDATE lp_users SET name=?, primary_group = ?, perms=? WHERE uuid=?";
    private static final String USER_DELETE = "DELETE FROM lp_users WHERE uuid=?";
    private static final String USER_DELETE_ALL = "DELETE FROM lp_users WHERE perms=?";

    private static final String GROUP_INSERT = "INSERT INTO lp_groups VALUES(?, ?)";
    private static final String GROUP_SELECT = "SELECT perms FROM lp_groups WHERE name=?";
    private static final String GROUP_SELECT_ALL = "SELECT * FROM lp_groups";
    private static final String GROUP_UPDATE = "UPDATE lp_groups SET perms=? WHERE name=?";
    private static final String GROUP_DELETE = "DELETE FROM lp_groups WHERE name=?";

    private static final String TRACK_INSERT = "INSERT INTO lp_tracks VALUES(?, ?)";
    private static final String TRACK_SELECT = "SELECT groups FROM lp_tracks WHERE name=?";
    private static final String TRACK_SELECT_ALL = "SELECT * FROM lp_tracks";
    private static final String TRACK_UPDATE = "UPDATE lp_tracks SET groups=? WHERE name=?";
    private static final String TRACK_DELETE = "DELETE FROM lp_tracks WHERE name=?";

    private static final String UUIDCACHE_INSERT = "INSERT INTO lp_uuid VALUES(?, ?)";
    private static final String UUIDCACHE_SELECT = "SELECT uuid FROM lp_uuid WHERE name=?";
    private static final String UUIDCACHE_SELECT_NAME = "SELECT name FROM lp_uuid WHERE uuid=?";
    private static final String UUIDCACHE_UPDATE = "UPDATE lp_uuid SET uuid=? WHERE name=?";

    private static final String ACTION_INSERT = "INSERT INTO lp_actions(`time`, `actor_uuid`, `actor_name`, `type`, `acted_uuid`, `acted_name`, `action`) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTION_SELECT_ALL = "SELECT * FROM lp_actions";

    private final Gson gson;

    SQLBacking(LuckPermsPlugin plugin, String name) {
        super(plugin, name);
        gson = new Gson();
    }

    abstract Connection getConnection() throws SQLException;

    protected static void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }
    }

    abstract boolean runQuery(String query, QueryPS queryPS);
    abstract boolean runQuery(String query, QueryPS queryPS, QueryRS queryRS);

    boolean runQuery(String query) {
        return runQuery(query, EMPTY_PS);
    }

    boolean runQuery(String query, QueryRS queryRS) {
        return runQuery(query, EMPTY_PS, queryRS);
    }

    boolean setupTables(String... tableQueries) {
        boolean success = true;
        for (String q : tableQueries) {
            if (!runQuery(q)) success = false;
        }

        return success && cleanupUsers();
    }

    @Override
    public boolean logAction(LogEntry entry) {
        return runQuery(ACTION_INSERT, preparedStatement -> {
            preparedStatement.setLong(1, entry.getTimestamp());
            preparedStatement.setString(2, entry.getActor().toString());
            preparedStatement.setString(3, entry.getActorName());
            preparedStatement.setString(4, Character.toString(entry.getType()));
            preparedStatement.setString(5, entry.getActed() == null ? "null" : entry.getActed().toString());
            preparedStatement.setString(6, entry.getActedName());
            preparedStatement.setString(7, entry.getAction());
        });
    }

    @Override
    public Log getLog() {
        final Log.Builder log = Log.builder();
        boolean success = runQuery(ACTION_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                final String actedUuid = resultSet.getString("acted_uuid");
                LogEntry e = new LogEntry(
                        resultSet.getLong("time"),
                        UUID.fromString(resultSet.getString("actor_uuid")),
                        resultSet.getString("actor_name"),
                        resultSet.getString("type").toCharArray()[0],
                        actedUuid.equals("null") ? null : UUID.fromString(actedUuid),
                        resultSet.getString("acted_name"),
                        resultSet.getString("action")
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
            // screw "effectively final"
            final String[] perms = new String[1];
            final String[] pg = new String[1];
            final String[] name = new String[1];
            final boolean[] exists = {false};

            boolean s = runQuery(USER_SELECT,
                    preparedStatement -> preparedStatement.setString(1, user.getUuid().toString()),
                    resultSet -> {
                        if (resultSet.next()) {
                            // User exists.
                            exists[0] = true;
                            perms[0] = resultSet.getString("perms");
                            pg[0] = resultSet.getString("primary_group");
                            name[0] = resultSet.getString("name");
                        }
                        return true;
                    }
            );

            if (!s) {
                return false;
            }

            if (exists[0]) {
                // User exists, let's load.
                Map<String, Boolean> nodes = gson.fromJson(perms[0], NM_TYPE);

                user.setNodes(nodes);
                user.setPrimaryGroup(pg[0]);

                boolean save = plugin.getUserManager().giveDefaultIfNeeded(user, false);

                if (user.getName() == null || user.getName().equalsIgnoreCase("null")) {
                    user.setName(name[0]);
                } else {
                    if (!name[0].equals(user.getName())) {
                        save = true;
                    }
                }

                if (save) {
                    String json = gson.toJson(exportToLegacy(user.getNodes()));
                    runQuery(USER_UPDATE, preparedStatement -> {
                        preparedStatement.setString(1, user.getName());
                        preparedStatement.setString(2, user.getPrimaryGroup());
                        preparedStatement.setString(3, json);
                        preparedStatement.setString(4, user.getUuid().toString());
                    });
                }

            } else {
                if (plugin.getUserManager().shouldSave(user)) {
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
        if (!plugin.getUserManager().shouldSave(user)) {
            user.getIoLock().lock();
            try {
                return runQuery(USER_DELETE, preparedStatement -> {
                    preparedStatement.setString(1, user.getUuid().toString());
                });
            } finally {
                user.getIoLock().unlock();
            }
        }

        user.getIoLock().lock();
        try {
            final boolean[] exists = {false};
            boolean success = runQuery(USER_SELECT,
                    preparedStatement -> preparedStatement.setString(1, user.getUuid().toString()),
                    resultSet -> {
                        if (resultSet.next()) {
                            exists[0] = true;
                        }
                        return true;
                    }
            );

            if (!success) {
                return false;
            }

            final String s = gson.toJson(exportToLegacy(user.getNodes()));

            if (exists[0]) {
                // User exists, let's update.
                return runQuery(USER_UPDATE, preparedStatement -> {
                    preparedStatement.setString(1, user.getName());
                    preparedStatement.setString(2, user.getPrimaryGroup());
                    preparedStatement.setString(3, s);
                    preparedStatement.setString(4, user.getUuid().toString());
                });
            } else {
                // Doesn't already exist, let's insert.
                return runQuery(USER_INSERT, preparedStatement -> {
                    preparedStatement.setString(1, user.getUuid().toString());
                    preparedStatement.setString(2, user.getName());
                    preparedStatement.setString(3, user.getPrimaryGroup());
                    preparedStatement.setString(4, s);
                });
            }


        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public boolean cleanupUsers() {
        return runQuery(USER_DELETE_ALL, preparedStatement -> {
            preparedStatement.setString(1, "{\"group.default\":true}");
        });
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();

        boolean success = runQuery(USER_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                String uuid = resultSet.getString("uuid");
                uuids.add(UUID.fromString(uuid));
            }
            return true;
        });

        return success ? uuids : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            final boolean[] exists = {false};
            final String[] perms = new String[1];

            boolean s = runQuery(GROUP_SELECT,
                    preparedStatement -> preparedStatement.setString(1, group.getName()),
                    resultSet -> {
                        if (resultSet.next()) {
                            exists[0] = true;
                            perms[0] = resultSet.getString("perms");
                        }
                        return true;
                    }
            );

            if (!s) {
                return false;
            }

            if (exists[0]) {
                // Group exists, let's load.
                Map<String, Boolean> nodes = gson.fromJson(perms[0], NM_TYPE);
                group.setNodes(nodes);
                return true;
            } else {
                String json = gson.toJson(exportToLegacy(group.getNodes()));
                return runQuery(GROUP_INSERT, preparedStatement -> {
                    preparedStatement.setString(1, group.getName());
                    preparedStatement.setString(2, json);
                });
            }

        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            final String[] perms = new String[1];
            boolean s = runQuery(GROUP_SELECT,
                    preparedStatement -> preparedStatement.setString(1, name),
                    resultSet -> {
                        if (resultSet.next()) {
                            perms[0] = resultSet.getString("perms");
                            return true;
                        }
                        return false;
                    }
            );

            if (!s) {
                return false;
            }

            // Group exists, let's load.
            Map<String, Boolean> nodes = gson.fromJson(perms[0], NM_TYPE);
            group.setNodes(nodes);
            return true;

        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllGroups() {
        List<String> groups = new ArrayList<>();
        boolean b = runQuery(GROUP_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                groups.add(name);
            }
            return true;
        });

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
            String json = gson.toJson(exportToLegacy(group.getNodes()));
            return runQuery(GROUP_UPDATE, preparedStatement -> {
                preparedStatement.setString(1, json);
                preparedStatement.setString(2, group.getName());
            });
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteGroup(Group group) {
        group.getIoLock().lock();
        boolean success;
        try {
            success = runQuery(GROUP_DELETE, preparedStatement -> {
                preparedStatement.setString(1, group.getName());
            });
        } finally {
            group.getIoLock().unlock();
        }

        if (success) plugin.getGroupManager().unload(group);
        return success;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            final boolean[] exists = {false};
            final String[] groups = new String[1];

            boolean s = runQuery(TRACK_SELECT,
                    preparedStatement -> preparedStatement.setString(1, track.getName()),
                    resultSet -> {
                        if (resultSet.next()) {
                            exists[0] = true;
                            groups[0] = resultSet.getString("groups");
                        }
                        return true;
                    }
            );

            if (!s) {
                return false;
            }

            if (exists[0]) {
                // Track exists, let's load.
                track.setGroups(gson.fromJson(groups[0], T_TYPE));
                return true;
            } else {
                String json = gson.toJson(track.getGroups());
                return runQuery(TRACK_INSERT, preparedStatement -> {
                    preparedStatement.setString(1, track.getName());
                    preparedStatement.setString(2, json);
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
            final String[] groups = {null};
            boolean s = runQuery(TRACK_SELECT,
                    preparedStatement -> preparedStatement.setString(1, name),
                    resultSet -> {
                        if (resultSet.next()) {
                            groups[0] = resultSet.getString("groups");
                            return true;
                        }
                        return false;
                    }
            );

            if (!s) {
                return false;
            }

            track.setGroups(gson.fromJson(groups[0], T_TYPE));
            return true;

        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean loadAllTracks() {
        List<String> tracks = new ArrayList<>();
        boolean b = runQuery(TRACK_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                tracks.add(name);
            }
            return true;
        });

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
            return runQuery(TRACK_UPDATE, preparedStatement -> {
                preparedStatement.setString(1, s);
                preparedStatement.setString(2, track.getName());
            });
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public boolean deleteTrack(Track track) {
        track.getIoLock().lock();
        boolean success;
        try {
            success = runQuery(TRACK_DELETE, preparedStatement -> {
                preparedStatement.setString(1, track.getName());
            });
        } finally {
            track.getIoLock().unlock();
        }

        if (success) plugin.getTrackManager().unload(track);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        final String u = username.toLowerCase();
        final boolean[] update = {false};
        boolean s = runQuery(UUIDCACHE_SELECT,
                preparedStatement -> preparedStatement.setString(1, u),
                resultSet -> {
                    if (resultSet.next()) {
                        update[0] = true;
                    }
                    return true;
                }
        );

        if (!s) {
            return false;
        }

        if (update[0]) {
            return runQuery(UUIDCACHE_UPDATE, preparedStatement -> {
                preparedStatement.setString(1, uuid.toString());
                preparedStatement.setString(2, u);
            });
        } else {
            return runQuery(UUIDCACHE_INSERT, preparedStatement -> {
                preparedStatement.setString(1, u);
                preparedStatement.setString(2, uuid.toString());
            });
        }
    }

    @Override
    public UUID getUUID(String username) {
        final String u = username.toLowerCase();
        final UUID[] uuid = {null};

        boolean success = runQuery(UUIDCACHE_SELECT,
                preparedStatement -> preparedStatement.setString(1, u),
                resultSet -> {
                    if (resultSet.next()) {
                        uuid[0] = UUID.fromString(resultSet.getString("uuid"));
                        return true;
                    }
                    return false;
                }
        );

        return success ? uuid[0] : null;
    }

    @Override
    public String getName(UUID uuid) {
        final String u = uuid.toString();
        final String[] name = {null};

        boolean success = runQuery(UUIDCACHE_SELECT_NAME,
                preparedStatement -> preparedStatement.setString(1, u),
                resultSet -> {
                    if (resultSet.next()) {
                        name[0] = resultSet.getString("name");
                        return true;
                    }
                    return false;
                }
        );

        return success ? name[0] : null;
    }

    interface QueryPS {
        void onRun(PreparedStatement preparedStatement) throws SQLException;
    }

    interface QueryRS {
        boolean onResult(ResultSet resultSet) throws SQLException;
    }
}
