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

package me.lucko.luckperms.common.storage.backing.sqlprovider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.common.storage.DatastoreConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class MySQLProvider extends SQLProvider {

    private final DatastoreConfiguration configuration;
    private HikariDataSource hikari;

    public MySQLProvider(DatastoreConfiguration configuration) {
        super("MySQL");
        this.configuration = configuration;
    }

    @Override
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();

        String address = configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : "3306";

        String database = configuration.getDatabase();
        String username = configuration.getUsername();
        String password = configuration.getPassword();

        config.setMaximumPoolSize(configuration.getPoolSize());

        config.setPoolName("luckperms");
        config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("port", port);
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
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10)); // 10000
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(5)); // 5000
        config.setValidationTimeout(TimeUnit.SECONDS.toMillis(3)); // 3000
        config.setConnectionTestQuery("/* LuckPerms ping */ SELECT 1");

        hikari = new HikariDataSource(config);
    }

    @Override
    public void shutdown() throws Exception {
        if (hikari != null) {
            hikari.close();
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    @Override
    public boolean runQuery(String query, QueryPS queryPS) {
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
    public boolean runQuery(String query, QueryPS queryPS, QueryRS queryRS) {
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
}
