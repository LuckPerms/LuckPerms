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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

public abstract class FlatfileProvider extends SQLProvider {

    private final File file;
    private final ReentrantLock lock = new ReentrantLock();
    private Connection connection;

    public FlatfileProvider(String name, File file) {
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
    public Connection getConnection() throws SQLException {
        lock.lock();
        try {
            if (connection == null || connection.isClosed()) {
                try {
                    Class.forName(getDriverClass());
                } catch (ClassNotFoundException ignored) {}

                connection = DriverManager.getConnection(getDriverId() + ":" + file.getAbsolutePath());
            }

        } finally {
            lock.unlock();
        }

        return connection;
    }

    @Override
    public boolean runQuery(String query, QueryPS queryPS) {
        boolean success = false;

        PreparedStatement preparedStatement = null;

        try {
            Connection connection = getConnection();
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
        }
        return success;
    }

    @Override
    public boolean runQuery(String query, QueryPS queryPS, QueryRS queryRS) {
        boolean success = false;

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            Connection connection = getConnection();
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
        }
        return success;
    }
}
