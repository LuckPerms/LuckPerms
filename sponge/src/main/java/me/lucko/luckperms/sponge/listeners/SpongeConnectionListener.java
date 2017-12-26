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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.utils.LoginHelper;
import me.lucko.luckperms.common.utils.UuidCache;
import me.lucko.luckperms.sponge.LPSpongePlugin;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class SpongeConnectionListener {
    private final LPSpongePlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    @Listener(order = Order.EARLY)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientAuth(ClientConnectionEvent.Auth e) {
        /* Called when the player first attempts a connection with the server.
           Listening on AFTER_PRE priority to allow plugins to modify username / UUID data here. (auth plugins) */

        final GameProfile p = e.getProfile();
        final String username = p.getName().orElseThrow(() -> new RuntimeException("No username present for user " + p.getUniqueId()));

        if (plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.getLog().info("Processing auth event for " + p.getUniqueId() + " - " + p.getName());
        }

        plugin.getUniqueConnections().add(p.getUniqueId());

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = LoginHelper.loadUser(plugin, p.getUniqueId(), username, false);
            plugin.getApiProvider().getEventFactory().handleUserLoginProcess(p.getUniqueId(), username, user);
        } catch (Exception ex) {
            plugin.getLog().severe("Exception occured whilst loading data for " + p.getUniqueId() + " - " + p.getName());
            ex.printStackTrace();

            deniedAsyncLogin.add(p.getUniqueId());

            e.setCancelled(true);
            e.setMessageCancelled(false);
            //noinspection deprecation
            e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.asString(plugin.getLocaleManager())));
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientAuthMonitor(ClientConnectionEvent.Auth e) {
        /* Listen to see if the event was cancelled after we initially handled the connection
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW.
        if (deniedAsyncLogin.remove(e.getProfile().getUniqueId())) {

            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (e.isCancelled()) {
                plugin.getLog().severe("Player connection was re-allowed for " + e.getProfile().getUniqueId());
                e.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.FIRST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientLogin(ClientConnectionEvent.Login e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded.
           Listening on LOW priority to allow plugins to further modify data here. (auth plugins, etc.) */

        final GameProfile player = e.getProfile();

        if (plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            plugin.getLog().info("Processing login event for " + player.getUniqueId() + " - " + player.getName());
        }

        final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));

        /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
        if (user == null) {
            deniedLogin.add(player.getUniqueId());

            plugin.getLog().warn("User " + player.getUniqueId() + " - " + player.getName() + " doesn't have data pre-loaded. - denying login.");
            e.setCancelled(true);
            e.setMessageCancelled(false);
            //noinspection deprecation
            e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.asString(plugin.getLocaleManager())));
        }
    }

    @Listener(order = Order.LAST)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientLoginMonitor(ClientConnectionEvent.Login e) {
        /* Listen to see if the event was cancelled after we initially handled the login
           If the connection was cancelled here, we need to do something to clean up the data that was loaded. */

        // Check to see if this connection was denied at LOW. Even if it was denied at LOW, their data will still be present.
        if (deniedLogin.remove(e.getProfile().getUniqueId())) {
            // This is a problem, as they were denied at low priority, but are now being allowed.
            if (!e.isCancelled()) {
                plugin.getLog().severe("Player connection was re-allowed for " + e.getProfile().getUniqueId());
                e.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.POST)
    public void onClientLeave(ClientConnectionEvent.Disconnect e) {
        /* We don't actually remove the user instance here, as Sponge likes to keep performing checks
           on players when they disconnect. The instance gets cleared up on a housekeeping task
           after a period of inactivity. */

        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect
        cache.clearCache(e.getTargetEntity().getUniqueId());
    }

}
