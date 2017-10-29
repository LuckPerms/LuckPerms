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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

@RequiredArgsConstructor
public class MessagingFactory<P extends LuckPermsPlugin> {

    @Getter(AccessLevel.PROTECTED)
    private final P plugin;

    public final ExtendedMessagingService getInstance() {
        String messagingType = plugin.getConfiguration().get(ConfigKeys.MESSAGING_SERVICE).toLowerCase();
        if (messagingType.equals("none") && plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
            messagingType = "redis";
        }

        if (messagingType.equals("none")) {
            return null;
        }

        plugin.getLog().info("Loading messaging service... [" + messagingType.toUpperCase() + "]");

        ExtendedMessagingService service = getServiceFor(messagingType);
        if (service != null) {
            return service;
        }

        plugin.getLog().warn("Messaging service '" + messagingType + "' not recognised.");
        return null;
    }

    protected ExtendedMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("redis")) {
            if (plugin.getConfiguration().get(ConfigKeys.REDIS_ENABLED)) {
                RedisMessagingService redis = new RedisMessagingService(plugin);
                try {
                    redis.init(plugin.getConfiguration().get(ConfigKeys.REDIS_ADDRESS), plugin.getConfiguration().get(ConfigKeys.REDIS_PASSWORD));
                    return redis;
                } catch (Exception e) {
                    plugin.getLog().warn("Couldn't load redis...");
                    e.printStackTrace();
                }
            } else {
                plugin.getLog().warn("Messaging Service was set to redis, but redis is not enabled!");
            }
        }

        return null;
    }

}
