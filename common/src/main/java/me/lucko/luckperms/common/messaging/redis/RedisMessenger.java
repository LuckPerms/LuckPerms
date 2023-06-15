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

package me.lucko.luckperms.common.messaging.redis;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

/**
 * An implementation of {@link Messenger} using Redis.
 */
public class RedisMessenger implements Messenger {

    private static final String CHANNEL = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private /* final */ JedisCluster jedisCluster;
    private /* final */ JedisPool jedisPool;
    private /* final */ Subscription sub;
    private boolean closing = false;

    public RedisMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init(List<String> addresses, String username, String password, boolean ssl) {
        Set<HostAndPort> hosts = addresses.stream().map(s -> {
            String[] addressSplit = s.split(":");
            String host = addressSplit[0];
            int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : Protocol.DEFAULT_PORT;
            return new HostAndPort(host, port);
        }).collect(Collectors.toSet());
        DefaultJedisClientConfig.Builder jedisClientConfig = DefaultJedisClientConfig.builder()
                .password(password)
                .ssl(ssl)
                .timeoutMillis(Protocol.DEFAULT_TIMEOUT);
        if (username != null) jedisClientConfig.user(username);

        JedisClientConfig config = jedisClientConfig.build();
        try {
            this.jedisCluster = new JedisCluster(hosts, config);
            this.plugin.getLogger().info("Redis Cluster supported was detected!");
        } catch (JedisClusterOperationException e) {
            // The Redis cluster could not be initialized. Therefore, we do not use the cluster support.
            Optional<HostAndPort> hostAndPort = hosts.stream().findAny();
            if (hostAndPort.isPresent()) {
                this.jedisPool = new JedisPool(hostAndPort.get(), config);
            } else {
                this.plugin.getLogger().warn("No host for Redis could be found!");
                return; // If there is no host, then nothing will work anyway.
            }
        }

        this.sub = new Subscription();
        this.plugin.getBootstrap().getScheduler().executeAsync(this.sub);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        if (this.jedisPool != null) {
            try (Jedis jedis = this.jedisPool.getResource()) {
                jedis.publish(CHANNEL, outgoingMessage.asEncodedString());
            }
        } else if (this.jedisCluster != null) {
            this.jedisCluster.publish(CHANNEL, outgoingMessage.asEncodedString());
        }
    }

    @Override
    public void close() {
        this.closing = true;
        this.sub.unsubscribe();
        if (this.jedisCluster != null) this.jedisCluster.close();
        if (this.jedisPool != null) this.jedisPool.destroy();
    }

    private class Subscription extends JedisPubSub implements Runnable {

        @Override
        public void run() {
            boolean first = true;
            while (!RedisMessenger.this.closing && !Thread.interrupted() && this.isRedisAlive()) {
                Jedis jedis = null;
                try {
                    if (first) {
                        first = false;
                    } else {
                        RedisMessenger.this.plugin.getLogger().info("Redis pubsub connection re-established");
                    }

                    if (RedisMessenger.this.jedisCluster != null) {
                        RedisMessenger.this.jedisCluster.subscribe(this, CHANNEL); // blocking call
                    } else if (RedisMessenger.this.jedisPool != null) {
                        jedis = RedisMessenger.this.jedisPool.getResource();
                        jedis.subscribe(this, CHANNEL); // blocking call
                    }

                } catch (Exception e) {
                    if (RedisMessenger.this.closing) {
                        return;
                    }

                    RedisMessenger.this.plugin.getLogger().warn("Redis pubsub connection dropped, trying to re-open the connection", e);
                    try {
                        unsubscribe();
                    } catch (Exception ignored) {

                    }

                    // Sleep for 5 seconds to prevent massive spam in console
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    if (jedis != null) jedis.close();
                }
            }
        }

        @Override
        public void onMessage(String channel, String msg) {
            if (!channel.equals(CHANNEL)) {
                return;
            }
            RedisMessenger.this.consumer.consumeIncomingMessageAsString(msg);
        }

        private boolean isRedisAlive() {
            if (RedisMessenger.this.jedisCluster != null) return !RedisMessenger.this.jedisCluster.getClusterNodes().isEmpty();
            if (RedisMessenger.this.jedisPool != null) return !RedisMessenger.this.jedisPool.isClosed();
            return false;
        }
    }
}
