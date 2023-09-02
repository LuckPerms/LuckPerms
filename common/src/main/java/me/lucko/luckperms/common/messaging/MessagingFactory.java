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

package me.lucko.luckperms.common.messaging;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.messaging.nats.NatsMessenger;
import me.lucko.luckperms.common.messaging.postgres.PostgresMessenger;
import me.lucko.luckperms.common.messaging.rabbitmq.RabbitMQMessenger;
import me.lucko.luckperms.common.messaging.redis.RedisMessenger;
import me.lucko.luckperms.common.messaging.sql.SqlMessenger;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MariaDbConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MySqlConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.PostgresConnectionFactory;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.MessengerProvider;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessagingFactory<P extends LuckPermsPlugin> {
    private final P plugin;

    public MessagingFactory(P plugin) {
        this.plugin = plugin;
    }

    protected P getPlugin() {
        return this.plugin;
    }

    public final InternalMessagingService getInstance() {
        String messagingType = this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE);
        if (messagingType.equals("none")) {
            messagingType = "auto";
        }

        // attempt to detect "auto" messaging service type.
        if (messagingType.equals("auto")) {
            if (this.plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
                messagingType = "redis";
            } else if (this.plugin.getConfiguration().get(ConfigKeys.RABBITMQ_ENABLED)) {
                messagingType = "rabbitmq";
            } else if (this.plugin.getConfiguration().get(ConfigKeys.NATS_ENABLED)) {
                messagingType = "nats";
            } else {
                for (StorageImplementation implementation : this.plugin.getStorage().getImplementations()) {
                    if (implementation instanceof SqlStorage) {
                        SqlStorage sql = (SqlStorage) implementation;
                        if (sql.getConnectionFactory() instanceof MySqlConnectionFactory || sql.getConnectionFactory() instanceof MariaDbConnectionFactory) {
                            messagingType = "sql";
                            break;
                        }
                        if (sql.getConnectionFactory() instanceof PostgresConnectionFactory) {
                            messagingType = "postgresql";
                            break;
                        }
                    }
                }
            }
        }

        if (messagingType.equals("auto") || messagingType.equals("notsql")) {
            return null;
        }

        if (messagingType.equals("custom")) {
            this.plugin.getLogger().info("Messaging service is set to custom. No service is initialized at this stage yet.");
            return null;
        }

        this.plugin.getLogger().info("Loading messaging service... [" + messagingType.toUpperCase(Locale.ROOT) + "]");
        InternalMessagingService service = getServiceFor(messagingType);
        if (service != null) {
            return service;
        }
        this.plugin.getLogger().warn("Messaging service '" + messagingType + "' not recognised.");
        return null;
    }

    protected InternalMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("redis")) {
            if (this.plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
                try {
                    return new LuckPermsMessagingService(this.plugin, new RedisMessengerProvider());
                } catch (Exception e) {
                    getPlugin().getLogger().severe("Exception occurred whilst enabling Redis messaging service", e);
                }
            } else {
                this.plugin.getLogger().warn("Messaging Service was set to redis, but redis is not enabled!");
            }
        } else if (messagingType.equals("nats")) {
            if (this.plugin.getConfiguration().get(ConfigKeys.NATS_ENABLED)) {
                try {
                    return new LuckPermsMessagingService(this.plugin, new NatsMesengerProvider());
                } catch (Exception e) {
                    getPlugin().getLogger().severe("Exception occurred whilst enabling Nats messaging service", e);
                }
            } else {
                this.plugin.getLogger().warn("Messaging Service was set to nats, but nats is not enabled!");
            }
        } else if (messagingType.equals("rabbitmq")) {
            if (this.plugin.getConfiguration().get(ConfigKeys.RABBITMQ_ENABLED)) {
                try {
                    return new LuckPermsMessagingService(this.plugin, new RabbitMQMessengerProvider());
                } catch (Exception e) {
                    getPlugin().getLogger().severe("Exception occurred whilst enabling RabbitMQ messaging service", e);
                }
            } else {
                this.plugin.getLogger().warn("Messaging Service was set to rabbitmq, but rabbitmq is not enabled!");
            }
        } else if (messagingType.equals("sql")) {
            try {
                return new LuckPermsMessagingService(this.plugin, new SqlMessengerProvider());
            } catch (Exception e) {
                getPlugin().getLogger().severe("Exception occurred whilst enabling SQL messaging service", e);
            }
        } else if (messagingType.equals("postgresql")) {
            try {
                return new LuckPermsMessagingService(this.plugin, new PostgresMessengerProvider());
            } catch (Exception e) {
                getPlugin().getLogger().severe("Exception occurred whilst enabling Postgres messaging service", e);
            }
        }

        return null;
    }

    private class NatsMesengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "Nats";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            NatsMessenger natsMessenger = new NatsMessenger(getPlugin(), incomingMessageConsumer);

            LuckPermsConfiguration configuration = getPlugin().getConfiguration();
            String address = configuration.get(ConfigKeys.NATS_ADDRESS);
            String username = configuration.get(ConfigKeys.NATS_USERNAME);
            String password = configuration.get(ConfigKeys.NATS_PASSWORD);
            if (password.isEmpty()) {
                password = null;
            }
            if (username.isEmpty()) {
                username = null;
            }
            boolean ssl = configuration.get(ConfigKeys.NATS_SSL);

            natsMessenger.init(address, username, password, ssl);
            return natsMessenger;
        }
    }

    private class RedisMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "Redis";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            RedisMessenger redis = new RedisMessenger(getPlugin(), incomingMessageConsumer);

            LuckPermsConfiguration config = getPlugin().getConfiguration();
            String address = config.get(ConfigKeys.REDIS_ADDRESS);
            List<String> addresses = config.get(ConfigKeys.REDIS_ADDRESSES);
            String username = config.get(ConfigKeys.REDIS_USERNAME);
            String password = config.get(ConfigKeys.REDIS_PASSWORD);
            if (password.isEmpty()) {
                password = null;
            }
            if (username.isEmpty()) {
                username = null;
            }
            boolean ssl = config.get(ConfigKeys.REDIS_SSL);

            if (!addresses.isEmpty()) {
                // redis cluster
                addresses = new ArrayList<>(addresses);
                if (address != null) {
                    addresses.add(address);
                }
                redis.init(addresses, username, password, ssl);
            } else {
                // redis pool
                redis.init(address, username, password, ssl);
            }

            return redis;
        }
    }

    private class RabbitMQMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "RabbitMQ";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            RabbitMQMessenger rabbitmq = new RabbitMQMessenger(getPlugin(), incomingMessageConsumer);

            LuckPermsConfiguration config = getPlugin().getConfiguration();
            String address = config.get(ConfigKeys.RABBITMQ_ADDRESS);
            String virtualHost = config.get(ConfigKeys.RABBITMQ_VIRTUAL_HOST);
            String username = config.get(ConfigKeys.RABBITMQ_USERNAME);
            String password = config.get(ConfigKeys.RABBITMQ_PASSWORD);

            rabbitmq.init(address, virtualHost, username, password);
            return rabbitmq;
        }
    }

    private class SqlMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "Sql";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            for (StorageImplementation implementation : getPlugin().getStorage().getImplementations()) {
                if (implementation instanceof SqlStorage) {
                    SqlStorage storage = (SqlStorage) implementation;
                    if (storage.getConnectionFactory() instanceof MySqlConnectionFactory || storage.getConnectionFactory() instanceof MariaDbConnectionFactory) {
                        // found an implementation match!
                        SqlMessenger sql = new SqlMessenger(getPlugin(), storage, incomingMessageConsumer);
                        sql.init();
                        return sql;
                    }
                }
            }

            throw new IllegalStateException("Can't find a supported sql storage implementation");
        }
    }

    private class PostgresMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "PostgreSQL";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            for (StorageImplementation implementation : getPlugin().getStorage().getImplementations()) {
                if (implementation instanceof SqlStorage) {
                    SqlStorage storage = (SqlStorage) implementation;
                    if (storage.getConnectionFactory() instanceof PostgresConnectionFactory) {
                        // found an implementation match!
                        PostgresMessenger messenger = new PostgresMessenger(getPlugin(), storage, incomingMessageConsumer);
                        messenger.init();
                        return messenger;
                    }
                }
            }

            throw new IllegalStateException("Can't find a supported sql storage implementation");
        }
    }

}
