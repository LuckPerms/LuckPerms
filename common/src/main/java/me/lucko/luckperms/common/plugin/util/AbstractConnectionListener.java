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

package me.lucko.luckperms.common.plugin.util;

import me.lucko.luckperms.api.PlayerSaveResult;
import me.lucko.luckperms.api.platform.PlatformType;
import me.lucko.luckperms.common.assignments.AssignmentRule;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.PlayerSaveResultImpl;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract listener utility for handling new player connections
 */
public abstract class AbstractConnectionListener {
    private final LuckPermsPlugin plugin;
    private final Set<UUID> uniqueConnections = ConcurrentHashMap.newKeySet();

    protected AbstractConnectionListener(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the unique players which have connected to the server since it started.
     *
     * @return the unique connections
     */
    public Set<UUID> getUniqueConnections() {
        return this.uniqueConnections;
    }

    protected void recordConnection(UUID uuid) {
        this.uniqueConnections.add(uuid);
    }

    public User loadUser(UUID uuid, String username) {
        final long startTime = System.currentTimeMillis();

        // register with the housekeeper to avoid accidental unloads
        this.plugin.getUserManager().getHouseKeeper().registerUsage(uuid);

        // save uuid data.
        PlayerSaveResult saveResult = this.plugin.getStorage().savePlayerData(uuid, username).join();

        // fire UserFirstLogin event
        if (saveResult.includes(PlayerSaveResultImpl.Status.CLEAN_INSERT)) {
            this.plugin.getEventFactory().handleUserFirstLogin(uuid, username);
        }

        // most likely because ip forwarding is not setup correctly
        // print a warning to the console
        if (saveResult.includes(PlayerSaveResult.Status.OTHER_UUIDS_PRESENT_FOR_USERNAME)) {
            Set<UUID> otherUuids = saveResult.getOtherUuids();

            this.plugin.getLogger().warn("LuckPerms already has data for player '" + username + "' - but this data is stored under a different UUID.");
            this.plugin.getLogger().warn("'" + username + "' has previously used the unique ids " + otherUuids + " but is now connecting with '" + uuid + "'");

            if (uuid.version() == 4) {
                if (this.plugin.getBootstrap().getType() == PlatformType.BUNGEE) {
                    this.plugin.getLogger().warn("The UUID the player is connecting with now is Mojang-assigned (type 4). This implies that BungeeCord's IP-Forwarding has not been setup correctly on one (or more) of the backend servers.");
                } else {
                    this.plugin.getLogger().warn("The UUID the player is connecting with now is Mojang-assigned (type 4). This implies that one of the other servers in your network is not authenticating correctly.");
                    this.plugin.getLogger().warn("If you're using BungeeCord, please ensure that IP-Forwarding is setup correctly on all of your backend servers!");
                }
            } else {
                this.plugin.getLogger().warn("The UUID the player is connecting with now is NOT Mojang-assigned (type " + uuid.version() + "). This implies that THIS server is not authenticating correctly, but one (or more) of the other servers/proxies in the network are.");
                this.plugin.getLogger().warn("If you're using BungeeCord, please ensure that IP-Forwarding is setup correctly on all of your backend servers!");
            }

            this.plugin.getLogger().warn("See here for more info: https://github.com/lucko/LuckPerms/wiki/Network-Installation#pre-setup");
        }

        User user = this.plugin.getStorage().loadUser(uuid, username).join();
        if (user == null) {
            throw new NullPointerException("User is null");
        }

        // Setup defaults for the user
        boolean save = false;
        for (AssignmentRule rule : this.plugin.getConfiguration().get(ConfigKeys.DEFAULT_ASSIGNMENTS)) {
            if (rule.apply(user)) {
                save = true;
            }
        }

        // If they were given a default, persist the new assignments back to the storage.
        if (save) {
            this.plugin.getStorage().saveUser(user).join();
        }

        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            this.plugin.getLogger().warn("Processing login for " + username + " took " + time + "ms.");
        }

        return user;
    }

}
