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
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.waterdog.LPWaterdogPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class WaterdogConnectionListener extends AbstractConnectionListener {
    private final LPWaterdogPlugin plugin;

    public WaterdogConnectionListener(LPWaterdogPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    public void register(EventManager eventManager) {
        eventManager.subscribe(
                PlayerDisconnectEvent.class,
                this::onPlayerQuit,
                EventPriority.LOW
        );
        eventManager.subscribe(
                PlayerLoginEvent.class,
                this::onPlayerLogin,
                EventPriority.LOWEST
        );
        eventManager.subscribe(
                PlayerLoginEvent.class,
                this::onPlayerPostLogin,
                EventPriority.LOW
        );
    }
    
    private void onPlayerLogin(PlayerLoginEvent e) {
        final UUID uniqueId = e.getPlayer().getUniqueId();
        final String name = e.getPlayer().getName();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + uniqueId + " - " + name);
        }

        if (e.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + uniqueId + " - " + name + ". No permissions data will be loaded.");
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
            User user = loadUser(uniqueId, name);
            recordConnection(uniqueId);

            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, name, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + name, ex);

            // there was some error loading
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                // cancel the login attempt
                Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                e.setCancelReason(LegacyComponentSerializer.legacySection().serialize(reason));
                e.setCancelled(true);
            }

            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, name, null);
        }
    }

    private void onPlayerPostLogin(PlayerLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final User user = this.plugin.getUserManager().getIfLoaded(e.getPlayer().getUniqueId());

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUniqueId() + " - " + player.getName());
        }

        if (user != null) {
            this.plugin.getLogger().info("User successfully logined " + player.getUniqueId() + " - " + player.getName());
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
            e.getPlayer().disconnect(LegacyComponentSerializer.legacySection().serialize(reason));

            return;
        }

        // just send a message
        this.plugin.getBootstrap().getScheduler().sync().execute(() -> {
            if (!player.isConnected()) {
                return;
            }

            Message.LOADING_STATE_ERROR.send(this.plugin.getSenderFactory().wrap(player));
        });
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    private void onPlayerQuit(PlayerDisconnectEvent e) {
        handleDisconnect(e.getPlayer().getUniqueId());
    }

}
