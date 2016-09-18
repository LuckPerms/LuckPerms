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

import lombok.Cleanup;
import me.lucko.luckperms.LuckPermsPlugin;

import java.io.File;
import java.sql.*;

public class H2Datastore extends SQLDatastore {

    private static final String CREATETABLE_UUID = "CREATE TABLE IF NOT EXISTS `lp_uuid` (`name` VARCHAR(16) NOT NULL, `uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_USERS = "CREATE TABLE IF NOT EXISTS `lp_users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(16) NOT NULL, `primary_group` VARCHAR(36) NOT NULL, `perms` TEXT NOT NULL, PRIMARY KEY (`uuid`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_GROUPS = "CREATE TABLE IF NOT EXISTS `lp_groups` (`name` VARCHAR(36) NOT NULL, `perms` TEXT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_TRACKS = "CREATE TABLE IF NOT EXISTS `lp_tracks` (`name` VARCHAR(36) NOT NULL, `groups` TEXT NULL, PRIMARY KEY (`name`)) DEFAULT CHARSET=utf8;";
    private static final String CREATETABLE_ACTION = "CREATE TABLE IF NOT EXISTS `lp_actions` (`id` INT AUTO_INCREMENT NOT NULL, `time` BIGINT NOT NULL, `actor_uuid` VARCHAR(36) NOT NULL, `actor_name` VARCHAR(16) NOT NULL, `type` CHAR(1) NOT NULL, `acted_uuid` VARCHAR(36) NOT NULL, `acted_name` VARCHAR(36) NOT NULL, `action` VARCHAR(256) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET=utf8;";

    private final File file;
    private Connection connection = null;
    private final Object connectionLock = new Object();

    public H2Datastore(LuckPermsPlugin plugin, File file) {
        super(plugin, "H2");
        this.file = file;
    }

    @Override
    public void init() {
        if (!setupTables(CREATETABLE_UUID, CREATETABLE_USERS, CREATETABLE_GROUPS, CREATETABLE_TRACKS, CREATETABLE_ACTION)) {
            plugin.getLog().severe("Error occurred whilst initialising the database.");
            shutdown();
        } else {
            setAcceptingLogins(true);
        }
    }

    @Override
    boolean runQuery(QueryPS queryPS) {
        boolean success = false;
        try {
            Connection connection = getConnection();
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
            Connection connection = getConnection();
            if (connection == null || connection.isClosed()) {
                throw new IllegalStateException("SQL connection is null");
            }

            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement(queryRS.getQuery());
            queryRS.onRun(preparedStatement);

            @Cleanup ResultSet resultSet = preparedStatement.executeQuery();
            success = queryRS.onResult(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return success;
    }

    @Override
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    Connection getConnection() throws SQLException {
        synchronized (connectionLock) {
            if (connection == null || connection.isClosed()) {
                try {
                    Class.forName("org.h2.Driver");
                } catch (ClassNotFoundException ignored) {}

                connection = DriverManager.getConnection("jdbc:h2:" + file.getAbsolutePath());
            }
        }

        return connection;
    }
}
