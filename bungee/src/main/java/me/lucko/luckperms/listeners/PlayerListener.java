package me.lucko.luckperms.listeners;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPBungeePlugin;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.data.Datastore;
import me.lucko.luckperms.users.User;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

@AllArgsConstructor
public class PlayerListener implements Listener {

    private final LPBungeePlugin plugin;

    @EventHandler
    public void onPlayerPostLogin(PostLoginEvent e) {
        final ProxiedPlayer player = e.getPlayer();

        plugin.getDatastore().loadOrCreateUser(player.getUniqueId(), player.getName(), success -> {
            if (!success) {
                e.getPlayer().sendMessage(new TextComponent(Util.color("&e&l[LP] &cPermissions data could not be loaded. Please contact an administrator.")));
            } else {
                User user = plugin.getUserManager().getUser(player.getUniqueId());
                user.refreshPermissions();
            }
        });

        plugin.getDatastore().saveUUIDData(e.getPlayer().getName(), e.getPlayer().getUniqueId(), success -> {});
    }

    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent e) {
        ProxiedPlayer player = e.getPlayer();

        // Unload the user from memory when they disconnect
        User user = plugin.getUserManager().getUser(player.getUniqueId());
        plugin.getUserManager().unloadUser(user);
    }
}
