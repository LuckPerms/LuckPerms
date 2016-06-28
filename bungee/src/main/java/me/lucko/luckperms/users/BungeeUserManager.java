package me.lucko.luckperms.users;

import me.lucko.luckperms.LPBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class BungeeUserManager extends UserManager {
    private final LPBungeePlugin plugin;

    public BungeeUserManager(LPBungeePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void unloadUser(User user) {
        if (user != null) {
            // Cannot clear the ProxiedPlayer's permission map, they're leaving so that will get GCed anyway
            // Calling getPermissions.clear() throws an UnsupportedOperationException
            getUsers().remove(user.getUuid());
        }
    }

    @Override
    public void cleanupUser(User user) {
        if (plugin.getProxy().getPlayer(user.getUuid()) == null) {
            unloadUser(user);
        }
    }

    @Override
    public User makeUser(UUID uuid) {
        return new BungeeUser(uuid, plugin);
    }

    @Override
    public User makeUser(UUID uuid, String username) {
        return new BungeeUser(uuid, username, plugin);
    }

    @Override
    public void updateAllUsers() {
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            plugin.getDatastore().loadUser(p.getUniqueId());
        }
    }
}
