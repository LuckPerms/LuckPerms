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
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class PostgreSQLProvider extends SQLProvider {

    private final DatastoreConfiguration configuration;
    private HikariDataSource hikari;

    public PostgreSQLProvider(DatastoreConfiguration configuration) {
        super("PostgreSQL");
        this.configuration = configuration;
    }

    @Override
    public void init() throws Exception {
        HikariConfig config = new HikariConfig();

        String address = configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : "5432";

        String database = configuration.getDatabase();
        String username = configuration.getUsername();
        String password = configuration.getPassword();

        config.setMaximumPoolSize(configuration.getPoolSize());

        config.setPoolName("luckperms");
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("portNumber", port);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", username);
        config.addDataSourceProperty("password", password);

        // We will wait for 15 seconds to get a connection from the pool.
        // Default is 30, but it shouldn't be taking that long.
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(15)); // 15000

        // If a connection is not returned within 10 seconds, it's probably safe to assume it's been leaked.
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(10)); // 10000

        // Just in-case the driver isn't JDBC4+
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
    public WrappedConnection getConnection() throws SQLException {
        Connection connection = hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        return new WrappedConnection(connection, true);
    }
}
