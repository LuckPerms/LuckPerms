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

package me.lucko.luckperms.common.storage;

import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import me.lucko.luckperms.common.storage.implementation.sql.StatementProcessor;
import me.lucko.luckperms.common.storage.implementation.sql.connection.ConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.file.H2ConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.file.NonClosableConnection;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SqlStorageTest extends AbstractStorageTest {

    @Override
    protected StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception {
        return new SqlStorage(plugin, new TestH2ConnectionFactory(), "luckperms_");
    }

    @Test
    public void testRecreateTables() throws Exception {
        SqlStorage sql = (SqlStorage) this.storage;

        LoggedAction testAction = LoggedAction.build()
                .source(UUID.randomUUID())
                .sourceName("Test")
                .targetType(Action.Target.Type.TRACK)
                .targetName("test")
                .description("test ")
                .timestamp(Instant.now())
                .build();

        // perform an action - ensure it works
        this.storage.logAction(testAction);

        // delete the table
        try (Connection c = sql.getConnectionFactory().getConnection()) {
            c.createStatement().execute("DROP TABLE `luckperms_actions`");
        }

        // perform the action again - expect an exception
        assertThrows(SQLException.class, () -> this.storage.logAction(testAction));

        // recreate the table & repeat the action, ensure it works
        this.storage.init();
        this.storage.logAction(testAction);
    }

    private static class TestH2ConnectionFactory implements ConnectionFactory {
        private final NonClosableConnection connection;

        TestH2ConnectionFactory() throws SQLException {
            this.connection = new NonClosableConnection(
                    DriverManager.getConnection("jdbc:h2:mem:test")
            );
        }

        @Override
        public Connection getConnection() {
            return this.connection;
        }

        @Override
        public String getImplementationName() {
            return "H2";
        }

        @Override
        public StorageMetadata getMeta() {
            return new StorageMetadata();
        }

        @Override
        public void init(LuckPermsPlugin plugin) {

        }

        @Override
        public StatementProcessor getStatementProcessor() {
            return H2ConnectionFactory.STATEMENT_PROCESSOR;
        }

        @Override
        public void shutdown() throws Exception {
            this.connection.shutdown();
        }
    }
}
