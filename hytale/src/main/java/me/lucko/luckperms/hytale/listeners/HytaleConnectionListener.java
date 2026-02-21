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

package me.lucko.luckperms.hytale.listeners;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HytaleConnectionListener extends AbstractConnectionListener {
    private final LPHytalePlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());

    public HytaleConnectionListener(LPHytalePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    public void register(EventRegistry registry) {
        registry.register(EventPriority.EARLY, PlayerSetupConnectEvent.class, this::onPlayerPreLogin);
        registry.register(EventPriority.LAST, PlayerSetupConnectEvent.class, this::onPlayerPreLoginMonitor);
        registry.register(EventPriority.NORMAL, PlayerConnectEvent.class, this::onPlayerPostLogin);
        registry.register(EventPriority.LAST, PlayerDisconnectEvent.class, this::onPlayerQuit);
    }

    private void onPlayerPreLogin(PlayerSetupConnectEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins)
           Also, give other plugins a chance to cancel the event. */

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login for " + e.getUuid() + " - " + e.getUsername());
        }

        if (e.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + e.getUuid() + " - " + e.getUsername() + ". No permissions data will be loaded.");
            this.deniedAsyncLogin.add(e.getUuid());
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
            User user = loadUser(e.getUuid(), e.getUsername());
            recordConnection(e.getUuid());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(e.getUuid(), e.getUsername(), user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + e.getUuid() + " - " + e.getUsername(), ex);

            // deny the connection
            this.deniedAsyncLogin.add(e.getUuid());

            Component reason = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
            e.setCancelled(true);
            e.setReason(PlainTextComponentSerializer.plainText().serialize(reason));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(e.getUuid(), e.getUsername(), null);
        }
    }

    private void onPlayerPreLoginMonitor(PlayerSetupConnectEvent e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (this.deniedAsyncLogin.remove(e.getUuid())) {
            // their data was never loaded at LOW priority, now check to see if they have been magically allowed since then.

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (!e.isCancelled()) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.getUuid());
                e.setCancelled(true);
            }
        }
    }

    private void onPlayerPostLogin(PlayerConnectEvent e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded. */

        PlayerRef player = e.getPlayerRef();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing post-login for " + player.getUuid() + " - " + player.getUsername());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(player.getUuid());
        if (user != null) {
            return;
        }

        if (!getUniqueConnections().contains(player.getUuid())) {
            this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() +
                    " doesn't have data pre-loaded, they have never been processed during pre-login in this session." +
                    " - denying login.");
        } else {
            this.plugin.getLogger().warn("User " + player.getUuid() + " - " + player.getUsername() +
                    " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                    " - denying login.");
        }

        Component reason = TranslationManager.render(Message.LOADING_STATE_ERROR.build(), player.getLanguage());
        player.getPacketHandler().disconnect(PlainTextComponentSerializer.plainText().serialize(reason));
    }

    private void onPlayerQuit(PlayerDisconnectEvent e) {
        final PlayerRef player = e.getPlayerRef();
        handleDisconnect(player.getUuid());
    }

}
