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
import lombok.AllArgsConstructor;
import lombok.Getter;
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

@SuppressWarnings("UnnecessaryLocalVariable")
abstract class SQLDatastore extends Datastore {

    private static final Type NM_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();
    private static final Type T_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_INSERT = "INSERT INTO lp_users VALUES(?, ?, ?, ?)";
    private static final String USER_SELECT = "SELECT * FROM lp_users WHERE uuid=?";
    private static final String USER_SELECT_ALL = "SELECT uuid FROM lp_users";
    private static final String USER_UPDATE = "UPDATE lp_users SET name=?, primary_group = ?, perms=? WHERE uuid=?";
    private static final String USER_DELETE = "DELETE FROM lp_users WHERE perms=?";

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
    private static final String UUIDCACHE_UPDATE = "UPDATE lp_uuid SET uuid=? WHERE name=?";

    private static final String ACTION_INSERT = "INSERT INTO lp_actions(`time`, `actor_uuid`, `actor_name`, `type`, `acted_uuid`, `acted_name`, `action`) VALUES(?, ?, ?, ?, ?, ?, ?)";
    private static final String ACTION_SELECT_ALL = "SELECT * FROM lp_actions";

    private final Gson gson;

    SQLDatastore(LuckPermsPlugin plugin, String name) {
        super(plugin, name);
        gson = new Gson();
    }

    abstract Connection getConnection() throws SQLException;

    abstract boolean runQuery(QueryPS queryPS);
    abstract boolean runQuery(QueryRS queryRS);

    boolean setupTables(String... tableQueries) {
        boolean success = true;
        for (String q : tableQueries) {
            if (!runQuery(new Query(q))) success = false;
        }

        return success && cleanupUsers();
    }

