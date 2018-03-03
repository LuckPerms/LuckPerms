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

package me.lucko.luckperms.common.listener;

import me.lucko.luckperms.common.assignments.AssignmentRule;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract listener utility for handling new player connections
 */
public abstract class AbstractConnectionListener implements ConnectionListener {
    private final LuckPermsPlugin plugin;
    private final Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    protected AbstractConnectionListener(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Set<UUID> getUniqueConnections() {
        return this.uniqueConnections;
    }

    protected void recordConnection(UUID uuid) {
        this.uniqueConnections.add(uuid);
    }

    public User loadUser(UUID u, String username) {
        final long startTime = System.currentTimeMillis();

        // register with the housekeeper to avoid accidental unloads
        this.plugin.getUserManager().getHouseKeeper().registerUsage(u);

        // save uuid data.
        String name = this.plugin.getStorage().noBuffer().getName(u).join();
        if (name == null) {
            this.plugin.getEventFactory().handleUserFirstLogin(u, username);
        }
        this.plugin.getStorage().noBuffer().saveUUIDData(u, username);

        User user = this.plugin.getStorage().noBuffer().loadUser(u, username).join();
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
            this.plugin.getLogger().warn("Processing login for " + username + " took " + time + "ms.");
        }

        return user;
    }

}
