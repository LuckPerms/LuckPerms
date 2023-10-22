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

import me.lucko.luckperms.common.storage.StorageMetadata;
import me.lucko.luckperms.common.storage.implementation.sql.connection.ConnectionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstract {@link ConnectionFactory} using a file based database driver.
 */
abstract class FlatfileConnectionFactory implements ConnectionFactory {

    /** The current open connection, if any */
    private NonClosableConnection connection;
    /** The path to the database file */
    private final Path file;

    FlatfileConnectionFactory(Path file) {
        this.file = file;
    }

    /**
     * Creates a connection to the database.
     *
     * @param file the database file
     * @return the connection
     * @throws SQLException if any error occurs
     */
    protected abstract Connection createConnection(Path file) throws SQLException;

    @Override
    public synchronized Connection getConnection() throws SQLException {
        NonClosableConnection connection = this.connection;
        if (connection == null || connection.isClosed()) {
            connection = new NonClosableConnection(createConnection(this.file));
            this.connection = connection;
        }
        return connection;
    }

    @Override
    public void shutdown() throws Exception {
        if (this.connection != null) {
            this.connection.shutdown();
        }
    }

    /**
     * Gets the path of the file the database driver actually ends up writing to.
     *
     * @return the write file
     */
    protected Path getWriteFile() {
        return this.file;
    }

    protected void migrateOldDatabaseFile(String oldName) {
        Path oldFile = getWriteFile().getParent().resolve(oldName);
        if (Files.exists(oldFile)) {
            try {
                Files.move(oldFile, getWriteFile());
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public StorageMetadata getMeta() {
        StorageMetadata metadata = new StorageMetadata();

        Path databaseFile = getWriteFile();
        if (Files.exists(databaseFile)) {
            try {
                long length = Files.size(databaseFile);
                metadata.sizeBytes(length);
            } catch (IOException e) {
                // ignore
            }
        }

        return metadata;
    }
}
