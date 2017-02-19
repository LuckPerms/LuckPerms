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

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.core.UuidCache;
import me.lucko.luckperms.common.core.model.User;
import me.lucko.luckperms.common.defaults.Rule;
import me.lucko.luckperms.common.utils.AbstractListener;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class BungeeListener extends AbstractListener implements Listener {
    private static final TextComponent WARN_MESSAGE = new TextComponent(Message.LOADING_ERROR.toString());
    private final LPBungeePlugin plugin;

    BungeeListener(LPBungeePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPermissionCheck(PermissionCheckEvent e) {
        if (!(e.getSender() instanceof ProxiedPlayer)) {
            e.setHasPermission(true);
            return;
        }

        final ProxiedPlayer player = ((ProxiedPlayer) e.getSender());

        User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) {
            return;
        }

        if (user.getUserData() == null) {
            plugin.getLog().warn("Player " + player.getName() + " does not have any user data setup.");
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

        e.setHasPermission(user.getUserData().getPermissionData(contexts).getPermissionValue(e.getPermission()).asBoolean());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent e) {
        /* Delay the login here, as we want to cache UUID data before the player is connected to a backend bukkit server.
           This means that a player will have the same UUID across the network, even if parts of the network are running in
           Offline mode. */
        e.registerIntent(plugin);
        plugin.doAsync(() -> {
            final long startTime = System.currentTimeMillis();
            final UuidCache cache = plugin.getUuidCache();
            final PendingConnection c = e.getConnection();

            if (!plugin.getConfiguration().get(ConfigKeys.ONLINE_MODE)) {
                UUID uuid = plugin.getStorage().getUUID(c.getName()).join();
                if (uuid != null) {
                    cache.addToCache(c.getUniqueId(), uuid);
                } else {
                    // No previous data for this player
                    plugin.getApiProvider().getEventFactory().handleUserFirstLogin(c.getUniqueId(), c.getName());
                    cache.addToCache(c.getUniqueId(), c.getUniqueId());
                    plugin.getStorage().force().saveUUIDData(c.getName(), c.getUniqueId()).join();
                }
            } else {
                String name = plugin.getStorage().getName(c.getUniqueId()).join();
                if (name == null) {
                    plugin.getApiProvider().getEventFactory().handleUserFirstLogin(c.getUniqueId(), c.getName());
                }

                // Online mode, no cache needed. This is just for name -> uuid lookup.
                plugin.getStorage().force().saveUUIDData(c.getName(), c.getUniqueId()).join();
            }

            // We have to make a new user on this thread whilst the connection is being held, or we get concurrency issues as the Bukkit server
            // and the BungeeCord server try to make a new user at the same time.
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
            e.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId()));

        if (user == null) {
            plugin.getProxy().getScheduler().schedule(plugin, () -> player.sendMessage(WARN_MESSAGE), 3, TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        onLeave(e.getPlayer().getUniqueId());
    }
}
