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

package me.lucko.luckperms.nemisys.listeners;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.nemisys.LPNemisysPlugin;
import me.lucko.luckperms.nemisys.model.permissible.LPPermissible;
import me.lucko.luckperms.nemisys.model.permissible.PermissibleInjector;

import org.itxtech.nemisys.Player;
import org.itxtech.nemisys.event.EventHandler;
import org.itxtech.nemisys.event.EventPriority;
import org.itxtech.nemisys.event.Listener;
import org.itxtech.nemisys.event.player.PlayerAsyncPreLoginEvent;
import org.itxtech.nemisys.event.player.PlayerLoginEvent;
import org.itxtech.nemisys.event.player.PlayerLogoutEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NemisysConnectionListener extends AbstractConnectionListener implements Listener {
    private final LPNemisysPlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public NemisysConnectionListener(LPNemisysPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(PlayerAsyncPreLoginEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins) */

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + e.getUuid() + " - " + e.getName());
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(e.getUuid(), e.getName());
            this.plugin.getEventFactory().handleUserLoginProcess(e.getUuid(), e.getName(), user);
            recordConnection(e.getUuid());
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + e.getUuid() + " - " + e.getName());
            ex.printStackTrace();

            // deny the connection
            this.deniedAsyncLogin.add(e.getUuid());
            e.disAllow(Message.LOADING_DATABASE_ERROR.asString(this.plugin.getLocaleManager()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreLoginMonitor(PlayerAsyncPreLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (this.deniedAsyncLogin.remove(e.getUuid())) {
            // their data was never loaded at LOW priority, now check to see if they have been magically allowed since then.

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.getLoginResult() == PlayerAsyncPreLoginEvent.LoginResult.SUCCESS) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.getUuid());
                e.disAllow("");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded. */

        final Player player = e.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUuid() + " - " + player.getName());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            this.deniedLogin.add(player.getUuid());

            if (!getUniqueConnections().contains(player.getUuid())) {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getName() +
                        " doesn't have data pre-loaded, they have never need processed during pre-login in this session." +
                        " - denying login.");
            } else {
                this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                        " - denying login.");
            }

            e.setCancelled();
            e.setKickMessage(Message.LOADING_STATE_ERROR.asString(this.plugin.getLocaleManager()));
            return;
        }

        // User instance is there, now we can inject our custom Permissible into the player.
        // Care should be taken at this stage to ensure that async tasks which manipulate nukkit data check that the player is still online.
        try {
            // Make a new permissible for the user
            LPPermissible lpPermissible = new LPPermissible(player, user, this.plugin);

            // Inject into the player
            PermissibleInjector.inject(player, lpPermissible);

        } catch (Throwable t) {
            this.plugin.getLogger().warn("Exception thrown when setting up permissions for " +
                    player.getUuid() + " - " + player.getName() + " - denying login.");
            t.printStackTrace();

            e.setCancelled();
            e.setKickMessage(Message.LOADING_SETUP_ERROR.asString(this.plugin.getLocaleManager()));
            return;
        }

        this.plugin.refreshAutoOp(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the login
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW. Even if it was denied at LOW, their data will still be present.
        if (this.deniedLogin.remove(e.getPlayer().getUuid())) {
            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (!e.isCancelled()) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.getPlayer().getUuid());
                e.setCancelled();
            }
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerLogoutEvent e) {
        final Player player = e.getPlayer();

        // Remove the custom permissible
        try {
            PermissibleInjector.unInject(player, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Handle auto op
        if (this.plugin.getConfiguration().get(ConfigKeys.AUTO_OP)) {
            player.setOp(false);
        }

        // Register with the housekeeper, so the User's instance will stick
        // around for a bit after they disconnect
        this.plugin.getUserManager().getHouseKeeper().registerUsage(player.getUuid());

        // force a clear of transient nodes
        this.plugin.getBootstrap().getScheduler().executeAsync(() -> {
            User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());
            if (user != null) {
                user.clearTransientNodes();
            }
        });

        // remove their contexts cache
        this.plugin.getContextManager().onPlayerQuit(player);
    }

}
