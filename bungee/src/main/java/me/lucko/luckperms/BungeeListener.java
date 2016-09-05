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

package me.lucko.luckperms;

import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.core.UuidCache;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.AbstractListener;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.Collections;
import java.util.Map;
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
            return;
        }

        final ProxiedPlayer player = ((ProxiedPlayer) e.getSender());
        final User user = plugin.getUserManager().get(plugin.getUuidCache().getUUID(player.getUniqueId()));
        if (user == null) return;


        final String server = player.getServer() == null ? null : (player.getServer().getInfo() == null ? null : player.getServer().getInfo().getName());
        Map<String, Boolean> local = user.getLocalPermissions(plugin.getConfiguration().getServer(), server, null, Collections.singletonList(e.getPermission()));
        for (Map.Entry<String, Boolean> en : local.entrySet()) {
            if (en.getKey().equalsIgnoreCase(e.getPermission())) {
                e.setHasPermission(en.getValue());
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(LoginEvent e) {
        /* Delay the login here, as we want to cache UUID data before the player is connected to a backend bukkit server.
           This means that a player will have the same UUID across the network, even if parts of the network are running in
           Offline mode. */
        e.registerIntent(plugin);
        plugin.doAsync(() -> {
            final long startTime = System.currentTimeMillis();
            final UuidCache cache = plugin.getUuidCache();
            final PendingConnection c = e.getConnection();

            if (!cache.isOnlineMode()) {
                UUID uuid = plugin.getDatastore().getUUID(c.getName());
                if (uuid != null) {
                    cache.addToCache(c.getUniqueId(), uuid);
                } else {
                    // No previous data for this player
                    cache.addToCache(c.getUniqueId(), c.getUniqueId());
                    plugin.getDatastore().saveUUIDData(c.getName(), c.getUniqueId());
                }
            } else {
                // Online mode, no cache needed. This is just for name -> uuid lookup.
                plugin.getDatastore().saveUUIDData(c.getName(), c.getUniqueId());
            }

            // We have to make a new user on this thread whilst the connection is being held, or we get concurrency issues as the Bukkit server
            // and the BungeeCord server try to make a new user at the same time.
            plugin.getDatastore().loadUser(cache.getUUID(c.getUniqueId()), c.getName());
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
        } else {
            user.refreshPermissions();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        onLeave(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerServerSwitch(ServerSwitchEvent e) {
        refreshPlayer(e.getPlayer().getUniqueId());
    }
}
