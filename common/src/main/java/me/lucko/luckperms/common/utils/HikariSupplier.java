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

package me.lucko.luckperms.common.utils;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A simple hikari wrapper
 */
public class HikariSupplier implements AutoCloseable {

    private final String address;
    private final String database;
    private final String username;
    private final String password;

    private HikariDataSource hikari;

    public HikariSupplier(String address, String database, String username, String password) {
        this.address = address;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void setup(String poolName) {
        this.hikari = new HikariDataSource();
        this.hikari.setPoolName(poolName);
        this.hikari.setMaximumPoolSize(2);
        this.hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        this.hikari.addDataSourceProperty("serverName", this.address.split(":")[0]);
        this.hikari.addDataSourceProperty("port", this.address.split(":")[1]);
        this.hikari.addDataSourceProperty("databaseName", this.database);
        this.hikari.addDataSourceProperty("user", this.username);
        this.hikari.addDataSourceProperty("password", this.password);
    }

    @Override
    public void close() {
        this.hikari.close();
    }

    public Connection getConnection() throws SQLException {
        return this.hikari.getConnection();
    }
}
