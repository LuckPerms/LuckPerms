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

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.MessengerProvider;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.messaging.redis.RedisMessenger;
import me.lucko.luckperms.common.messaging.sql.SqlMessenger;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.sql.SqlStorage;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MariaDbConnectionFactory;
import me.lucko.luckperms.common.storage.implementation.sql.connection.hikari.MySqlConnectionFactory;

import org.checkerframework.checker.nullness.qual.NonNull;

public class MessagingFactory<P extends LuckPermsPlugin> {
    private final P plugin;

    public MessagingFactory(P plugin) {
        this.plugin = plugin;
    }

    protected P getPlugin() {
        return this.plugin;
    }

    public final InternalMessagingService getInstance() {
        String messagingType = this.plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).toLowerCase();
        if (messagingType.equals("none") && this.plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            messagingType = "redis";
        }

        if (messagingType.equals("none") && this.plugin.getStorage().getImplementation() instanceof SqlStorage) {
            SqlStorage dao = (SqlStorage) this.plugin.getStorage().getImplementation();
            if (dao.getConnectionFactory() instanceof MySqlConnectionFactory || dao.getConnectionFactory() instanceof MariaDbConnectionFactory) {
                messagingType = "sql";
            }
        }

        if (messagingType.equals("none") || messagingType.equals("notsql")) {
            return null;
        }

        this.plugin.getLogger().info("Loading messaging service... [" + messagingType.toUpperCase() + "]");

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
                    e.printStackTrace();
                }
            } else {
                this.plugin.getLogger().warn("Messaging Service was set to redis, but redis is not enabled!");
            }
        } else if (messagingType.equals("sql")) {
            try {
                return new LuckPermsMessagingService(this.plugin, new SqlMessengerProvider());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private class RedisMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "Redis";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            RedisMessenger redis = new RedisMessenger(getPlugin(), incomingMessageConsumer);
            redis.init(getPlugin().getConfiguration().get(ConfigKeys.REDIS_ADDRESS), getPlugin().getConfiguration().get(ConfigKeys.REDIS_PASSWORD));
            return redis;
        }
    }

    private class SqlMessengerProvider implements MessengerProvider {

        @Override
        public @NonNull String getName() {
            return "Sql";
        }

        @Override
        public @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer) {
            SqlStorage dao = (SqlStorage) getPlugin().getStorage().getImplementation();
            Preconditions.checkState(dao.getConnectionFactory() instanceof MySqlConnectionFactory || dao.getConnectionFactory() instanceof MariaDbConnectionFactory, "not a supported sql type");

            SqlMessenger sql = new SqlMessenger(getPlugin(), dao, incomingMessageConsumer);
            sql.init();
            return sql;
        }
    }

}
