/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.bungee;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.defaults.Rule;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class BungeeListener implements Listener {
    private final LPBungeePlugin plugin;

    private final Set<UUID> deniedLogin = Collections.synchronizedSet(new HashSet<>());

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(LoginEvent e) {
        /* Called when the player first attempts a connection with the server.
           Listening on LOW priority to allow plugins to modify username / UUID data here. (auth plugins)

           Delay the login here, as we want to cache UUID data before the player is connected to a backend bukkit server.
           This means that a player will have the same UUID across the network, even if parts of the network are running in
           Offline mode. */

        // registers the plugins intent to modify this events state going forward.
        // this will prevent the event from completing until we're finished handling.
        e.registerIntent(plugin);

        final long startTime = System.currentTimeMillis();
        final PendingConnection c = e.getConnection();

        /* either the plugin hasn't finished starting yet, or there was an issue connecting to the DB, performing file i/o, etc.
           as this is bungeecord, we will still allow the login, as players can't really do much harm without permissions data.
           the proxy will just fallback to using the config file perms. */
        if (!plugin.getStorage().isAcceptingLogins()) {
            // log that the user tried to login, but was denied at this stage.
            deniedLogin.add(c.getUniqueId());
            return;
        }

        /* another plugin (or the proxy itself) has cancelled this connection already */
        if (e.isCancelled()) {
            plugin.getLog().warn("Connection from " + c.getUniqueId() + " was already denied. No permissions data will be loaded.");
            deniedLogin.add(c.getUniqueId());
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
        plugin.doAsync(() -> {
            final UuidCache cache = plugin.getUuidCache();

            if (!plugin.getConfiguration().get(ConfigKeys.USE_SERVER_UUIDS)) {
                UUID uuid = plugin.getStorage().getUUID(c.getName()).join();
                if (uuid != null) {
                    cache.addToCache(c.getUniqueId(), uuid);
                } else {
                    // No previous data for this player
                    plugin.getApiProvider().getEventFactory().handleUserFirstLogin(c.getUniqueId(), c.getName());
                    cache.addToCache(c.getUniqueId(), c.getUniqueId());

                    // Join this call, as we want this to be set for when the player connects to the backend.
                    plugin.getStorage().force().saveUUIDData(c.getName(), c.getUniqueId()).join();
                }
            } else {
                String name = plugin.getStorage().getName(c.getUniqueId()).join();
                if (name == null) {
                    plugin.getApiProvider().getEventFactory().handleUserFirstLogin(c.getUniqueId(), c.getName());
                }

                // Online mode, no cache needed. This is just for name -> uuid lookup.
                // Again, join this call so the data is available for the backend.
                plugin.getStorage().force().saveUUIDData(c.getName(), c.getUniqueId()).join();
            }

            /* We have to make a new user on this thread whilst the connection is being held, or we get concurrency issues
               as the Bukkit server and the BungeeCord server try to make a new user at the same time. */
            plugin.getStorage().force().loadUser(cache.getUUID(c.getUniqueId()), c.getName()).join();

            User user = plugin.getUserManager().get(cache.getUUID(c.getUniqueId()));
            if (user == null) {
                plugin.getLog().warn("Failed to load user: " + c.getName());
            } else {
                // Setup defaults for the user
                boolean save = false;
                for (Rule rule : plugin.getConfiguration().get(ConfigKeys.DEFAULT_ASSIGNMENTS)) {
                    if (rule.apply(user)) {
                        save = true;
                    }
                }

                // If they were given a default, persist the new assignments back to the storage.
                if (save) {
                    plugin.getStorage().force().saveUser(user).join();
                }

                user.setupData(false); // Pretty nasty calculation call. Sets up the caching system so data is ready when the user joins.
            }

            final long time = System.currentTimeMillis() - startTime;
            if (time >= 1000) {
                plugin.getLog().warn("Processing login for " + c.getName() + " took " + time + "ms.");
            }

            // finally, complete out intent to modify state, so the proxy can continue handling the connection.
            e.completeIntent(plugin);

            // schedule a cleanup of the users data in a few seconds.
            // this should cover the eventuality that the login fails.
            plugin.getUserManager().scheduleUnload(c.getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId()));

        if (user == null) {
            plugin.getProxy().getScheduler().schedule(plugin, () -> {
                if (!player.isConnected()) {
                    return;
                }

                player.sendMessage(new TextComponent(Message.LOADING_ERROR.asString(plugin.getLocaleManager())));
            }, 3, TimeUnit.SECONDS);
        }
    }

    // Wait until the last priority to unload, so plugins can still perform permission checks on this event
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        // Request that the users data is unloaded.
        plugin.getUserManager().scheduleUnload(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPermissionCheck(PermissionCheckEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        final ProxiedPlayer player = ((ProxiedPlayer) e.getSender());

        User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return;
        }

        UserData userData = user.getUserData();
        if (userData == null) {
            plugin.getLog().warn("Player " + player.getName() + " does not have any user data setup.");
            plugin.doAsync(() -> user.setupData(false));
            return;
        }

        Contexts contexts = new Contexts(
                plugin.getContextManager().getApplicableContext(player),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                true,
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                false
        );

        e.setHasPermission(userData.getPermissionData(contexts).getPermissionValue(e.getPermission()).asBoolean());
    }

    // We don't pre-process all servers, so we have to do it here.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerSwitch(ServerConnectEvent e) {
        String serverName = e.getTarget().getName();
        UUID uuid = e.getPlayer().getUniqueId();

        plugin.doAsync(() -> {
            MutableContextSet set = MutableContextSet.create();
            set.add("server", plugin.getConfiguration().get(ConfigKeys.SERVER));
            set.add("world", serverName);

            User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(uuid));
            if (user == null) {
                return;
            }
            UserData userData = user.getUserData();
            if (userData == null) {
                return;
            }

            Contexts contexts = new Contexts(
                    set.makeImmutable(),
                    plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_PERMS),
                    plugin.getConfiguration().get(ConfigKeys.INCLUDING_GLOBAL_WORLD_PERMS),
                    true,
                    plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_GROUPS),
                    plugin.getConfiguration().get(ConfigKeys.APPLYING_GLOBAL_WORLD_GROUPS),
                    false
            );

            userData.preCalculate(contexts);
        });
    }
}
