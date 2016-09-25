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

package me.lucko.luckperms.storage.methods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.data.Log;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.storage.Datastore;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.User;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static me.lucko.luckperms.core.PermissionHolder.exportToLegacy;

abstract class SQLDatastore extends Datastore {
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

    SQLDatastore(LuckPermsPlugin plugin, String name) {
        super(plugin, name);
        gson = new Gson();
    }

    abstract Connection getConnection() throws SQLException;

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
        User user = plugin.getUserManager().make(uuid, username);
        boolean success = runQuery(USER_SELECT,
                preparedStatement -> preparedStatement.setString(1, user.getUuid().toString()),
                resultSet -> {
                    if (resultSet.next()) {
                        // User exists, let's load.
                        Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                        user.setNodes(nodes);
                        user.setPrimaryGroup(resultSet.getString("primary_group"));

                        if (user.getName().equalsIgnoreCase("null")) {
                            user.setName(resultSet.getString("name"));
                        } else {
                            if (!resultSet.getString("name").equals(user.getName())) {
                                runQuery(USER_UPDATE, preparedStatement -> {
                                    preparedStatement.setString(1, user.getName());
                                    preparedStatement.setString(2, user.getPrimaryGroup());
                                    preparedStatement.setString(3, gson.toJson(exportToLegacy(user.getNodes())));
                                    preparedStatement.setString(4, user.getUuid().toString());
                                });
                            }
                        }
                    }
                    return true;
                }
        );

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        if (!plugin.getUserManager().shouldSave(user)) {
            return runQuery(USER_DELETE, preparedStatement -> {
                preparedStatement.setString(1, user.getUuid().toString());
            });
        }

        return runQuery(USER_SELECT,
                preparedStatement -> preparedStatement.setString(1, user.getUuid().toString()),
                resultSet -> {
                    if (!resultSet.next()) {
                        // Doesn't already exist, let's insert.
                        return runQuery(USER_INSERT, preparedStatement -> {
                            preparedStatement.setString(1, user.getUuid().toString());
                            preparedStatement.setString(2, user.getName());
                            preparedStatement.setString(3, user.getPrimaryGroup());
                            preparedStatement.setString(4, gson.toJson(exportToLegacy(user.getNodes())));
                        });

                    } else {
                        // User exists, let's update.
                        return runQuery(USER_UPDATE, preparedStatement -> {
                            preparedStatement.setString(1, user.getName());
                            preparedStatement.setString(2, user.getPrimaryGroup());
                            preparedStatement.setString(3, gson.toJson(exportToLegacy(user.getNodes())));
                            preparedStatement.setString(4, user.getUuid().toString());
                        });
                    }
                }
        );
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
        Group group = plugin.getGroupManager().make(name);
        boolean success = runQuery(GROUP_SELECT,
                preparedStatement -> preparedStatement.setString(1, group.getName()),
                resultSet -> {
                    if (!resultSet.next()) {
                        return runQuery(GROUP_INSERT, preparedStatement -> {
                            preparedStatement.setString(1, group.getName());
                            preparedStatement.setString(2, gson.toJson(exportToLegacy(group.getNodes())));
                        });
                    } else {
                        Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                        group.setNodes(nodes);
                        return true;
                    }
                }
        );

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);
        boolean success = runQuery(GROUP_SELECT,
                preparedStatement -> preparedStatement.setString(1, name),
                resultSet -> {
                    if (resultSet.next()) {
                        Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                        group.setNodes(nodes);
                        return true;
                    }
                    return false;
                }
        );

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        List<Group> groups = new ArrayList<>();
        boolean success = runQuery(GROUP_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                Group group = plugin.getGroupManager().make(resultSet.getString("name"));
                Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                group.setNodes(nodes);
                groups.add(group);
            }
            return true;
        });

        if (success) {
            GroupManager gm = plugin.getGroupManager();
            gm.unloadAll();
            groups.forEach(gm::set);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        return runQuery(GROUP_UPDATE, preparedStatement -> {
            preparedStatement.setString(1, gson.toJson(exportToLegacy(group.getNodes())));
            preparedStatement.setString(2, group.getName());
        });
    }

    @Override
    public boolean deleteGroup(Group group) {
        boolean success = runQuery(GROUP_DELETE, preparedStatement -> {
            preparedStatement.setString(1, group.getName());
        });

        if (success) plugin.getGroupManager().unload(group);
        return success;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success = runQuery(TRACK_SELECT,
                preparedStatement -> preparedStatement.setString(1, track.getName()),
                resultSet -> {
                    if (!resultSet.next()) {
                        return runQuery(TRACK_INSERT, preparedStatement -> {
                            preparedStatement.setString(1, track.getName());
                            preparedStatement.setString(2, gson.toJson(track.getGroups()));
                        });
                    } else {
                        track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                        return true;
                    }
                }
        );

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success = runQuery(TRACK_SELECT,
                preparedStatement -> preparedStatement.setString(1, name),
                resultSet -> {
                    if (resultSet.next()) {
                        track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                        return true;
                    }
                    return false;
                }
        );

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadAllTracks() {
        List<Track> tracks = new ArrayList<>();
        boolean success = runQuery(TRACK_SELECT_ALL, resultSet -> {
            while (resultSet.next()) {
                Track track = plugin.getTrackManager().make(resultSet.getString("name"));
                track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                tracks.add(track);
            }
            return true;
        });

        if (success) {
            TrackManager tm = plugin.getTrackManager();
            tm.unloadAll();
            tracks.forEach(tm::set);
        }
        return success;
    }

    @Override
    public boolean saveTrack(Track track) {
        return runQuery(TRACK_UPDATE, preparedStatement -> {
            preparedStatement.setString(1, gson.toJson(track.getGroups()));
            preparedStatement.setString(2, track.getName());
        });
    }

    @Override
    public boolean deleteTrack(Track track) {
        boolean success = runQuery(TRACK_DELETE, preparedStatement -> {
            preparedStatement.setString(1, track.getName());
        });

        if (success) plugin.getTrackManager().unload(track);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        final String u = username.toLowerCase();
        return runQuery(UUIDCACHE_SELECT,
                preparedStatement -> preparedStatement.setString(1, u),
                resultSet -> {
                    boolean success;
                    if (resultSet.next()) {
                        success = runQuery(UUIDCACHE_UPDATE, preparedStatement -> {
                            preparedStatement.setString(1, uuid.toString());
                            preparedStatement.setString(2, u);
                        });
                    } else {
                        success = runQuery(UUIDCACHE_INSERT, preparedStatement -> {
                            preparedStatement.setString(1, u);
                            preparedStatement.setString(2, uuid.toString());
                        });
                    }
                    return success;
                }
        );
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
