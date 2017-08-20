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

package me.lucko.luckperms.common.storage.backing.sql.provider;

import me.lucko.luckperms.common.storage.backing.sql.WrappedConnection;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.concurrent.locks.ReentrantLock;

abstract class FlatfileProvider extends SQLProvider {
    protected static final DecimalFormat DF = new DecimalFormat("#.00");

    protected final File file;
    private final ReentrantLock lock = new ReentrantLock();
    private WrappedConnection connection;

    FlatfileProvider(String name, File file) {
        super(name);
        this.file = file;
    }

    protected abstract String getDriverClass();
    protected abstract String getDriverId();

    @Override
    public void init() throws Exception {

    }

    @Override
    public void shutdown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Override
    public WrappedConnection getConnection() throws SQLException {
        lock.lock();
        try {
            if (this.connection == null || this.connection.isClosed()) {
                try {
                    Class.forName(getDriverClass());
                } catch (ClassNotFoundException ignored) {}

                Connection connection = DriverManager.getConnection(getDriverId() + ":" + file.getAbsolutePath());
                if (connection != null) {
                    this.connection = new WrappedConnection(connection, false);
                }
            }

        } finally {
            lock.unlock();
        }

        if (this.connection == null) {
            throw new SQLException("Connection is null");
        }

        return this.connection;
    }
}
