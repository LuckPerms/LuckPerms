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

package me.lucko.luckperms.common.storage.implementation.sql.connection.hikari;

import com.google.common.collect.ImmutableList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.logging.PluginLogger;
import me.lucko.luckperms.common.storage.implementation.sql.connection.ConnectionFactory;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class HikariConnectionFactory implements ConnectionFactory {

    protected final StorageCredentials configuration;
    private HikariDataSource hikari;

    public HikariConnectionFactory(StorageCredentials configuration) {
        this.configuration = configuration;
    }

    protected String getDriverClass() {
        return null;
    }

    protected void appendProperties(HikariConfig config, Map<String, String> properties) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            config.addDataSourceProperty(property.getKey(), property.getValue());
        }
    }

    protected void appendConfigurationInfo(HikariConfig config) {
        String address = this.configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : "3306";

        config.setDataSourceClassName(getDriverClass());
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("port", port);
        config.addDataSourceProperty("databaseName", this.configuration.getDatabase());
        config.setUsername(this.configuration.getUsername());
        config.setPassword(this.configuration.getPassword());
    }

    @Override
    public void init(LuckPermsPlugin plugin) {
        HikariConfig config;
        try {
            config = new HikariConfig();
        } catch (LinkageError e) {
            // dumb plugins seem to keep doing stupid stuff with shading of SLF4J and Log4J.
            // detect this and print a more useful error message.
            handleLinkageError(e, plugin);
            throw e;
        }

        config.setPoolName("luckperms-hikari");

        appendConfigurationInfo(config);

        Map<String, String> properties = new HashMap<>(this.configuration.getProperties());

        // https://github.com/brettwooldridge/HikariCP/wiki/Rapid-Recovery
        properties.putIfAbsent("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));

        appendProperties(config, properties);

        config.setMaximumPoolSize(this.configuration.getMaxPoolSize());
        config.setMinimumIdle(this.configuration.getMinIdleConnections());
        config.setMaxLifetime(this.configuration.getMaxLifetime());
        config.setConnectionTimeout(this.configuration.getConnectionTimeout());

        // don't perform any initial connection validation - we subsequently call #getConnection
        // to setup the schema anyways
        config.setInitializationFailTimeout(-1);

        this.hikari = new HikariDataSource(config);
    }

    @Override
    public void shutdown() {
        if (this.hikari != null) {
            this.hikari.close();
        }
    }

    @Override
    public Map<String, String> getMeta() {
        Map<String, String> meta = new LinkedHashMap<>();
        boolean success = true;

        long start = System.currentTimeMillis();
        try (Connection c = getConnection()) {
            try (Statement s = c.createStatement()) {
                s.execute("/* ping */ SELECT 1");
            }
        } catch (SQLException e) {
            success = false;
        }
        long duration = System.currentTimeMillis() - start;

        if (success) {
            meta.put("Ping", "&a" + duration + "ms");
            meta.put("Connected", "true");
        } else {
            meta.put("Connected", "false");
        }

        return meta;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (this.hikari == null) {
            throw new SQLException("Unable to get a connection from the pool. (hikari is null)");
        }
        Connection connection = this.hikari.getConnection();
        if (connection == null) {
            throw new SQLException("Unable to get a connection from the pool. (getConnection returned null)");
        }
        return connection;
    }

    private static void handleLinkageError(LinkageError linkageError, LuckPermsPlugin plugin) {
        List<String> noteworthyClasses = ImmutableList.of(
                "org.slf4j.LoggerFactory",
                "org.slf4j.ILoggerFactory",
                "org.apache.logging.slf4j.Log4jLoggerFactory",
                "org.apache.logging.log4j.spi.LoggerContext",
                "org.apache.logging.log4j.spi.AbstractLoggerAdapter",
                "org.slf4j.impl.StaticLoggerBinder"
        );

        PluginLogger logger = plugin.getLogger();
        logger.warn("A " + linkageError.getClass().getSimpleName() + " has occurred whilst initialising Hikari. This is likely due to classloading conflicts between other plugins.");
        logger.warn("Please check for other plugins below (and try loading LuckPerms without them installed) before reporting the issue.");

        for (String className : noteworthyClasses) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
            } catch (Exception ex) {
                continue;
            }

            ClassLoader loader = clazz.getClassLoader();
            String loaderName;
            try {
                loaderName = plugin.getBootstrap().identifyClassLoader(loader) + " (" + loader.toString() + ")";
            } catch (Exception e) {
                loaderName = loader.toString();
            }

            logger.warn("Class " + className + " has been loaded by: " + loaderName);
        }
    }
}
