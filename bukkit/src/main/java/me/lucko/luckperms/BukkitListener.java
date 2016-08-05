package me.lucko.luckperms;

import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.BukkitUser;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.AbstractListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

class BukkitListener extends AbstractListener implements Listener {
    private final LPBukkitPlugin plugin;

    BukkitListener(LPBukkitPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent e) {
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // Datastore is disabled, prevent players from joining the server
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.toString());
            return;
        }

        onAsyncLogin(e.getUniqueId(), e.getName());
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent e) {
        final Player player = e.getPlayer();
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(player.getUniqueId()));

        if (user == null) {
            e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_ERROR.toString());
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
        refreshPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        refreshPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        onLeave(e.getPlayer().getUniqueId());
    }
}
