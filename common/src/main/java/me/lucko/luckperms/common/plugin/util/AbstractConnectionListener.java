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

import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.platform.Platform;

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

    protected void recordConnection(UUID uniqueId) {
        this.uniqueConnections.add(uniqueId);
    }

    public User loadUser(UUID uniqueId, String username) {
        final long startTime = System.currentTimeMillis();

        // register with the housekeeper to avoid accidental unloads
        this.plugin.getUserManager().getHouseKeeper().registerUsage(uniqueId);

        // save uuid data.
        PlayerSaveResult saveResult = this.plugin.getStorage().savePlayerData(uniqueId, username).join();

        // fire UserFirstLogin event
        if (saveResult.includes(PlayerSaveResult.Outcome.CLEAN_INSERT)) {
            this.plugin.getEventDispatcher().dispatchUserFirstLogin(uniqueId, username);
        }

        // most likely because ip forwarding is not setup correctly
        // print a warning to the console
        if (saveResult.includes(PlayerSaveResult.Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME)) {
            Set<UUID> otherUuids = saveResult.getOtherUniqueIds();

            this.plugin.getLogger().warn("LuckPerms already has data for player '" + username + "' - but this data is stored under a different UUID.");
            this.plugin.getLogger().warn("'" + username + "' has previously used the unique ids " + otherUuids + " but is now connecting with '" + uniqueId + "'");

            if (uniqueId.version() == 4) {
                if (this.plugin.getBootstrap().getType() == Platform.Type.BUNGEECORD) {
                    this.plugin.getLogger().warn("The UUID the player is connecting with now is Mojang-assigned (type 4). This implies that BungeeCord's IP-Forwarding has not been setup correctly on one (or more) of the backend servers.");
                } if (this.plugin.getBootstrap().getType() == Platform.Type.VELOCITY) {
                    this.plugin.getLogger().warn("The UUID the player is connecting with now is Mojang-assigned (type 4). This implies that Velocity's IP-Forwarding has not been setup correctly on one (or more) of the backend servers.");
                } else {
                    this.plugin.getLogger().warn("The UUID the player is connecting with now is Mojang-assigned (type 4). This implies that one of the other servers in your network is not authenticating correctly.");
                    this.plugin.getLogger().warn("If you're using BungeeCord/Velocity, please ensure that IP-Forwarding is setup correctly on all of your backend servers!");
                }
            } else {
                this.plugin.getLogger().warn("The UUID the player is connecting with now is NOT Mojang-assigned (type " + uniqueId.version() + "). This implies that THIS server is not authenticating correctly, but one (or more) of the other servers/proxies in the network are.");
                this.plugin.getLogger().warn("If you're using BungeeCord/Velocity, please ensure that IP-Forwarding is setup correctly on all of your backend servers!");
            }

            this.plugin.getLogger().warn("See here for more info: https://luckperms.net/wiki/Network-Installation#pre-setup");
        }

        User user = this.plugin.getStorage().loadUser(uniqueId, username).join();
        if (user == null) {
            throw new NullPointerException("User is null");
        }

        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            this.plugin.getLogger().warn("Processing login for " + username + " took " + time + "ms.");
        }

        return user;
    }

    public void handleDisconnect(UUID uniqueId) {
        // Register with the housekeeper, so the User's instance will stick
        // around for a bit after they disconnect
        this.plugin.getUserManager().getHouseKeeper().registerUsage(uniqueId);

        // force a clear of transient nodes
        this.plugin.getBootstrap().getScheduler().async(() -> {
            User user = this.plugin.getUserManager().getIfLoaded(uniqueId);
            if (user != null) {
                user.clearNodes(DataType.TRANSIENT, null, false);
            }
        });
    }

}
