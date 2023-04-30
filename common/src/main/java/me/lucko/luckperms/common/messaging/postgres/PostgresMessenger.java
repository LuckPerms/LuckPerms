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
import me.lucko.luckperms.common.plugin.scheduler.SchedulerTask;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
        try (PGConnection connection = this.sqlStorage.getConnectionFactory().getConnection().unwrap(PGConnection.class)) {
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
            this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
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

    private class NotificationListener implements PGNotificationListener, AutoCloseable {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicBoolean listening = new AtomicBoolean(false);

        public void listenAndBind() {
            try (PGConnection connection = PostgresMessenger.this.sqlStorage.getConnectionFactory().getConnection().unwrap(PGConnection.class)) {
                connection.addNotificationListener(CHANNEL, this);

                try (Statement s = connection.createStatement()) {
                    s.execute("LISTEN \"" + CHANNEL + "\"");
                }

                this.listening.set(true);
                this.latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                this.listening.set(false);
            }
        }

        public boolean isListening() {
            return this.listening.get();
        }

        @Override
        public void notification(int processId, String channelName, String payload) {
            if (!CHANNEL.equals(channelName)) {
                return;
            }
            PostgresMessenger.this.consumer.consumeIncomingMessageAsString(payload);
        }

        @Override
        public void closed() {
            this.latch.countDown();
        }

        @Override
        public void close() {
            this.latch.countDown();
        }
    }

}
