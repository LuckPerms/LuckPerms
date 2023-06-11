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
import java.util.Set;
import java.util.stream.Collectors;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;

/**
 * An implementation of {@link Messenger} using Redis.
 */
public class RedisMessenger implements Messenger {
    private static final String CHANNEL = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private /* final */ JedisCluster jedisCluster;
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

        this.jedisCluster = new JedisCluster(hosts, jedisClientConfig.build());
        this.sub = new Subscription();
        this.plugin.getBootstrap().getScheduler().executeAsync(this.sub);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        this.jedisCluster.publish(CHANNEL, outgoingMessage.asEncodedString());
    }

    @Override
    public void close() {
        this.closing = true;
        this.sub.unsubscribe();
        this.jedisCluster.close();
    }

    private class Subscription extends JedisPubSub implements Runnable {

        @Override
        public void run() {
            boolean first = true;
            while (!RedisMessenger.this.closing && !Thread.interrupted() && !RedisMessenger.this.jedisCluster.getClusterNodes().isEmpty()) {
                try {
                    if (first) {
                        first = false;
                    } else {
                        RedisMessenger.this.plugin.getLogger().info("Redis pubsub connection re-established");
                    }

                    RedisMessenger.this.jedisCluster.subscribe(this, CHANNEL); // blocking call
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
    }

}
