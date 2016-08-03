package me.lucko.luckperms.listeners;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.UuidCache;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.UUID;

@AllArgsConstructor
public class PlayerListener implements Listener {
    private static final String KICK_MESSAGE = Util.color(Message.PREFIX + "User data could not be loaded. Please contact an administrator.");
    private final LPBukkitPlugin plugin;

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        final long startTime = System.currentTimeMillis();
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // Datastore is disabled, prevent players from joining the server
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, KICK_MESSAGE);
            return;
        }
        
        final UuidCache cache = plugin.getUuidCache();
        if (!cache.isOnlineMode()) {
            UUID uuid = plugin.getDatastore().getUUID(e.getName());
            if (uuid != null) {
                cache.addToCache(e.getUniqueId(), uuid);
            } else {
                // No previous data for this player
                cache.addToCache(e.getUniqueId(), e.getUniqueId());
                plugin.getDatastore().saveUUIDData(e.getName(), e.getUniqueId(), b -> {});
            }
        } else {
            // Online mode, no cache needed. This is just for name -> uuid lookup.
            plugin.getDatastore().saveUUIDData(e.getName(), e.getUniqueId(), b -> {});
        }

        plugin.getDatastore().loadOrCreateUser(cache.getUUID(e.getUniqueId()), e.getName());
        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            plugin.getLogger().warning("Processing login for " + e.getName() + " took " + time + "ms.");
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        final Player player = e.getPlayer();
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(e.getPlayer().getUniqueId()));

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
        refreshPlayer(e.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        final Player player = e.getPlayer();
        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect;
        cache.clearCache(player.getUniqueId());

        final User user = plugin.getUserManager().getUser(cache.getUUID(player.getUniqueId()));
        plugin.getUserManager().unloadUser(user);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        refreshPlayer(e.getPlayer());
    }

    private void refreshPlayer(Player p) {
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(p.getUniqueId()));
        if (user != null) {
            user.refreshPermissions();
        }
    }

}
