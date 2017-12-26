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

package me.lucko.luckperms.bukkit.listeners;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.model.LPPermissible;
import me.lucko.luckperms.bukkit.model.PermissibleInjector;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.utils.LoginHelper;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BukkitConnectionListener implements Listener {
    private final LPBukkitPlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins) */

        /* wait for the plugin to enable. because these events are fired async, they can be called before
           the plugin has enabled.  */
        try {
            plugin.getEnableLatch().await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        if (plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.getLog().info("Processing pre-login for " + e.getUniqueId() + " - " + e.getName());
        }

        plugin.getUniqueConnections().add(e.getUniqueId());

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = LoginHelper.loadUser(plugin, e.getUniqueId(), e.getName(), false);
            plugin.getApiProvider().getEventFactory().handleUserLoginProcess(e.getUniqueId(), e.getName(), user);
        } catch (Exception ex) {
            plugin.getLog().severe("Exception occured whilst loading data for " + e.getUniqueId() + " - " + e.getName());
            ex.printStackTrace();

            // deny the connection
            deniedAsyncLogin.add(e.getUniqueId());
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.asString(plugin.getLocaleManager()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreLoginMonitor(AsyncPlayerPreLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (deniedAsyncLogin.remove(e.getUniqueId())) {
            // their data was never loaded at LOW priority, now check to see if they have been magically allowed since then.

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                plugin.getLog().severe("Player connection was re-allowed for " + e.getUniqueId());
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "");
            }

            return;
        }

        // Login event was cancelled by another plugin, but it wasn't cancelled when we handled it at LOW
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // Schedule cleanup of this user.
            plugin.getUserManager().scheduleUnload(e.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded. */

        final Player player = e.getPlayer();

        if (plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.getLog().info("Processing login for " + player.getUniqueId() + " - " + player.getName());
        }

        final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            deniedLogin.add(e.getPlayer().getUniqueId());

            plugin.getLog().warn("User " + player.getUniqueId() + " - " + player.getName() + " doesn't have data pre-loaded. - denying login.");
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.asString(plugin.getLocaleManager()));
            return;
        }

        // User instance is there, now we can inject our custom Permissible into the player.
        // Care should be taken at this stage to ensure that async tasks which manipulate bukkit data check that the player is still online.
        try {
            // Make a new permissible for the user
            LPPermissible lpPermissible = new LPPermissible(player, user, plugin);

            // Inject into the player
            PermissibleInjector.inject(player, lpPermissible);

        } catch (Throwable t) {
            t.printStackTrace();
        }

        plugin.refreshAutoOp(user, player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the login
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW. Even if it was denied at LOW, their data will still be present.
        boolean denied = false;
        if (deniedLogin.remove(e.getPlayer().getUniqueId())) {
            denied = true;

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.getResult() == PlayerLoginEvent.Result.ALLOWED) {
                plugin.getLog().severe("Player connection was re-allowed for " + e.getPlayer().getUniqueId());
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "");
            }
        }

        // Login event was cancelled by another plugin since we first loaded their data
        if (denied || e.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            // Schedule cleanup of this user.
            plugin.getUserManager().scheduleUnload(e.getPlayer().getUniqueId());
            return;
        }

        // everything is going well. login was processed ok, this is just to refresh auto-op status.
        plugin.refreshAutoOp(plugin.getUserManager().getIfLoaded(e.getPlayer().getUniqueId()), e.getPlayer());
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        final Player player = e.getPlayer();

        // Remove the custom permissible
        try {
            PermissibleInjector.unInject(player, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Handle auto op
        if (plugin.getConfiguration().get(ConfigKeys.AUTO_OP)) {
            player.setOp(false);
        }

        // Request that the users data is unloaded.
        plugin.getUserManager().scheduleUnload(player.getUniqueId());
    }

}
