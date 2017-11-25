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

package me.lucko.luckperms.common.storage.dao.sql.connection.hikari;

import com.zaxxer.hikari.HikariConfig;

import me.lucko.luckperms.common.storage.StorageCredentials;

public class MySqlConnectionFactory extends HikariConnectionFactory {
    public MySqlConnectionFactory(StorageCredentials configuration) {
        super("MySQL", configuration);
    }

    @Override
    protected void appendConfigurationInfo(HikariConfig config) {
        String address = configuration.getAddress();
        String[] addressSplit = address.split(":");
        address = addressSplit[0];
        String port = addressSplit.length > 1 ? addressSplit[1] : "3306";
        String database = configuration.getDatabase();

        config.setJdbcUrl("jdbc:mysql://" + address + ":" + port + "/" + database);
        config.setUsername(configuration.getUsername());
        config.setPassword(configuration.getPassword());
    }

    @Override
    protected void appendProperties(HikariConfig config, StorageCredentials credentials) {
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("alwaysSendSetIsolation", "false");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");

        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("cacheCallableStmts", "true");

        // append configurable properties
        super.appendProperties(config, credentials);
    }

}
