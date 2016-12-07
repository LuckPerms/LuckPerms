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

import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.common.LuckPermsPlugin;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Uses Redis to push/receive changes to/from other servers
 */
@RequiredArgsConstructor
public class RedisMessaging implements MessagingService {
    private static final String CHANNEL = "luckperms";

    private final LuckPermsPlugin plugin;
    private JedisPool jedisPool;
    private LPSub sub;

    public void init(String address, String password) {
        String host = address.substring(0, address.indexOf(':'));
        int port = Integer.parseInt(address.substring(address.indexOf(":") + 1));

        if (password.equals("")) {
            jedisPool = new JedisPool(new JedisPoolConfig(), host, port);
        } else {
            jedisPool = new JedisPool(new JedisPoolConfig(), host, port, 0, password);
        }

        plugin.doAsync(() -> {
            sub = new LPSub(plugin);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.subscribe(sub, CHANNEL);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        sub.unsubscribe();
        jedisPool.destroy();
    }

    @Override
    public void pushUpdate() {
        plugin.doAsync(() -> {
            UUID id = sub.generateId();
            plugin.getLog().info("[Redis Messaging] Sending redis ping with id: " + id.toString());
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(CHANNEL, "update:" + id.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @RequiredArgsConstructor
    private static class LPSub extends JedisPubSub {
        private final LuckPermsPlugin plugin;
        private final Set<UUID> receivedMsgs = Collections.synchronizedSet(new HashSet<>());

        public UUID generateId() {
            UUID uuid = UUID.randomUUID();
            receivedMsgs.add(uuid);
            return uuid;
        }

        @Override
        public void onMessage(String channel, String msg) {
            if (!channel.equals(CHANNEL)) {
                return;
            }

            if (!msg.startsWith("update:")) {
                return;
            }

            String requestId = msg.substring("update:".length());
            UUID uuid;
            try {
                uuid = UUID.fromString(requestId);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }

            if (!receivedMsgs.add(uuid)) {
                return;
            }

            plugin.getLog().info("[Redis Messaging] Received update ping with id: " + uuid.toString());
            plugin.getUpdateTaskBuffer().request();
        }
    }

}
