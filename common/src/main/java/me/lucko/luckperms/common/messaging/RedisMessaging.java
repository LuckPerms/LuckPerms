/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.LuckPermsPlugin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * An implementation of {@link me.lucko.luckperms.api.MessagingService} using Redis.
 */
public class RedisMessaging extends AbstractMessagingService {
    private final LuckPermsPlugin plugin;
    private JedisPool jedisPool;
    private LPSub sub;

    public RedisMessaging(LuckPermsPlugin plugin) {
        super(plugin, "Redis");
        this.plugin = plugin;
    }

    public void init(String address, String password) {
        String[] addressSplit = address.split(":");
        String host = addressSplit[0];
        int port = addressSplit.length > 1 ? Integer.parseInt(addressSplit[1]) : 6379;

        if (password.equals("")) {
            jedisPool = new JedisPool(new JedisPoolConfig(), host, port);
        } else {
            jedisPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
        }

        plugin.doAsync(() -> {
            sub = new LPSub(this);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(sub, CHANNEL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void close() {
        sub.unsubscribe();
        jedisPool.destroy();
    }

    @Override
    protected void sendMessage(String channel, String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(CHANNEL, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiredArgsConstructor
    private static class LPSub extends JedisPubSub {
        private final RedisMessaging parent;

        @Override
        public void onMessage(String channel, String msg) {
            parent.onMessage(channel, msg, null);
        }
    }

}
