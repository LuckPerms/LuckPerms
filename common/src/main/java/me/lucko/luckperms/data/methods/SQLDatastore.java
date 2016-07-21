package me.lucko.luckperms.data.methods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.Getter;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.tracks.Track;
import me.lucko.luckperms.tracks.TrackManager;
import me.lucko.luckperms.users.User;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnnecessaryLocalVariable")
abstract class SQLDatastore extends Datastore {

    private static final Type NM_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();
    private static final Type T_TYPE = new TypeToken<List<String>>(){}.getType();

    private static final String USER_INSERT = "INSERT INTO lp_users VALUES(?, ?, ?, ?)";
    private static final String USER_SELECT = "SELECT * FROM lp_users WHERE uuid=?";
    private static final String USER_UPDATE = "UPDATE lp_users SET name=?, primary_group = ?, perms=? WHERE uuid=?";

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

    private final Gson gson;

    SQLDatastore(LuckPermsPlugin plugin, String name) {
        super(plugin, name);
        gson = new Gson();
    }

    abstract Connection getConnection() throws SQLException;

    private boolean runQuery(QueryPS queryPS) {
        boolean success = false;
        try {
            @Cleanup Connection connection = getConnection();
            if (connection == null) {
                throw new IllegalStateException("SQL connection is null");
            }

            @Cleanup PreparedStatement preparedStatement =  connection.prepareStatement(queryPS.getQuery());
            queryPS.onRun(preparedStatement);
            preparedStatement.execute();
            success = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    private boolean runQuery(QueryRS queryRS) {
        boolean success = false;
        try {
            @Cleanup Connection connection = getConnection();
            if (connection == null) {
                throw new IllegalStateException("SQL connection is null");
            }

            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(queryRS.getQuery());
            queryRS.onRun(preparedStatement);
            preparedStatement.execute();

            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            success = queryRS.onResult(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    boolean setupTables(String... tableQueries) {
        boolean success = true;
        for (String q : tableQueries) {
            if (!runQuery(new Query(q))) success = false;
        }

        return success;
    }

    @Override
    public boolean loadUser(UUID uuid) {
        User user = plugin.getUserManager().makeUser(uuid);
        boolean success = runQuery(new QueryRS(USER_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, uuid.toString());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    user.setName(resultSet.getString("name"));
                    user.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                    user.setPrimaryGroup(resultSet.getString("primary_group"));
                    return true;
                }
                return false;
            }
        });

        if (success) plugin.getUserManager().updateOrSetUser(user);
        return success;
    }

    @Override
    public boolean loadOrCreateUser(UUID uuid, String username) {
        User user = plugin.getUserManager().makeUser(uuid, username);
        boolean success = runQuery(new QueryRS(USER_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, user.getUuid().toString());
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean success = true;
                if (!resultSet.next()) {
                    plugin.getUserManager().giveDefaults(user);

                    success = runQuery(new QueryPS(USER_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, user.getUuid().toString());
                            preparedStatement.setString(2, user.getName());
                            preparedStatement.setString(3, user.getPrimaryGroup());
                            preparedStatement.setString(4, gson.toJson(user.getNodes()));
                        }
                    });
                } else {
                    user.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                    user.setPrimaryGroup(resultSet.getString("primary_group"));
                }
                return success;
            }
        });

        if (success) plugin.getUserManager().updateOrSetUser(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        boolean success = runQuery(new QueryPS(USER_UPDATE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, user.getName());
                preparedStatement.setString(2, user.getPrimaryGroup());
                preparedStatement.setString(3, gson.toJson(user.getNodes()));
                preparedStatement.setString(4, user.getUuid().toString());
            }
        });
        return success;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
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
                            preparedStatement.setString(2, gson.toJson(group.getNodes()));
                        }
                    });
                } else {
                    group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                }
                return success;
            }
        });

        if (success) plugin.getGroupManager().updateOrSetGroup(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
        boolean success = runQuery(new QueryRS(GROUP_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, name);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                if (resultSet.next()) {
                    group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                    return true;
                }
                return false;
            }
        });

        if (success) plugin.getGroupManager().updateOrSetGroup(group);
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
                    Group group = plugin.getGroupManager().makeGroup(resultSet.getString("name"));
                    group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                    groups.add(group);
                }
                return true;
            }
        });

        if (success) {
            GroupManager gm = plugin.getGroupManager();
            gm.unloadAll();
            groups.forEach(gm::setGroup);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        boolean success = runQuery(new QueryPS(GROUP_UPDATE) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, gson.toJson(group.getNodes()));
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

        if (success) plugin.getGroupManager().unloadGroup(group);
        return success;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().makeTrack(name);
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

        if (success) plugin.getTrackManager().updateOrSetTrack(track);
        return success;
    }

    @Override
    public boolean loadTrack(String name) {
        Track track = plugin.getTrackManager().makeTrack(name);
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

        if (success) plugin.getTrackManager().updateOrSetTrack(track);
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
                    Track track = plugin.getTrackManager().makeTrack(resultSet.getString("name"));
                    track.setGroups(gson.fromJson(resultSet.getString("groups"), T_TYPE));
                    tracks.add(track);
                }
                return true;
            }
        });

        if (success) {
            TrackManager tm = plugin.getTrackManager();
            tm.unloadAll();
            tracks.forEach(tm::setTrack);
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
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        boolean success = runQuery(new QueryRS(UUIDCACHE_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, username);
            }

            @Override
            boolean onResult(ResultSet resultSet) throws SQLException {
                boolean success;
                if (resultSet.next()) {
                    success = runQuery(new QueryPS(UUIDCACHE_UPDATE) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, uuid.toString());
                            preparedStatement.setString(2, username);
                        }
                    });
                } else {
                    success = runQuery(new QueryPS(UUIDCACHE_INSERT) {
                        @Override
                        void onRun(PreparedStatement preparedStatement) throws SQLException {
                            preparedStatement.setString(1, username);
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
        final UUID[] uuid = {null};

        boolean success = runQuery(new QueryRS(UUIDCACHE_SELECT) {
            @Override
            void onRun(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setString(1, username);
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
    private abstract class QueryPS {
        private final String query;
        abstract void onRun(PreparedStatement preparedStatement) throws SQLException;
    }

    @Getter
    @AllArgsConstructor
    private abstract class QueryRS {
        private final String query;
        abstract void onRun(PreparedStatement preparedStatement) throws SQLException;
        abstract boolean onResult(ResultSet resultSet) throws SQLException;
    }
}
