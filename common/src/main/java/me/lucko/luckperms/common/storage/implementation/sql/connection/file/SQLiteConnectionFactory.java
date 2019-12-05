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

package me.lucko.luckperms.common.storage.implementation.sql.connection.file;

import me.lucko.luckperms.common.dependencies.Dependency;
import me.lucko.luckperms.common.dependencies.classloader.IsolatedClassLoader;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Properties;
import java.util.function.Function;

public class SQLiteConnectionFactory extends FlatfileConnectionFactory {

    // the method invoked to obtain new connection instances
    private final Method createConnectionMethod;
    // the active connection
    private NonClosableConnection connection;

    public SQLiteConnectionFactory(LuckPermsPlugin plugin, Path file) {
        super(file);

        // backwards compat
        Path data = file.getParent().resolve("luckperms.sqlite");
        if (Files.exists(data)) {
            try {
                Files.move(data, file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // setup the classloader
        IsolatedClassLoader classLoader = plugin.getDependencyManager().obtainClassLoaderWith(EnumSet.of(Dependency.SQLITE_DRIVER));
        try {
            Class<?> jdcbClass = classLoader.loadClass("org.sqlite.JDBC");
            this.createConnectionMethod = jdcbClass.getMethod("createConnection", String.class, Properties.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getImplementationName() {
        return "SQLite";
    }

    private Connection createConnection(String url) throws SQLException {
        try {
            return (Connection) this.createConnectionMethod.invoke(null, url, new Properties());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SQLException) {
                throw ((SQLException) e.getCause());
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            Connection connection = createConnection("jdbc:sqlite:" + this.file.toString());
            if (connection != null) {
                this.connection = NonClosableConnection.wrap(connection);
            }
        }

        if (this.connection == null) {
            throw new SQLException("Unable to get a connection.");
        }

        return this.connection;
    }

    @Override
    public void shutdown() throws Exception {
        if (this.connection != null) {
            this.connection.shutdown();
        }
    }

    @Override
    public Function<String, String> getStatementProcessor() {
        return s -> s.replace("'", "`");
    }

}
