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

package me.lucko.luckperms.common.messaging.postgres;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.util.regex.Pattern;

/**
 * @author Mark Vainomaa
 */
public class PostgresMessenger implements Messenger {
    private static final String CHANNEL = "luckperms_update";
    private final LuckPermsPlugin plugin;
    private final SqlStorage storage;
    private final IncomingMessageConsumer consumer;
    private NotificationListener listener;

    public PostgresMessenger(LuckPermsPlugin plugin, SqlStorage sqlStorage, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.storage = sqlStorage;
        this.consumer = consumer;
    }

    public void init() {
        this.listener = new NotificationListener(this);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        try (PGConnection conn = this.storage.getConnectionFactory().getConnection().unwrap(PGConnection.class)) {
            try (PreparedStatement stmt = conn.prepareCall("SELECT pg_notify(?, ?);")) {
                stmt.setString(1, CHANNEL);
                stmt.setString(2, outgoingMessage.asEncodedString());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.listener.close();
    }

    private static class NotificationListener implements PGNotificationListener, Runnable, Closeable {
        private final Object closeLock = new Object();
        private boolean closed = false;
        private final PostgresMessenger parent;

        public NotificationListener(PostgresMessenger parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            boolean wasBroken = false;

            while (!Thread.interrupted()) {
                synchronized (this.closeLock) {
                    if (this.closed) {
                        break;
                    }
                }

                try (PGConnection conn = this.parent.storage.getConnectionFactory().getConnection().unwrap(PGConnection.class)) {
                    if (wasBroken) {
                        parent.plugin.getLogger().info("PostgreSQL listen connection re-established");
                        //wasBroken = false;
                    }

                    conn.addNotificationListener(Pattern.quote(CHANNEL), this);

                    synchronized (this.closeLock) {
                        closeLock.wait();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                wasBroken = true;
                parent.plugin.getLogger().warn("PostgreSQL listen connection dropped, trying to re-open the connection");

                // Sleep for 2 seconds to prevent massive spam in console
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public void close() {
            synchronized (this.closeLock) {
                if (this.closed) {
                    return;
                }

                this.closed = true;
                this.closeLock.notify();
            }
        }

        @Override
        public void notification(int processId, String channelName, String payload) {
            if (!channelName.equals(CHANNEL)) {
                return;
            }
            this.parent.consumer.consumeIncomingMessageAsString(payload);
        }

        @Override
        public void closed() {
            synchronized (this.closeLock) {
                this.closeLock.notify();
            }
        }
    }
}
