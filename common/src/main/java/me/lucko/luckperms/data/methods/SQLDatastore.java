package me.lucko.luckperms.data.methods;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.groups.GroupManager;
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
import java.util.logging.Level;

@SuppressWarnings("UnnecessaryLocalVariable")
public abstract class SQLDatastore extends Datastore {

    private static final Type NM_TYPE = new TypeToken<Map<String, Boolean>>(){}.getType();

    private static final String USER_INSERT = "INSERT INTO lp_users VALUES(?, ?, ?)";
    private static final String USER_SELECT = "SELECT * FROM lp_users WHERE uuid=?";
    private static final String USER_UPDATE = "UPDATE lp_users SET name=?, perms=? WHERE uuid=?";

    private static final String GROUP_INSERT = "INSERT INTO lp_groups VALUES(?, ?)";
    private static final String GROUP_SELECT = "SELECT perms FROM lp_groups WHERE name=?";
    private static final String GROUP_SELECT_ALL = "SELECT * FROM lp_groups";
    private static final String GROUP_UPDATE = "UPDATE lp_groups SET perms=? WHERE name=?";
    private static final String GROUP_DELETE = "DELETE FROM lp_groups WHERE name=?";

    private static final String UUIDCACHE_INSERT = "INSERT INTO lp_uuid VALUES(?, ?)";
    private static final String UUIDCACHE_SELECT = "SELECT uuid FROM lp_uuid WHERE name=?";
    private static final String UUIDCACHE_UPDATE = "UPDATE lp_uuid SET uuid=? WHERE name=?";

    private Gson gson;

    public SQLDatastore(LuckPermsPlugin plugin, String name) {
        super(plugin, name);
        gson = new Gson();
    }

    abstract Connection getConnection() throws SQLException;

    private static void executeQuery(Connection connection, String query, Closer closer) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        closer.add(preparedStatement);
        preparedStatement.execute();
        preparedStatement.close();
    }

    private boolean runQuery(Query query) {
        boolean success = false;

        Connection connection = null;
        Closer c = new Closer();
        try {
            connection = getConnection();
            if (connection == null) {
                throw new IllegalStateException("SQL connection is null");
            }
            success = query.onRun(connection, c);
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        } finally {
            c.closeAll();
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    void setupTables(String... tableQueries) {
        boolean success = runQuery((connection, closer) -> {
            for (String s : tableQueries) {
                executeQuery(connection, s, closer);
            }
            return true;
        });

        if (!success) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred whilst initialising the database. All connections are disallowed.");
            shutdown();
            setAcceptingLogins(false);
        }
    }

    @Override
    public boolean loadUser(UUID uuid) {
        User user = plugin.getUserManager().makeUser(uuid);
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, uuid.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);
            if (resultSet.next()) {
                user.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                user.setName(resultSet.getString("name"));
                return true;
            }
            return false;
        });

        // User updating and loading should be done sync as permission attachments are updated
        if (success) plugin.doSync(() -> plugin.getUserManager().updateOrSetUser(user));
        return success;
    }

    @Override
    public boolean loadOrCreateUser(UUID uuid, String username) {
        User user = plugin.getUserManager().makeUser(uuid, username);
        try {
            user.setPermission(plugin.getConfiguration().getDefaultGroupNode(), true);
        } catch (ObjectAlreadyHasException ignored) {}
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, user.getUuid().toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);
            if (!resultSet.next()) {
                PreparedStatement preparedStatement1 = connection.prepareStatement(USER_INSERT);
                closer.add(preparedStatement1);
                preparedStatement1.setString(1, user.getUuid().toString());
                preparedStatement1.setString(2, user.getName());
                preparedStatement1.setString(3, gson.toJson(user.getNodes()));
                preparedStatement1.execute();
            } else {
                user.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
            }
            return true;
        });

        // User updating and loading should be done sync as permission attachments are updated
        if (success) plugin.doSync(() -> plugin.getUserManager().updateOrSetUser(user));
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_UPDATE);
            closer.add(preparedStatement);
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, gson.toJson(user.getNodes()));
            preparedStatement.setString(3, user.getUuid().toString());
            preparedStatement.execute();
            return true;
        });
        return success;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, group.getName());

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);

            if (!resultSet.next()) {
                PreparedStatement preparedStatement1 = connection.prepareStatement(GROUP_INSERT);
                closer.add(preparedStatement1);
                preparedStatement1.setString(1, group.getName());
                preparedStatement1.setString(2, gson.toJson(group.getNodes()));
                preparedStatement1.execute();

            } else {
                group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
            }
            return true;
        });

        if (success) plugin.getGroupManager().updateOrSetGroup(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, name);

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);
            if (resultSet.next()) {
                group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                return true;
            }
            return false;
        });

        if (success) plugin.getGroupManager().updateOrSetGroup(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        List<Group> groups = new ArrayList<>();
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SELECT_ALL);
            closer.add(preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);
            while (resultSet.next()) {
                Group group = plugin.getGroupManager().makeGroup(resultSet.getString("name"));
                group.getNodes().putAll(gson.fromJson(resultSet.getString("perms"), NM_TYPE));
                groups.add(group);
            }
            return true;
        });

        GroupManager gm = plugin.getGroupManager();
        if (success) groups.forEach(gm::setGroup);
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_UPDATE);
            closer.add(preparedStatement);
            preparedStatement.setString(1, gson.toJson(group.getNodes()));
            preparedStatement.setString(2, group.getName());
            preparedStatement.execute();
            return true;
        });
        return success;
    }

    @Override
    public boolean deleteGroup(Group group) {
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_DELETE);
            closer.add(preparedStatement);
            preparedStatement.setString(1, group.getName());
            preparedStatement.execute();
            return true;
        });
        if (success) plugin.getGroupManager().unloadGroup(group);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(UUIDCACHE_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);

            PreparedStatement preparedStatement1;
            if (resultSet.next()) {
                preparedStatement1 = connection.prepareStatement(UUIDCACHE_UPDATE);
                closer.add(preparedStatement1);
                preparedStatement1.setString(1, uuid.toString());
                preparedStatement1.setString(2, username);

            } else {
                preparedStatement1 = connection.prepareStatement(UUIDCACHE_INSERT);
                closer.add(preparedStatement1);
                preparedStatement1.setString(1, username);
                preparedStatement1.setString(2, uuid.toString());
            }

            preparedStatement.execute();
            return true;
        });
        return success;
    }

    @Override
    public UUID getUUID(String username) {
        final UUID[] uuid = {null};
        boolean success = runQuery((connection, closer) -> {
            PreparedStatement preparedStatement = connection.prepareStatement(UUIDCACHE_SELECT);
            closer.add(preparedStatement);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();
            closer.add(resultSet);
            if (resultSet.next()) {
                uuid[0] = UUID.fromString(resultSet.getString("uuid"));
                return true;
            }
            return false;
        });

        return success ? uuid[0] : null;
    }

    interface Query {
        boolean onRun(Connection connection, Closer closer) throws SQLException;
    }

    private class Closer {
        private List<AutoCloseable> objects = new ArrayList<>();

        public void add(AutoCloseable a) {
            objects.add(a);
        }

        void closeAll() {
            objects.stream().filter(a -> a != null).forEach(a -> {
                try {
                    a.close();
                } catch (Exception ignored) {}
            });
        }
    }
}
