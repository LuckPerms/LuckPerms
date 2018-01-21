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

package me.lucko.luckperms.common.storage.dao.sql.connection.file;

import org.sqlite.JDBC;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

public class SQLiteConnectionFactory extends FlatfileConnectionFactory {
    private final ReentrantLock lock = new ReentrantLock();
    private NonClosableConnection connection;

    public SQLiteConnectionFactory(File file) {
        super("SQLite", file);

        // backwards compat
        File data = new File(file.getParent(), "luckperms.sqlite");
        if (data.exists()) {
            data.renameTo(file);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        this.lock.lock();
        try {
            if (this.connection == null || this.connection.isClosed()) {
                Connection connection = JDBC.createConnection("jdbc:sqlite:" + this.file.getAbsolutePath(), new Properties());
                if (connection != null) {
                    this.connection = new NonClosableConnection(connection);
                }
            }

        } finally {
            this.lock.unlock();
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

}
