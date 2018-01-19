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

import me.lucko.luckperms.api.messenger.IncomingMessageConsumer;
import me.lucko.luckperms.api.messenger.Messenger;
import me.lucko.luckperms.api.messenger.message.OutgoingMessage;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.Nonnull;

/**
 * An implementation of {@link Messenger} using Redis.
 */
public class RedisMessenger implements Messenger {
    private static final String CHANNEL = "lpuc";

    private final LuckPermsPlugin plugin;
    private final IncomingMessageConsumer consumer;

    private JedisPool jedisPool;
    private LPSub sub;

    public RedisMessenger(LuckPermsPlugin plugin, IncomingMessageConsumer consumer) {
        this.plugin = plugin;
        this.consumer = consumer;
    }

    public void init(String address, String password) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : 6379;

        if (password.equals("")) {
            this.jedisPool = new JedisPool(new JedisPoolConfig(), host, port);
        } else {
            this.jedisPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
        }

        this.plugin.getScheduler().doAsync(() -> {
            this.sub = new LPSub(this);
            try (Jedis jedis = this.jedisPool.getResource()) {
                jedis.subscribe(this.sub, CHANNEL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void sendOutgoingMessage(@Nonnull OutgoingMessage outgoingMessage) {
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

    private static class LPSub extends JedisPubSub {
        private final RedisMessenger parent;

        public LPSub(RedisMessenger parent) {
            this.parent = parent;
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
