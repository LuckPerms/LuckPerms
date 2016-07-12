package me.lucko.luckperms.listeners;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBukkitPlugin;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@AllArgsConstructor
public class PlayerListener implements Listener {
    private final LPBukkitPlugin plugin;

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if (!plugin.getDatastore().isAcceptingLogins()) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Util.color(Message.PREFIX + "Error whilst validating login with the network. \nPlease contact an administrator."));
            return;
        }
        plugin.getDatastore().loadOrCreateUser(e.getUniqueId(), e.getName());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        Player player = e.getPlayer();
        User user = plugin.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                    Util.color(Message.PREFIX + "User data could not be loaded. Please contact an administrator."));
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
        // Save UUID data for the player
        plugin.getDatastore().saveUUIDData(e.getPlayer().getName(), e.getPlayer().getUniqueId(), success -> {});

        User user = plugin.getUserManager().getUser(e.getPlayer().getUniqueId());
        if (user != null) {
            // Refresh permissions again
            user.refreshPermissions();
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        // Unload the user from memory when they disconnect
        User user = plugin.getUserManager().getUser(player.getUniqueId());
        plugin.getUserManager().unloadUser(user);
    }

}
