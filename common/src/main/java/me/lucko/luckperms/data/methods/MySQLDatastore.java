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

package me.lucko.luckperms.data.methods;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.data.MySQLConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLDatastore extends SQLDatastore {

    private static final String CREATETABLE_UUID = "CREATE TABLE IF NOT EXISTS `lp_uuid` (`name` VARCHAR(16) NOT NULL, `uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_USERS = "CREATE TABLE IF NOT EXISTS `lp_users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, `perms` TEXT NOT NULL, PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_GROUPS = "CREATE TABLE IF NOT EXISTS `lp_groups` (`name` VARCHAR(36) NOT NULL, `perms` TEXT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_TRACKS = "CREATE TABLE IF NOT EXISTS `lp_tracks` (`name` VARCHAR(36) NOT NULL, `groups` TEXT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";

    private final MySQLConfiguration configuration;
    private HikariDataSource hikari;

    public MySQLDatastore(LuckPermsPlugin plugin, MySQLConfiguration configuration) {
        super(plugin, "MySQL");
        this.configuration = configuration;
    }

    @Override
    public void init() {
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

        if (!setupTables(CREATETABLE_UUID, CREATETABLE_USERS, CREATETABLE_GROUPS, CREATETABLE_TRACKS)) {
            plugin.getLog().severe("Error occurred whilst initialising the database. All connections are disallowed.");
            shutdown();
        } else {
            setAcceptingLogins(true);
        }
    }

    @Override
    boolean runQuery(QueryPS queryPS) {
        boolean success = false;
        try {
            @Cleanup Connection connection = getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("SQL connection is null");
            }

            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(queryPS.getQuery());
            queryPS.onRun(preparedStatement);
            preparedStatement.execute();
            success = true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    @Override
    boolean runQuery(QueryRS queryRS) {
        boolean success = false;
        try {
            @Cleanup Connection connection = getConnection();
            if (connection == null || connection.isClosed()) {
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
