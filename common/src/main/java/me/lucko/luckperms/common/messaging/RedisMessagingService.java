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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import redis.clients.jedis.shaded.Jedis;
import redis.clients.jedis.shaded.JedisPool;
import redis.clients.jedis.shaded.JedisPoolConfig;
import redis.clients.jedis.shaded.JedisPubSub;

/**
 * An implementation of {@link me.lucko.luckperms.api.MessagingService} using Redis.
 */
public class RedisMessagingService extends AbstractMessagingService {
    private final LuckPermsPlugin plugin;
    private JedisPool jedisPool;
    private LPSub sub;

    public RedisMessagingService(LuckPermsPlugin plugin) {
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

        plugin.getScheduler().doAsync(() -> {
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
    protected void sendMessage(String message) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(CHANNEL, message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiredArgsConstructor
    private static class LPSub extends JedisPubSub {
        private final RedisMessagingService parent;

        @Override
        public void onMessage(String channel, String msg) {
            if (!channel.equals(CHANNEL)) {
                return;
            }
            parent.onMessage(msg, null);
        }
    }

}
