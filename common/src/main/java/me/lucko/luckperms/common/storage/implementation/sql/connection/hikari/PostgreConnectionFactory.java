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

import com.zaxxer.hikari.HikariConfig;
import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.classloader.IsolatedClassLoader;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.misc.StorageCredentials;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

public class PostgreConnectionFactory extends HikariConnectionFactory {
    private final Class<?> dataSourceClass; // holds a strong reference

    public PostgreConnectionFactory(LuckPermsPlugin plugin, StorageCredentials configuration) {
        super(configuration);

        // setup the classloader
        IsolatedClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(EnumSet.of(Dependency.POSTGRESQL_DRIVER, Dependency.PGJDBC_NG_SPY));
        try {
            this.dataSourceClass = classLoader.loadClass("com.impossibl.postgres.jdbc.PGDataSource");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getImplementationName() {
        return "PostgreSQL";
    }

    @Override
    protected void appendProperties(HikariConfig config, Map<String, String> properties) {
        // remove the default config properties which don't exist for PostgreSQL
        properties.remove("useUnicode");
        properties.remove("characterEncoding");

        // socket timeout is named differently, and expected to be as integer, not string
        String socketTimeout = properties.remove("socketTimeout");
        if (socketTimeout != null) {
            config.addDataSourceProperty("networkTimeout", Integer.parseInt(socketTimeout));
        }

        // housekeeper and hikari do not work together very well
        config.addDataSourceProperty("housekeeper", false);

        super.appendProperties(config, properties);
    }

    @Override
    protected void appendConfigurationInfo(HikariConfig config) {
        String address = this.configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : 5432;

        String database = this.configuration.getDatabase();
        String username = this.configuration.getUsername();
        String password = this.configuration.getPassword();

        config.setDataSourceClassName(dataSourceClass.getName());
        config.addDataSourceProperty("serverName", address);
        config.addDataSourceProperty("portNumber", port);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", username);
        config.addDataSourceProperty("password", password);
    }

    @Override
    public Function<String, String> getStatementProcessor() {
        return s -> s.replace("'", "\"");
    }
}
