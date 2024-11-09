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

package me.lucko.luckperms.common.storage.implementation.sql.builder;

import me.lucko.luckperms.common.storage.implementation.sql.StatementProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PreparedStatementBuilder {
    private final StringBuilder sb = new StringBuilder();
    private final List<String> variables = new ArrayList<>();

    public PreparedStatementBuilder() {

    }

    public PreparedStatementBuilder append(String s) {
        this.sb.append(s);
        return this;
    }

    public PreparedStatementBuilder append(char c) {
        this.sb.append(c);
        return this;
    }

    public PreparedStatementBuilder variable(String variable) {
        this.sb.append('?');
        this.variables.add(variable);
        return this;
    }

    public PreparedStatement build(Connection connection, StatementProcessor processor) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(processor.process(this.sb.toString()));
            for (int i = 0; i < this.variables.size(); i++) {
                String var = this.variables.get(i);
                statement.setString(i + 1, var);
            }
            return statement;
        } catch (SQLException e) {
            // if an exception is thrown, any try-with-resources block above this call won't be able to close the statement
            if (statement != null) {
                statement.close();
            }
            throw e;
        }
    }

    public String toReadableString() {
        String s = this.sb.toString();
        for (String var : this.variables) {
            s = s.replaceFirst("\\?", var);
        }
        return s;
    }

    public String toQueryString() {
        return this.sb.toString();
    }
}
