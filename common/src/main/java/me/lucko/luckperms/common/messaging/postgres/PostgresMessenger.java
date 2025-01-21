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

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;

import java.net.SocketException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An implementation of {@link Messenger} using Postgres.
 */
public class PostgresMessenger implements Messenger {

    private static final String CHANNEL = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final SqlStorage sqlStorage;
    private final IncomingMessageConsumer consumer;

    private NotificationListener listener;
    private SchedulerTask checkConnectionTask;

    public PostgresMessenger(LuckPermsPlugin plugin, SqlStorage sqlStorage, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.sqlStorage = sqlStorage;
        this.consumer = consumer;
    }

    public void init() {
        checkAndReopenConnection(true);
        this.checkConnectionTask = this.plugin.getBootstrap().getScheduler().asyncRepeating(() -> checkAndReopenConnection(false), 5, TimeUnit.SECONDS);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        try (Connection connection = this.sqlStorage.getConnectionFactory().getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
                ps.setString(1, CHANNEL);
                ps.setString(2, outgoingMessage.asEncodedString());
                ps.execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            this.checkConnectionTask.cancel();
            if (this.listener != null) {
                this.listener.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks the connection, and re-opens it if necessary.
     *
     * @return true if the connection is now alive, false otherwise
     */
    private boolean checkAndReopenConnection(boolean firstStartup) {
        boolean listenerActive = this.listener != null && this.listener.isListening();
        if (listenerActive) {
            return true;
        }

        // (re)create

        if (!firstStartup) {
            this.plugin.getLogger().warn("Postgres listen/notify connection dropped, trying to re-open the connection");
        }

        try {
            this.listener = new NotificationListener();
            this.plugin.getBootstrap().getScheduler().async(() -> {
                this.listener.listenAndBind();
                if (!firstStartup) {
                    this.plugin.getLogger().info("Postgres listen/notify connection re-established");
                }
            });
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private class NotificationListener implements AutoCloseable {
        private static final int RECEIVE_TIMEOUT_MILLIS = 1000;

        private final AtomicBoolean open = new AtomicBoolean(true);
        private final AtomicReference<Thread> listeningThread = new AtomicReference<>();

        public void listenAndBind() {
            try (Connection connection = PostgresMessenger.this.sqlStorage.getConnectionFactory().getConnection()) {
                try (Statement s = connection.createStatement()) {
                    s.execute("LISTEN \"" + CHANNEL + "\"");
                }

                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                this.listeningThread.set(Thread.currentThread());

                while (this.open.get()) {
                    PGNotification[] notifications = pgConnection.getNotifications(RECEIVE_TIMEOUT_MILLIS);
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            handleNotification(notification);
                        }
                    }
                }

            } catch (PSQLException e) {
                if (!(e.getCause() instanceof SocketException && e.getCause().getMessage().equals("Socket closed"))) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.listeningThread.set(null);
            }
        }

        public boolean isListening() {
            return this.listeningThread.get() != null;
        }

        public void handleNotification(PGNotification notification) {
            if (!CHANNEL.equals(notification.getName())) {
                return;
            }
            PostgresMessenger.this.consumer.consumeIncomingMessageAsString(notification.getParameter());
        }

        @Override
        public void close() {
            if (this.open.compareAndSet(true, false)) {
                Thread thread = this.listeningThread.get();
                if (thread != null) {
                    thread.interrupt();
                }
            }
        }
    }

}
