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

package me.lucko.luckperms.common.messaging.sql;

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An implementation of {@link Messenger} using SQL.
 */
public abstract class AbstractSqlMessenger implements Messenger {

    private final IncomingMessageConsumer consumer;
    private long lastId = -1;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean closed = false;

    protected AbstractSqlMessenger(IncomingMessageConsumer consumer) {
        this.consumer = consumer;
    }

    protected abstract Connection getConnection() throws SQLException;
    protected abstract String getTableName();

    public void init() throws SQLException {
        try (Connection c = getConnection()) {
            // init table
            String createStatement = "CREATE TABLE IF NOT EXISTS `" + getTableName() + "` (`id` INT AUTO_INCREMENT NOT NULL, `time` TIMESTAMP NOT NULL, `msg` TEXT NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET = utf8mb4";
            try (Statement s = c.createStatement()) {
                try {
                    s.execute(createStatement);
                } catch (SQLException e) {
                    if (e.getMessage().contains("Unknown character set")) {
                        // try again
                        s.execute(createStatement.replace("utf8mb4", "utf8"));
                    } else {
                        throw e;
                    }
                }
            }

            // pull last id
            try (PreparedStatement ps = c.prepareStatement("SELECT MAX(`id`) as `latest` FROM `" + getTableName() + "`")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        this.lastId = rs.getLong("latest");
                    }
                }
            }
        }
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        this.lock.readLock().lock();
        if (this.closed) {
            this.lock.readLock().unlock();
            return;
        }

        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO `" + getTableName() + "` (`time`, `msg`) VALUES(NOW(), ?)")) {
                ps.setString(1, outgoingMessage.asEncodedString());
                ps.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void pollMessages() {
        this.lock.readLock().lock();
        if (this.closed) {
            this.lock.readLock().unlock();
            return;
        }

        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("SELECT `id`, `msg` FROM `" + getTableName() + "` WHERE `id` > ? AND (NOW() - `time` < 30)")) {
                ps.setLong(1, this.lastId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long id = rs.getLong("id");
                        this.lastId = Math.max(this.lastId, id);

                        String message = rs.getString("msg");
                        this.consumer.consumeIncomingMessageAsString(message);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    public void runHousekeeping() {
        this.lock.readLock().lock();
        if (this.closed) {
            this.lock.readLock().unlock();
            return;
        }

        try (Connection c = getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM `" + getTableName() + "` WHERE (NOW() - `time` > 60)")) {
                ps.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        this.lock.writeLock().lock();
        try {
            this.closed = true;
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
