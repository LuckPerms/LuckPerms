package me.lucko.luckperms.listeners;

import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.UuidCache;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

@RequiredArgsConstructor
public class PlayerListener implements Listener {
    private static final String KICK_MESSAGE = Util.color(Message.PREFIX + "User data could not be loaded. Please contact an administrator.");
    private final LPBukkitPlugin plugin;

    /*
    cache: username --> uuid
        returns mojang if not in offline mode


    if server in offline mode:
        go to datastore, look for uuid, add to cache.

    *** player prelogin, load or create, using CACHE uuid



    *** player login, we get their username and check if it's there

    *** player join, save uuid data and refresh

    *** player quit, unload

     */

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // Datastore is disabled, prevent players from joining the server
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
            return;
        }
        
        final UuidCache cache = plugin.getUuidCache();
        if (!cache.isOnlineMode()) {
            UUID uuid = plugin.getDatastore().getUUID(e.getName());
            if (uuid != null) {
                cache.addToCache(e.getName(), uuid);
            } else {
                cache.addToCache(e.getName(), e.getUniqueId());
                plugin.getDatastore().saveUUIDData(e.getName(), e.getUniqueId(), b -> {});
            }
        } else {
            plugin.getDatastore().saveUUIDData(e.getName(), e.getUniqueId(), b -> {});
        }

        plugin.getDatastore().loadOrCreateUser(cache.getUUID(e.getName(), e.getUniqueId()), e.getName());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        final Player player = e.getPlayer();
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(e.getPlayer().getName(), e.getPlayer().getUniqueId()));

        if (user == null) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
            return;
        }

        if (user instanceof BukkitUser) {
            BukkitUser u = (BukkitUser) user;
            u.setAttachment(player.addAttachment(plugin));
        }

        user.refreshPermissions();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Refresh permissions again
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(e.getPlayer().getName(), e.getPlayer().getUniqueId()));
        if (user != null) {
            user.refreshPermissions();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect;
        cache.clearCache(player.getName());

        final User user = plugin.getUserManager().getUser(cache.getUUID(player.getName(), player.getUniqueId()));
        plugin.getUserManager().unloadUser(user);
    }

}
