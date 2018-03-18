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

import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Represents a connection which cannot be closed using the standard {@link #close()} method.
 */
public interface NonClosableConnection extends Connection {

    /**
     * Creates a {@link NonClosableConnection} that delegates calls to the given {@link Connection}.
     *
     * @param connection the connection to wrap
     * @return a non closable connection
     */
    static NonClosableConnection wrap(Connection connection) {
        return (NonClosableConnection) Proxy.newProxyInstance(
                NonClosableConnection.class.getClassLoader(),
                new Class[]{NonClosableConnection.class},
                (proxy, method, args) -> {

                    // block calls directly to #close
                    if (method.getName().equals("close")) {
                        return null;
                    }

                    // proxy calls to #shutdown to the real #close method
                    if (method.getName().equals("shutdown")) {
                        connection.close();
                        return null;
                    }

                    // delegate all other calls
                    return method.invoke(connection, args);
                }
        );
    }

    /**
     * Actually {@link #close() closes} the underlying connection.
     */
    void shutdown();
}
