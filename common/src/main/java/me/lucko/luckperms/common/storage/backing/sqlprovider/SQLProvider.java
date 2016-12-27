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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RequiredArgsConstructor
public abstract class SQLProvider {
    private static final QueryPS EMPTY_PS = preparedStatement -> {};

    @Getter
    private final String name;

    public abstract void init() throws Exception;

    public abstract void shutdown() throws Exception;

    public abstract WrappedConnection getConnection() throws SQLException;

    public boolean runQuery(String query, QueryPS queryPS) {
        try {
            try (Connection connection = getConnection()) {
                if (connection == null || connection.isClosed()) {
                    throw new IllegalStateException("SQL connection is null");
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    queryPS.onRun(preparedStatement);

                    preparedStatement.execute();
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean runQuery(String query, QueryPS queryPS, QueryRS queryRS) {
        try {
            try (Connection connection = getConnection()) {
                if (connection == null || connection.isClosed()) {
                    throw new IllegalStateException("SQL connection is null");
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    queryPS.onRun(preparedStatement);

                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        return queryRS.onResult(resultSet);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean runQuery(String query) {
        return runQuery(query, EMPTY_PS);
    }

    public boolean runQuery(String query, QueryRS queryRS) {
        return runQuery(query, EMPTY_PS, queryRS);
    }

    @FunctionalInterface
    public interface QueryPS {
        void onRun(PreparedStatement preparedStatement) throws SQLException;
    }

    @FunctionalInterface
    public interface QueryRS {
        boolean onResult(ResultSet resultSet) throws SQLException;
    }

}
