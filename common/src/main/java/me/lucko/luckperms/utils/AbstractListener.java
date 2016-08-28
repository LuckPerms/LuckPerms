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

package me.lucko.luckperms.utils;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.api.data.Callback;
import me.lucko.luckperms.api.event.events.UserFirstLoginEvent;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.users.User;

import java.util.UUID;

@AllArgsConstructor
public class AbstractListener {
    private final LuckPermsPlugin plugin;

    protected void onAsyncLogin(UUID u, String username) {
        final long startTime = System.currentTimeMillis();

        final UuidCache cache = plugin.getUuidCache();
        if (!cache.isOnlineMode()) {
            UUID uuid = plugin.getDatastore().getUUID(username);
            if (uuid != null) {
                cache.addToCache(u, uuid);
            } else {
                // No previous data for this player
                plugin.getApiProvider().fireEventAsync(new UserFirstLoginEvent(u, username));
                cache.addToCache(u, u);
                plugin.getDatastore().saveUUIDData(username, u, Callback.empty());
            }
        } else {
            UUID uuid = plugin.getDatastore().getUUID(username);
            if (uuid == null) {
                plugin.getApiProvider().fireEventAsync(new UserFirstLoginEvent(u, username));
            }

            // Online mode, no cache needed. This is just for name -> uuid lookup.
            plugin.getDatastore().saveUUIDData(username, u, Callback.empty());
        }

        plugin.getDatastore().loadUser(cache.getUUID(u), username);
        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            plugin.getLog().warn("Processing login for " + username + " took " + time + "ms.");
        }
    }

    protected void onLeave(UUID uuid) {
        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect;
        cache.clearCache(uuid);

        final User user = plugin.getUserManager().get(cache.getUUID(uuid));
        plugin.getUserManager().unload(user);
    }

    protected void refreshPlayer(UUID uuid) {
        final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(uuid));
        if (user != null) {
            user.refreshPermissions();
        }
    }
}
