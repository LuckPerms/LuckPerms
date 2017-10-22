/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.storage.backing.sql.provider.remote;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.common.storage.DatastoreConfiguration;
import me.lucko.luckperms.common.storage.backing.sql.provider.AbstractConnectionFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MySqlConnectionFactory extends AbstractConnectionFactory {

    private final DatastoreConfiguration configuration;
    private final String driverClass;
    private HikariDataSource hikari;

    public MySqlConnectionFactory(String name, String driverClass, DatastoreConfiguration configuration) {
        super(name);
        this.configuration = configuration;
        this.driverClass = driverClass;
    }

    @Override
    public void init() {
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
        config.setDataSourceClassName(driverClass);
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("port", port);
        config.addDataSourceProperty("databaseName", database);
        config.setUsername(username);
        config.setPassword(password);

        if (!getName().toLowerCase().equals("mariadb")) {

            // doesn't exist on the MariaDB driver
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("alwaysSendSetIsolation", "false");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");

            // already set as default on mariadb
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("cacheCallableStmts", "true");

            // make sure unicode characters can be used.
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.addDataSourceProperty("useUnicode", "true");
        } else {
            // hack for mariadb. this will call #setProperties on the datasource, which will append these options
            // onto the connections.
            config.addDataSourceProperty("properties", "useUnicode=true;characterEncoding=utf8");
        }

        // We will wait for 15 seconds to get a connection from the pool.
        // Default is 30, but it shouldn't be taking that long.
        config.setConnectionTimeout(TimeUnit.SECONDS.toMillis(15)); // 15000

        // If a connection is not returned within 10 seconds, it's probably safe to assume it's been leaked.
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(10)); // 10000

        // The drivers are really old in some of the older Spigot binaries, so Connection#isValid doesn't work.
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
    public Map<String, String> getMeta() {
        Map<String, String> ret = new LinkedHashMap<>();
        boolean success = true;

        long start = System.currentTimeMillis();
        try (Connection c = hikari.getConnection()) {
            try (Statement s = c.createStatement()) {
                s.execute("/* ping */ SELECT 1");
            }
        } catch (SQLException e) {
            success = false;
        }
        long duration = System.currentTimeMillis() - start;

        if (success) {
            ret.put("Ping", "&a" + duration + "ms");
            ret.put("Connected", "true");
        } else {
            ret.put("Connected", "false");
        }

        return ret;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool.");
        }
        return connection;
    }
}
