package me.lucko.luckperms.data;

import com.zaxxer.hikari.HikariDataSource;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.groups.GroupManager;
import me.lucko.luckperms.users.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class HikariDatastore extends Datastore {

    private static final String CREATETABLE_UUID = "CREATE TABLE IF NOT EXISTS `lp_uuid` (`name` VARCHAR(16) NOT NULL, `uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_USERS = "CREATE TABLE IF NOT EXISTS `lp_users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(16) NOT NULL, `perms` TEXT NOT NULL, PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_GROUPS = "CREATE TABLE IF NOT EXISTS `lp_groups` (`name` VARCHAR(36) NOT NULL, `perms` TEXT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";

    private static final String USER_INSERT = "INSERT INTO lp_users VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE name=?";
    private static final String USER_SELECT = "SELECT * FROM lp_users WHERE uuid=?";
    private static final String USER_SAVE = "UPDATE lp_users SET name=?, perms=? WHERE uuid=?";

    private static final String GROUP_INSERT = "INSERT INTO lp_groups VALUES(?, ?) ON DUPLICATE KEY UPDATE perms=?";
    private static final String GROUP_SELECT = "SELECT perms FROM lp_groups WHERE name=?";
    private static final String GROUP_SELECT_ALL = "SELECT * FROM lp_groups";
    private static final String GROUP_SAVE = "UPDATE lp_groups SET perms=? WHERE name=?";
    private static final String GROUP_DELETE = "DELETE FROM lp_groups WHERE name=?";

    private static final String UUIDCACHE_INSERT = "INSERT INTO lp_uuid VALUES(?, ?) ON DUPLICATE KEY UPDATE uuid=?";
    private static final String UUIDCACHE_SELECT = "SELECT uuid FROM lp_uuid WHERE name=?";

    private HikariDataSource hikari;

    public HikariDatastore(LuckPermsPlugin plugin) {
        super(plugin, "MySQL");
    }

    private static void executeQuery(Connection connection, String query) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.execute();
        preparedStatement.close();
    }

    private boolean runQuery(Query query) {
        boolean success = false;

        Connection connection = null;
        try {
            connection = hikari.getConnection();
            success = query.onRun(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            success = false;
        } finally {
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

    private void setupTables() {
        boolean success = runQuery(connection -> {
            executeQuery(connection, CREATETABLE_UUID);
            executeQuery(connection, CREATETABLE_USERS);
            executeQuery(connection, CREATETABLE_GROUPS);
            return true;
        });

        if (!success) {
            plugin.getLogger().log(Level.SEVERE, "Error occurred whilst connecting to the database. All connections are disallowed.");
            hikari.shutdown();
            setAcceptingLogins(false);
        }
    }

    @Override
    public void init(DatastoreConfiguration configuration) {
        hikari = new HikariDataSource();

        final String address = configuration.getAddress();
        final String database = configuration.getDatabase();
        final String username = configuration.getUsername();
        final String password = configuration.getPassword();

        hikari.setMaximumPoolSize(10);
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", address.split(":")[0]);
        hikari.addDataSourceProperty("port", address.split(":")[1]);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);

        setupTables();
    }

    @Override
    public boolean loadUser(UUID uuid) {
        User user = plugin.getUserManager().makeUser(uuid);
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_SELECT);
            preparedStatement.setString(1, uuid.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> nodes = new ArrayList<>();

            if (resultSet.next()) {
                if (!resultSet.getString("perms").equals("#")) {
                    nodes.addAll(Arrays.asList(resultSet.getString("perms").split(":")));
                }
                user.setName(resultSet.getString("name"));

                user.loadNodes(nodes);
                preparedStatement.close();
                resultSet.close();
                return true;
            }
            preparedStatement.close();
            resultSet.close();
            return false;
        });

        if (success) plugin.getUserManager().updateOrSetUser(user);
        return success;
    }

    @Override
    public boolean loadOrCreateUser(UUID uuid, String username) {
        User user = plugin.getUserManager().makeUser(uuid, username);
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_INSERT);
            preparedStatement.setString(1, uuid.toString());
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, plugin.getConfiguration().getDefaultGroupNode());
            preparedStatement.setString(4, username);
            preparedStatement.execute();
            preparedStatement.close();

            preparedStatement = connection.prepareStatement(USER_SELECT);
            preparedStatement.setString(1, uuid.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> nodes = new ArrayList<>();

            if (resultSet.next()) {
                if (!resultSet.getString("perms").equals("#")) {
                    nodes.addAll(Arrays.asList(resultSet.getString("perms").split(":")));
                }
                user.loadNodes(nodes);
                preparedStatement.close();
                resultSet.close();
                return true;
            }
            preparedStatement.close();
            resultSet.close();
            return true;
        });

        if (success) plugin.getUserManager().updateOrSetUser(user);
        return success;
    }

    @Override
    public boolean saveUser(User user) {
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(USER_SAVE);
            preparedStatement.setString(1, user.getName());
            preparedStatement.setString(2, user.serializeNodes());
            preparedStatement.setString(3, user.getUuid().toString());
            preparedStatement.execute();
            preparedStatement.close();
            return true;
        });
        return success;
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_INSERT);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, "#");
            preparedStatement.setString(3, "#");
            preparedStatement.execute();
            preparedStatement.close();

            preparedStatement = connection.prepareStatement(GROUP_SELECT);
            preparedStatement.setString(1, name);

            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> nodes = new ArrayList<>();

            if (resultSet.next()) {
                if (!resultSet.getString("perms").equals("#")) {
                    nodes.addAll(Arrays.asList(resultSet.getString("perms").split(":")));
                }
            }

            group.loadNodes(nodes);
            preparedStatement.close();
            resultSet.close();
            return true;
        });
        if (success) plugin.getGroupManager().updateOrSetGroup(group);
        return success;
    }

    @Override
    public boolean loadGroup(String name) {
        Group group = plugin.getGroupManager().makeGroup(name);
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SELECT);
            preparedStatement.setString(1, name);

            ResultSet resultSet = preparedStatement.executeQuery();
            List<String> nodes = new ArrayList<>();

            if (resultSet.next()) {
                if (!resultSet.getString("perms").equals("#")) {
                    nodes.addAll(Arrays.asList(resultSet.getString("perms").split(":")));
                }

                group.loadNodes(nodes);
                return true;
            }
            preparedStatement.close();
            resultSet.close();
            return false;
        });
        if (success) plugin.getGroupManager().updateOrSetGroup(group);
        return success;
    }

    @Override
    public boolean loadAllGroups() {
        List<Group> groups = new ArrayList<>();
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SELECT_ALL);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Group group = plugin.getGroupManager().makeGroup(resultSet.getString("name"));
                if (!resultSet.getString("perms").equals("#")) {
                    group.loadNodes(Arrays.asList(resultSet.getString("perms").split(":")));
                }
                groups.add(group);
            }
            preparedStatement.close();
            resultSet.close();
            return true;
        });

        GroupManager gm = plugin.getGroupManager();
        if (success) {
            groups.forEach(gm::setGroup);
        }
        return success;
    }

    @Override
    public boolean saveGroup(Group group) {
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_SAVE);
            preparedStatement.setString(1, group.serializeNodes());
            preparedStatement.setString(2, group.getName());
            preparedStatement.execute();
            preparedStatement.close();
            return true;
        });
        return success;
    }

    @Override
    public boolean deleteGroup(Group group) {
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(GROUP_DELETE);
            preparedStatement.setString(1, group.getName());
            preparedStatement.execute();
            preparedStatement.close();
            return true;
        });
        if (success) plugin.getGroupManager().unloadGroup(group);
        return success;
    }

    @Override
    public boolean saveUUIDData(String username, UUID uuid) {
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(UUIDCACHE_INSERT);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.setString(3, uuid.toString());
            preparedStatement.execute();
            preparedStatement.close();
            return true;
        });
        return success;
    }

    @Override
    public UUID getUUID(String username) {
        final UUID[] uuid = {null};
        boolean success = runQuery(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(UUIDCACHE_SELECT);
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                uuid[0] = UUID.fromString(resultSet.getString("uuid"));
                preparedStatement.close();
                resultSet.close();
                return true;
            }

            preparedStatement.close();
            resultSet.close();
            return false;
        });

        return success ? uuid[0] : null;
    }

    private interface Query {
        boolean onRun(Connection connection) throws SQLException;
    }
}
