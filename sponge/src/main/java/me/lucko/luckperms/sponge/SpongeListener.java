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

package me.lucko.luckperms.sponge;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.utils.LoginHelper;
import me.lucko.luckperms.sponge.timings.LPTiming;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;

import co.aikar.timings.Timing;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class SpongeListener {
    private final LPSpongePlugin plugin;

    private final Set<UUID> deniedAsyncLogin = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    @Listener(order = Order.EARLY)
    @IsCancelled(Tristate.UNDEFINED)
    public void onClientAuth(ClientConnectionEvent.Auth e) {
        /* Called when the player first attempts a connection with the server.
           Listening on AFTER_PRE priority to allow plugins to modify username / UUID data here. (auth plugins) */

        final GameProfile p = e.getProfile();

        /* either the plugin hasn't finished starting yet, or there was an issue connecting to the DB, performing file i/o, etc.
           we don't let players join in this case, because it means they can connect to the server without their permissions data.
           some server admins rely on negating perms to stop users from causing damage etc, so it's really important that
           this data is loaded. */
        if (!plugin.getStorage().isAcceptingLogins()) {

            // log that the user tried to login, but was denied at this stage.
            deniedAsyncLogin.add(p.getUniqueId());

            // actually deny the connection.
            plugin.getLog().warn("Permissions storage is not loaded. Denying connection from: " + p.getUniqueId() + " - " + p.getName());
            e.setCancelled(true);
            e.setMessageCancelled(false);
            //noinspection deprecation
            e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.asString(plugin.getLocaleManager())));
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
            LoginHelper.loadUser(plugin, p.getUniqueId(), p.getName().orElseThrow(() -> new RuntimeException("No username present for user " + p.getUniqueId())), false);
        } catch (Exception ex) {
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
        try (Timing ignored = plugin.getTimings().time(LPTiming.ON_CLIENT_LOGIN)) {
            /* Called when the player starts logging into the server.
               At this point, the users data should be present and loaded.
               Listening on LOW priority to allow plugins to further modify data here. (auth plugins, etc.) */

            final GameProfile player = e.getProfile();

            final User user = plugin.getUserManager().getIfLoaded(plugin.getUuidCache().getUUID(player.getUniqueId()));

            /* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
            if (user == null) {
                deniedLogin.add(player.getUniqueId());

                plugin.getLog().warn("User " + player.getUniqueId() + " - " + player.getName() + " doesn't have data pre-loaded. - denying login.");
                e.setCancelled(true);
                e.setMessageCancelled(false);
                //noinspection deprecation
                e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(Message.LOADING_ERROR.asString(plugin.getLocaleManager())));
                return;
            }

            // Attempt to pre-process some permissions for the user to save time later. Might not work, but it's better than nothing.
            Optional<Player> p = e.getCause().first(Player.class);
            if (p.isPresent()) {
                MutableContextSet context = MutableContextSet.fromSet(plugin.getContextManager().getApplicableContext(p.get()));

                List<String> worlds = plugin.getGame().isServerAvailable() ? plugin.getGame().getServer().getWorlds().stream()
                        .map(World::getName)
                        .collect(Collectors.toList()) : Collections.emptyList();

                plugin.doAsync(() -> {
                    UserData data = user.getUserData();
                    data.preCalculate(plugin.getService().calculateContexts(context.makeImmutable()));

                    for (String world : worlds) {
                        MutableContextSet modified = MutableContextSet.fromSet(context);
                        modified.removeAll("world");
                        modified.add("world", world);
                        data.preCalculate(plugin.getService().calculateContexts(modified.makeImmutable()));
                    }
                });
            }
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

    @Listener(order = Order.EARLY)
    public void onClientJoin(ClientConnectionEvent.Join e) {
        // Refresh permissions again
        plugin.doAsync(() -> LoginHelper.refreshPlayer(plugin, e.getTargetEntity().getUniqueId()));
    }

    @Listener(order = Order.POST)
    public void onClientLeave(ClientConnectionEvent.Disconnect e) {
        /* We don't actually remove the user instance here, as Sponge likes to keep performing checks
           on players when they disconnect. The instance gets cleared up on a housekeeping task
           after a period of inactivity. */
        try (Timing ignored = plugin.getTimings().time(LPTiming.ON_CLIENT_LEAVE)) {
            final UuidCache cache = plugin.getUuidCache();

            // Unload the user from memory when they disconnect
            cache.clearCache(e.getTargetEntity().getUniqueId());
        }
    }

    @Listener
    public void onSendCommand(SendCommandEvent e) {
        CommandSource source = e.getCause().first(CommandSource.class).orElse(null);
        if (source == null) return;

        final String name = e.getCommand().toLowerCase();
        if (name.equals("op") || name.equals("deop")) {
            Message.OP_DISABLED_SPONGE.send(plugin.getSenderFactory().wrap(source));
        }
    }
}
