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

package me.lucko.luckperms.common.managers.user;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.utils.ExpiringSet;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The instance responsible for unloading users which are no longer needed.
 */
public class UserHousekeeper implements Runnable {
    private final LuckPermsPlugin plugin;
    private final UserManager<?> userManager;

    // contains the uuids of users who have recently logged in / out
    private final ExpiringSet<UUID> recentlyUsed;

    // contains the uuids of users who have recently been retrieved from the API
    private final ExpiringSet<UUID> recentlyUsedApi;

    public UserHousekeeper(LuckPermsPlugin plugin, UserManager<?> userManager, TimeoutSettings timeoutSettings) {
        this.plugin = plugin;
        this.userManager = userManager;
        this.recentlyUsed = new ExpiringSet<>(timeoutSettings.duration, timeoutSettings.unit);
        this.recentlyUsedApi = new ExpiringSet<>(5, TimeUnit.MINUTES);
    }

    // called when a player attempts a connection or logs out
    public void registerUsage(UUID uuid) {
        this.recentlyUsed.add(uuid);
    }

    public void registerApiUsage(UUID uuid) {
        this.recentlyUsedApi.add(uuid);
    }

    public void clearApiUsage(UUID uuid) {
        this.recentlyUsedApi.remove(uuid);
    }

    @Override
    public void run() {
        for (UserIdentifier entry : this.userManager.getAll().keySet()) {
            cleanup(entry);
        }
    }

    public void cleanup(UserIdentifier identifier) {
        UUID uuid = identifier.getUuid();

        // unload users which aren't online and who haven't been online (or tried to login) recently
        if (this.recentlyUsed.contains(uuid) || this.recentlyUsedApi.contains(uuid) || this.plugin.isPlayerOnline(uuid)) {
            return;
        }

        // unload them
        this.userManager.unload(identifier);
    }

    public static TimeoutSettings timeoutSettings(long duration, TimeUnit unit) {
        return new TimeoutSettings(duration, unit);
    }

    public static final class TimeoutSettings {
        private final long duration;
        private final TimeUnit unit;

        private TimeoutSettings(long duration, TimeUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }
    }
}
