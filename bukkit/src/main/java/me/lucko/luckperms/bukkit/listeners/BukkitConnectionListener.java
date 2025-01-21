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

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.inject.permissible.LuckPermsPermissible;
import me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector;
import me.lucko.luckperms.bukkit.util.PlayerLocaleUtil;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class BukkitConnectionListener extends AbstractConnectionListener implements Listener {
    private static final Predicate<? super String> IS_CRAFTBUKKIT_PREDICATE = Pattern.compile("^(?:git|\\d+)-Bukkit-[0-9a-f]{7}(?: .*)?$").asPredicate();

    private final LPBukkitPlugin plugin;

    private final boolean detectedCraftBukkitOfflineMode;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public BukkitConnectionListener(LPBukkitPlugin plugin) {
        super(plugin);
        this.plugin = plugin;

        // check for craftbukkit + offline mode combination
        String version = plugin.getBootstrap().getServer().getVersion();
        boolean onlineMode = plugin.getBootstrap().getServer().getOnlineMode();

        if (!onlineMode && IS_CRAFTBUKKIT_PREDICATE.test(version)) {
            printCraftBukkitOfflineModeError();
            this.detectedCraftBukkitOfflineMode = true;
        } else {
            this.detectedCraftBukkitOfflineMode = false;
        }
    }

    private void printCraftBukkitOfflineModeError() {
        this.plugin.getLogger().warn("It appears that your server is running CraftBukkit and configured in offline (cracked) mode.");
        this.plugin.getLogger().warn("Due to a CraftBukkit limitation, LuckPerms cannot function correctly in this setup.");
        this.plugin.getLogger().warn("To resolve this, please either a) upgrade from CraftBukkit to Spigot or Paper, or b) enable online-mode.");
        this.plugin.getLogger().warn("For more info, please see: https://luckperms.net/wiki/Installation#craftbukkit-and-offline-mode");
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins)
           Also, give other plugins a chance to cancel the event. */

        /* wait for the plugin to enable. because these events are fired async, they can be called before
           the plugin has enabled.  */
        try {
            this.plugin.getBootstrap().getEnableLatch().await(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + e.getUniqueId() + " - " + e.getName());
        }

        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + e.getUniqueId() + " - " + e.getName() + ". No permissions data will be loaded.");
            this.deniedAsyncLogin.add(e.getUniqueId());
            return;
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
            User user = loadUser(e.getUniqueId(), e.getName());
            recordConnection(e.getUniqueId());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(e.getUniqueId(), e.getName(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + e.getUniqueId() + " - " + e.getName(), ex);

            // deny the connection
            this.deniedAsyncLogin.add(e.getUniqueId());

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, LegacyComponentSerializer.legacySection().serialize(reason));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(e.getUniqueId(), e.getName(), null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreLoginMonitor(AsyncPlayerPreLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (this.deniedAsyncLogin.remove(e.getUniqueId())) {
            // their data was never loaded at LOW priority, now check to see if they have been magically allowed since then.

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.getUniqueId());
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded. */

        final Player player = e.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login for " + player.getUniqueId() + " - " + player.getName());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUniqueId());

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            this.deniedLogin.add(player.getUniqueId());

            if (!getUniqueConnections().contains(player.getUniqueId())) {

                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session." +
                        " - denying login.");

                if (this.detectedCraftBukkitOfflineMode) {
                    printCraftBukkitOfflineModeError();

                    Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR_CB_OFFLINE_MODE.build(), PlayerLocaleUtil.getLocale(player));
                    e.disallow(PlayerLoginEvent.Result.KICK_OTHER, LegacyComponentSerializer.legacySection().serialize(reason));
                    return;
                }

            } else {
                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                        " - denying login.");
            }

            Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), PlayerLocaleUtil.getLocale(player));
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, LegacyComponentSerializer.legacySection().serialize(reason));
            return;
        }

        // User instance is there, now we can inject our custom Permissible into the player.
        // Care should be taken at this stage to ensure that async tasks which manipulate bukkit data check that the player is still online.
        try {
            // Make a new permissible for the user
            LuckPermsPermissible lpPermissible = new LuckPermsPermissible(player, user, this.plugin);

            // Inject into the player
            PermissibleInjector.inject(player, lpPermissible, this.plugin.getLogger());

        } catch (Throwable t) {
            this.plugin.getLogger().warn("Exception thrown when setting up permissions for " +
                    player.getUniqueId() + " - " + player.getName() + " - denying login.", t);

            Component reason = TranslationManager.render(Message.LOADING_SETUP_ERROR.build(), PlayerLocaleUtil.getLocale(player));
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, LegacyComponentSerializer.legacySection().serialize(reason));
            return;
        }

        this.plugin.getContextManager().signalContextUpdate(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLoginMonitor(PlayerLoginEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the login
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW. Even if it was denied at LOW, their data will still be present.
        if (this.deniedLogin.remove(e.getPlayer().getUniqueId())) {
            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.getResult() == PlayerLoginEvent.Result.ALLOWED) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.getPlayer().getUniqueId());
                e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "");
            }
        }

        PermissibleInjector.checkInjected(e.getPlayer(), this.plugin.getLogger());
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        handleDisconnect(player.getUniqueId());

        // perform unhooking from bukkit objects 1 tick later.
        // this allows plugins listening after us on MONITOR to still have intact permissions data
        this.plugin.getBootstrap().getScheduler().sync(() -> {
            // Remove the custom permissible
            try {
                PermissibleInjector.uninject(player, true);
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception thrown when unloading permissions from " +
                        player.getUniqueId() + " - " + player.getName(), ex);
            }

            // Handle auto op
            if (this.plugin.getConfiguration().get(ConfigKeys.AUTO_OP)) {
                player.setOp(false);
            }

            // remove their contexts cache
            this.plugin.getContextManager().onPlayerQuit(player);
        });
    }

}
