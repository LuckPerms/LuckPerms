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

package me.lucko.luckperms.common.utils;

import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.common.assignments.AssignmentRule;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract listener utility for handling new player connections
 */
public abstract class AbstractLoginListener {
    private final LuckPermsPlugin plugin;

    // if we should #join the uuid save future.
    // this is only really necessary on BungeeCord, as the data may be needed
    // on the backend, depending on uuid config options
    private final boolean joinUuidSave;

    protected AbstractLoginListener(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.joinUuidSave = plugin.getServerType() == PlatformType.BUNGEE;
    }

    public User loadUser(UUID u, String username) {
        final long startTime = System.currentTimeMillis();

        // register with the housekeeper to avoid accidental unloads
        this.plugin.getUserManager().getHouseKeeper().registerUsage(u);

        final UuidCache cache = this.plugin.getUuidCache();
        if (!this.plugin.getConfiguration().get(ConfigKeys.USE_SERVER_UUIDS)) {
            UUID uuid = this.plugin.getStorage().noBuffer().getUUID(username).join();
            if (uuid != null) {
                cache.addToCache(u, uuid);
            } else {
                // No previous data for this player
                this.plugin.getEventFactory().handleUserFirstLogin(u, username);
                cache.addToCache(u, u);
                CompletableFuture<Void> future = this.plugin.getStorage().noBuffer().saveUUIDData(u, username);
                if (this.joinUuidSave) {
                    future.join();
                }
            }
        } else {
            String name = this.plugin.getStorage().noBuffer().getName(u).join();
            if (name == null) {
                this.plugin.getEventFactory().handleUserFirstLogin(u, username);
            }

            // Online mode, no cache needed. This is just for name -> uuid lookup.
            CompletableFuture<Void> future = this.plugin.getStorage().noBuffer().saveUUIDData(u, username);
            if (this.joinUuidSave) {
                future.join();
            }
        }

        User user = this.plugin.getStorage().noBuffer().loadUser(cache.getUUID(u), username).join();
        if (user == null) {
            throw new NullPointerException("User is null");
        } else {
            // Setup defaults for the user
            boolean save = false;
            for (AssignmentRule rule : this.plugin.getConfiguration().get(ConfigKeys.DEFAULT_ASSIGNMENTS)) {
                if (rule.apply(user)) {
                    save = true;
                }
            }

            // If they were given a default, persist the new assignments back to the storage.
            if (save) {
                this.plugin.getStorage().noBuffer().saveUser(user).join();
            }

            // Does some minimum pre-calculations to (maybe) speed things up later.
            user.preCalculateData();
        }

        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            this.plugin.getLog().warn("Processing login for " + username + " took " + time + "ms.");
        }

        return user;
    }

}