    @Override
    public boolean logAction(LogEntry entry) {
        boolean success = runQuery(new QueryPS(ACTION_INSERT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setLong(1, entry.getTimestamp());
                preparedStatement.setString(2, entry.getActor().toString());
                preparedStatement.setString(3, entry.getActorName());
                preparedStatement.setString(4, Character.toString(entry.getType()));
                preparedStatement.setString(5, entry.getActed() == null ? "null" : entry.getActed().toString());
                preparedStatement.setString(6, entry.getActedName());
                preparedStatement.setString(7, entry.getAction());
            }
        });
        return success;
    }

    @Override
    public Log getLog() {
        final Log.Builder log = Log.builder();
        boolean success = runQuery(new QueryRS(ACTION_SELECT_ALL) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {

            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
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
            }
        });
        return success ? log.build() : null;
    }

    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().make(uuid, username);
        boolean success = runQuery(new QueryRS(USER_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, user.getUuid().toString());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    // User exists, let's load.
                    Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                    user.setNodes(nodes);
                    user.setPrimaryGroup(resultSet.getString("primary_group"));

                    if (user.getName().equalsIgnoreCase("null")) {
                        user.setName(resultSet.getString("name"));
                    } else {
                        if (!resultSet.getString("name").equals(user.getName())) {
                            runQuery(new QueryPS(USER_UPDATE) {
                                @Override
                                void onRun(PreparedStatement preparedStatement) throws SQLException {
                                    preparedStatement.setString(1, user.getName());
                                    preparedStatement.setString(2, user.getPrimaryGroup());
                                    preparedStatement.setString(3, gson.toJson(exportToLegacy(user.getNodes())));
                                    preparedStatement.setString(4, user.getUuid().toString());
                                }
                            });
                        }
                    }
                }
                return true;
            }
        });

        if (success) plugin.getUserManager().updateOrSet(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        if (!plugin.getUserManager().shouldSave(user)) {
            return true;
        }

        boolean success = runQuery(new QueryRS(USER_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, user.getUuid().toString());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean b;
                if (!resultSet.next()) {
                    // Doesn't already exist, let's insert.
                    b = runQuery(new QueryPS(USER_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, user.getUuid().toString());
                            preparedStatement.setString(2, user.getName());
                            preparedStatement.setString(3, user.getPrimaryGroup());
                            preparedStatement.setString(4, gson.toJson(exportToLegacy(user.getNodes())));
                        }
                    });

                } else {
                    // User exists, let's update.
                    b = runQuery(new QueryPS(USER_UPDATE) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, user.getName());
                            preparedStatement.setString(2, user.getPrimaryGroup());
                            preparedStatement.setString(3, gson.toJson(exportToLegacy(user.getNodes())));
                            preparedStatement.setString(4, user.getUuid().toString());
                        }
                    });
                }
                return b;
            }
        });

        return success;
    }

    @Override
    public boolean cleanupUsers() {
        boolean success = runQuery(new QueryPS(USER_DELETE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, "{\"group.default\":true}");
            }
        });
        return success;
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> uuids = new HashSet<>();

        boolean success = runQuery(new QueryRS(USER_SELECT_ALL) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    String uuid = resultSet.getString("uuid");
                    uuids.add(UUID.fromString(uuid));
                }
                return false;
            }
        });

        return success ? uuids : null;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);
        boolean success = runQuery(new QueryRS(GROUP_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, group.getName());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean success = true;
                if (!resultSet.next()) {
                    success = runQuery(new QueryPS(GROUP_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, group.getName());
                            preparedStatement.setString(2, gson.toJson(exportToLegacy(group.getNodes())));
                        }
                    });
                } else {
                    Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                    group.setNodes(nodes);
                }
                return success;
            }
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().make(name);
        boolean success = runQuery(new QueryRS(GROUP_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, name);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                    group.setNodes(nodes);
                    return true;
                }
                return false;
            }
        });

        if (success) plugin.getGroupManager().updateOrSet(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        List<Group> groups = new ArrayList<>();
        boolean success = runQuery(new QueryRS(GROUP_SELECT_ALL) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {

            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    Group group = plugin.getGroupManager().make(resultSet.getString("name"));
                    Map<String, Boolean> nodes = gson.fromJson(resultSet.getString("perms"), NM_TYPE);
                    group.setNodes(nodes);
                    groups.add(group);
                }
                return true;
            }
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
        boolean success = runQuery(new QueryPS(GROUP_UPDATE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, gson.toJson(exportToLegacy(group.getNodes())));
                preparedStatement.setString(2, group.getName());
            }
        });
        return success;
    }

    @Override
    public boolean deleteGroup(Group group) {
        boolean success = runQuery(new QueryPS(GROUP_DELETE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, group.getName());
            }
        });

        if (success) plugin.getGroupManager().unload(group);
        return success;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success = runQuery(new QueryRS(TRACK_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, track.getName());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean success = true;
                if (!resultSet.next()) {
                    success = runQuery(new QueryPS(TRACK_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, track.getName());
                            preparedStatement.setString(2, gson.toJson(track.getGroups()));
                        }
                    });
                } else {
                    track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                }
                return success;
            }
        });

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().make(name);
        boolean success = runQuery(new QueryRS(TRACK_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, name);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                    return true;
                }
                return false;
            }
        });

        if (success) plugin.getTrackManager().updateOrSet(track);
        return success;
    }

    @Override
    public boolean loadAllTracks() {
        List<Track> tracks = new ArrayList<>();
        boolean success = runQuery(new QueryRS(TRACK_SELECT_ALL) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {

            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                while (resultSet.next()) {
                    Track track = plugin.getTrackManager().make(resultSet.getString("name"));
                    track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                    tracks.add(track);
                }
                return true;
            }
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
        boolean success = runQuery(new QueryPS(TRACK_UPDATE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, gson.toJson(track.getGroups()));
                preparedStatement.setString(2, track.getName());
            }
        });
        return success;
    }

    @Override
    public boolean deleteTrack(Track track) {
        boolean success = runQuery(new QueryPS(TRACK_DELETE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, track.getName());
            }
        });

        if (success) plugin.getTrackManager().unload(track);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        final String u = username.toLowerCase();
        boolean success = runQuery(new QueryRS(UUIDCACHE_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, u);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean success;
                if (resultSet.next()) {
                    success = runQuery(new QueryPS(UUIDCACHE_UPDATE) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, uuid.toString());
                            preparedStatement.setString(2, u);
                        }
                    });
                } else {
                    success = runQuery(new QueryPS(UUIDCACHE_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, u);
                            preparedStatement.setString(2, uuid.toString());
                        }
                    });
                }
                return success;
            }
        });

        return success;
    }

    @Override
    public UUID getUUID(String username) {
        final String u = username.toLowerCase();
        final UUID[] uuid = {null};

        boolean success = runQuery(new QueryRS(UUIDCACHE_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, u);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    uuid[0] = UUID.fromString(resultSet.getString("uuid"));
                    return true;
                }
                return false;
            }
        });

        return success ? uuid[0] : null;
    }

    private class Query extends QueryPS {
        Query(String query) {
            super(query);
        }

        @Override
        void onRun(PreparedStatement preparedStatement) throws SQLException {
            // Do nothing
        }
    }

    @Getter
    @AllArgsConstructor
    abstract class QueryPS {
        private final String query;
        abstract void onRun(PreparedStatement preparedStatement) throws SQLException;
    }

    @Getter
    @AllArgsConstructor
    abstract class QueryRS {
        private final String query;
        abstract void onRun(PreparedStatement preparedStatement) throws SQLException;
        abstract boolean onResult(ResultSet resultSet) throws SQLException;
    }
}
