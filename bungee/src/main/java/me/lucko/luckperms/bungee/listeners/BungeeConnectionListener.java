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

package me.lucko.luckperms.bungee.listeners;

import me.lucko.luckperms.bungee.LPBungeePlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.concurrent.TimeUnit;

public class BungeeConnectionListener extends AbstractConnectionListener implements Listener {
    private final LPBungeePlugin plugin;

    public BungeeConnectionListener(LPBungeePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(LoginEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins)

           Delay the login here, as we want to cache UUID data before the player is connected to a backend bukkit server.
           This means that a player will have the same UUID across the network, even if parts of the network are running in
           Offline mode. */

        final PendingConnection c = e.getConnection();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + c.getUniqueId() + " - " + c.getName());
        }

        if (e.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + c.getUniqueId() + " - " + c.getName() + ". No permissions data will be loaded.");
            return;
        }

        /* registers the plugins intent to modify this events state going forward.
           this will prevent the event from completing until we're finished handling. */
        e.registerIntent(this.plugin.getLoader());

        this.plugin.getBootstrap().getScheduler().async(() -> {
            /* Actually process the login for the connection.
               We do this here to delay the login until the data is ready.
               If the login gets cancelled later on, then this will be cleaned up.

               This includes:
               - loading uuid data
               - loading permissions
               - creating a user instance in the UserManager for this connection.
               - setting up cached data. */
            try {
                User user = loadUser(c.getUniqueId(), c.getName());
                recordConnection(c.getUniqueId());
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(c.getUniqueId(), c.getName(), user);
            } catch (Exception ex) {
                this.plugin.getLogger().severe("Exception occurred whilst loading data for " + c.getUniqueId() + " - " + c.getName(), ex);

                // there was some error loading
                if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                    // cancel the login attempt
                    Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                    e.setCancelReason(BungeeComponentSerializer.get().serialize(reason));
                    e.setCancelled(true);
                }
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(c.getUniqueId(), c.getName(), null);
            }

            // finally, complete our intent to modify state, so the proxy can continue handling the connection.
            e.completeIntent(this.plugin.getLoader());
        });
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final User user = this.plugin.getUserManager().getIfLoaded(e.getPlayer().getUniqueId());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUniqueId() + " - " + player.getName());
        }

        if (user == null) {
            if (!getUniqueConnections().contains(player.getUniqueId())) {
                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session.");
            } else {
                this.plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session.");
            }

            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // disconnect the user
                Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                e.getPlayer().disconnect(BungeeComponentSerializer.get().serialize(reason));
            } else {
                // just send a message
                this.plugin.getBootstrap().getProxy().getScheduler().schedule(this.plugin.getLoader(), () -> {
                    if (!player.isConnected()) {
                        return;
                    }

                    Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
                }, 1, TimeUnit.SECONDS);
            }
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        handleDisconnect(e.getPlayer().getUniqueId());
    }

}
