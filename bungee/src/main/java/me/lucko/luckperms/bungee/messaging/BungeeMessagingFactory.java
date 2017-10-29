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

package me.lucko.luckperms.bungee.messaging;

import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.messaging.ExtendedMessagingService;
import me.lucko.luckperms.common.messaging.MessagingFactory;

public class BungeeMessagingFactory extends MessagingFactory<LPBungeePlugin> {
    public BungeeMessagingFactory(LPBungeePlugin plugin) {
        super(plugin);
    }

    @Override
    protected ExtendedMessagingService getServiceFor(String messagingType) {
        if (messagingType.equals("bungee")) {
            BungeeMessagingService bungeeMessaging = new BungeeMessagingService(getPlugin());
            bungeeMessaging.init();
            return bungeeMessaging;
        } else if (messagingType.equals("redisbungee")) {
            if (getPlugin().getProxy().getPluginManager().getPlugin("RedisBungee") == null) {
                getPlugin().getLog().warn("RedisBungee plugin not present.");
            } else {
                RedisBungeeMessagingService redisBungeeMessaging = new RedisBungeeMessagingService(getPlugin());
                redisBungeeMessaging.init();
                return redisBungeeMessaging;
            }
        }

        return super.getServiceFor(messagingType);
    }
}
