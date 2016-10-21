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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.DatastoreConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLBacking extends SQLBacking {

    private static final String CREATETABLE_UUID = "CREATE TABLE IF NOT EXISTS `lp_uuid` (`name` VARCHAR(16) NOT NULL, `uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_USERS = "CREATE TABLE IF NOT EXISTS `lp_users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, `perms` TEXT NOT NULL, PRIMARY KEY (`uuid`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_GROUPS = "CREATE TABLE IF NOT EXISTS `lp_groups` (`name` VARCHAR(36) NOT NULL, `perms` TEXT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_TRACKS = "CREATE TABLE IF NOT EXISTS `lp_tracks` (`name` VARCHAR(36) NOT NULL, `groups` TEXT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_ACTION = "CREATE TABLE IF NOT EXISTS `lp_actions` (`id` INT AUTO_INCREMENT NOT NULL, `time` BIGINT NOT NULL, `actor_uuid` VARCHAR(36) NOT NULL, `actor_name` VARCHAR(16) NOT NULL, `type` CHAR(1) NOT NULL, `acted_uuid` VARCHAR(36) NOT NULL, `acted_name` VARCHAR(36) NOT NULL, `action` VARCHAR(256) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET=utf8;";

    private final DatastoreConfiguration configuration;
    private HikariDataSource hikari;

    public MySQLBacking(LuckPermsPlugin plugin, DatastoreConfiguration configuration) {
        super(plugin, "MySQL");
        this.configuration = configuration;
    }

    @Override
    public void init() {
        HikariConfig config = new HikariConfig();

        final String address = configuration.getAddress();
        final String database = configuration.getDatabase();
        final String username = configuration.getUsername();
        final String password = configuration.getPassword();
        
        config.setMaximumPoolSize(configuration.getPoolSize());

        config.setPoolName("luckperms");
        config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        config.addDataSourceProperty("serverName", address.split(":")[0]);
        config.addDataSourceProperty("port", address.split(":")[1]);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", username);
        config.addDataSourceProperty("password", password);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cacheCallableStmts", true);
        config.addDataSourceProperty("alwaysSendSetIsolation", false);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("elideSetAutoCommits", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.setLeakDetectionThreshold(5000);

        hikari = new HikariDataSource(config);

        if (!setupTables(CREATETABLE_UUID, CREATETABLE_USERS, CREATETABLE_GROUPS, CREATETABLE_TRACKS, CREATETABLE_ACTION)) {
            plugin.getLog().severe("Error occurred whilst initialising the database.");
            shutdown();
        } else {
            setAcceptingLogins(true);
        }
    }

    @Override
    boolean runQuery(String query, QueryPS queryPS) {
        boolean success = false;

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("SQL connection is null");
            }

            preparedStatement = connection.prepareStatement(query);
            queryPS.onRun(preparedStatement);

            preparedStatement.execute();
            success = true;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(preparedStatement);
            close(connection);
        }
        return success;
    }

    @Override
    boolean runQuery(String query, QueryPS queryPS, QueryRS queryRS) {
        boolean success = false;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("SQL connection is null");
            }

            preparedStatement = connection.prepareStatement(query);
            queryPS.onRun(preparedStatement);

            resultSet = preparedStatement.executeQuery();
            success = queryRS.onResult(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(resultSet);
            close(preparedStatement);
            close(connection);
        }
        return success;
    }

    @Override
    public void shutdown() {
        if (hikari != null) {
            hikari.close();
        }
    }

    @Override
    Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }
}
