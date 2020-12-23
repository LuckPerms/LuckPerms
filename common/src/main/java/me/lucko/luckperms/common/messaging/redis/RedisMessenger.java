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

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;

import org.checkerframework.checker.nullness.qual.NonNull;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Protocol;

/**
 * An implementation of {@link Messenger} using Redis.
 */
public class RedisMessenger implements Messenger {
    private static final String CHANNEL = "luckperms:update";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private JedisPool jedisPool;
    private Subscription sub;

    public RedisMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init(String address, String password, boolean ssl) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : Protocol.DEFAULT_PORT;

        this.jedisPool = new JedisPool(new JedisPoolConfig(), host, port, Protocol.DEFAULT_TIMEOUT, password, ssl);

        this.sub = new Subscription(this);
        this.plugin.getBootstrap().getScheduler().executeAsync(this.sub);
    }

    @Override
    public void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        try (Jedis jedis = this.jedisPool.getResource()) {
            jedis.publish(CHANNEL, outgoingMessage.asEncodedString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        this.sub.unsubscribe();
        this.jedisPool.destroy();
    }

    private static class Subscription extends JedisPubSub implements Runnable {
        private final RedisMessenger parent;

        private Subscription(RedisMessenger parent) {
            this.parent = parent;
        }

        @Override
        public void run() {
            boolean wasBroken = false;
            while (!Thread.interrupted() && !this.parent.jedisPool.isClosed()) {
                try (Jedis jedis = this.parent.jedisPool.getResource()) {
                    if (wasBroken) {
                        this.parent.plugin.getLogger().info("Redis pubsub connection re-established");
                        wasBroken = false;
                    }
                    jedis.subscribe(this, CHANNEL);
                } catch (Exception e) {
                    wasBroken = true;
                    this.parent.plugin.getLogger().warn("Redis pubsub connection dropped, trying to re-open the connection: " + e.getMessage());
                    try {
                        unsubscribe();
                    } catch (Exception ignored) {

                    }

                    // Sleep for 2 seconds to prevent massive spam in console
                    try {
                        Thread.sleep(2000);
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
            this.parent.consumer.consumeIncomingMessageAsString(msg);
        }
    }

}
