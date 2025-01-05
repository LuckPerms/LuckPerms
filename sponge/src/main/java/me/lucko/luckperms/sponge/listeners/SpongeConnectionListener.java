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

package me.lucko.luckperms.sponge.listeners;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.sponge.LPSpongePlugin;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.util.Identifiable;
import org.spongepowered.api.util.Tristate;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SpongeConnectionListener extends AbstractConnectionListener {

    private final LPSpongePlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    public SpongeConnectionListener(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Listener(order = Order.EARLY)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientAuth(ServerSideConnectionEvent.Auth e) {
        /* Called when the player first attempts a connection with the server.
           Listening on AFTER_PRE priority to allow plugins to modify username / UUID data here. (auth plugins)
           Also, give other plugins a chance to cancel the event. */

        final GameProfile profile = e.profile();
        final String username = profile.name().orElseThrow(() -> new RuntimeException("No username present for user " + profile.uniqueId()));

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing auth event for " + profile.uniqueId() + " - " + profile.name());
        }

        if (e.isCancelled()) {
            // another plugin has disallowed the login.
            this.plugin.getLogger().info("Another plugin has cancelled the connection for " + profile.uniqueId() + " - " + username + ". No permissions data will be loaded.");
            this.deniedAsyncLogin.add(profile.uniqueId());
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
            User user = loadUser(profile.uniqueId(), username);
            recordConnection(profile.uniqueId());
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.uniqueId(), username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + profile.uniqueId() + " - " + profile.name(), ex);

            this.deniedAsyncLogin.add(profile.uniqueId());

            e.setCancelled(true);
            e.setMessage(TranslationManager.render(Message.LOADING_DATABASE_ERROR.build()));
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(profile.uniqueId(), username, null);
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientAuthMonitor(ServerSideConnectionEvent.Auth e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (this.deniedAsyncLogin.remove(e.profile().uniqueId())) {

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.isCancelled()) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.profile().uniqueId());
                e.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientLogin(ServerSideConnectionEvent.Login e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded.
           Listening on LOW priority to allow plugins to further modify data here. (auth plugins, etc.) */

        final GameProfile profile = e.profile();

        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing login event for " + profile.uniqueId() + " - " + profile.name());
        }

        final User user = this.plugin.getUserManager().getIfLoaded(profile.uniqueId());

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            this.deniedLogin.add(profile.uniqueId());

            if (!getUniqueConnections().contains(profile.uniqueId())) {
                this.plugin.getLogger().warn("User " + profile.uniqueId() + " - " + profile.name() +
                        " doesn't have data pre-loaded, they have never been processed during pre-login in this session." +
                        " - denying login.");
            } else {
                this.plugin.getLogger().warn("User " + profile.uniqueId() + " - " + profile.name() +
                        " doesn't currently have data pre-loaded, but they have been processed before in this session." +
                        " - denying login.");
            }

            e.setCancelled(true);
            e.setMessage(TranslationManager.render(Message.LOADING_STATE_ERROR.build()));
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientLoginMonitor(ServerSideConnectionEvent.Login e) {
        /* Listen to see if the event was cancelled after we initially handled the login
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW. Even if it was denied at LOW, their data will still be present.
        if (this.deniedLogin.remove(e.profile().uniqueId())) {
            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (!e.isCancelled()) {
                this.plugin.getLogger().severe("Player connection was re-allowed for " + e.profile().uniqueId());
                e.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.POST)
    public void onClientLeave(ServerSideConnectionEvent.Disconnect e) {
        Optional<GameProfile> profile = e.profile();
        profile.ifPresent(p -> handleDisconnect(p.uniqueId()));
    }

}
