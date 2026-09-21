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

package me.lucko.luckperms.waterdog.listeners;

import dev.waterdog.waterdogpe.event.EventManager;
import dev.waterdog.waterdogpe.event.EventPriority;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.waterdog.LPWaterdogPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.TimeUnit;

public class WaterdogConnectionListener extends AbstractConnectionListener {
    private final LPWaterdogPlugin plugin;

    public WaterdogConnectionListener(LPWaterdogPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register(EventManager eventManager) {
        eventManager.subscribe(PlayerLoginEvent.class, this::onPlayerLogin, EventPriority.LOWEST);
        eventManager.subscribe(PlayerLoginEvent.class, this::onPlayerPostLogin, EventPriority.LOW);
        eventManager.subscribe(PlayerDisconnectedEvent.class, this::onPlayerQuit, EventPriority.HIGHEST);
    }

    private void onPlayerLogin(PlayerLoginEvent e) {
        /* Called when the player first attempts a connection with the server.

           The PlayerLoginEvent is fired before the player is connected to a backend server, and is
           handled asynchronously, so we can load the player's data here and delay their connection
           until it is ready. This means a player will have the same UUID across the network, even if
           parts of the network are running in offline mode. */

        final ProxiedPlayer player = e.getPlayer();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + player.getUniqueId() + " - " + player.getName());
        }

        if (e.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + player.getUniqueId() + " - " + player.getName() + ". No permissions data will be loaded.");
            return;
        }

        /* Actually process the login for the connection.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(player.getUniqueId(), player.getName());
            recordConnection(player.getUniqueId());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUniqueId(), player.getName(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + player.getUniqueId() + " - " + player.getName(), ex);

            // there was some error loading
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // cancel the login attempt
                Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                e.setCancelReason(LegacyComponentSerializer.legacySection().serialize(reason));
                e.setCancelled(true);
            }
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(player.getUniqueId(), player.getName(), null);
        }
    }

    private void onPlayerPostLogin(PlayerLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final User user = this.plugin.getUserManager().getIfLoaded(player.getUniqueId());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUniqueId() + " - " + player.getName());
        }

        if (user != null) {
            return;
        }

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
            player.disconnect(LegacyComponentSerializer.legacySection().serialize(reason));
        } else {
            // just send a message
            this.plugin.getBootstrap().getScheduler().asyncLater(() -> {
                if (!player.isConnected()) {
                    return;
                }

                Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
            }, 1, TimeUnit.SECONDS);
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    private void onPlayerQuit(PlayerDisconnectedEvent e) {
        handleDisconnect(e.getPlayer().getUniqueId());
    }

}
