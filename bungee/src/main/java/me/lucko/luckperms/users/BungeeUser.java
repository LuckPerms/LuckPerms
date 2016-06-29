package me.lucko.luckperms.users;

import me.lucko.luckperms.LPBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BungeeUser extends User {

    private final LPBungeePlugin plugin;

    BungeeUser(UUID uuid, LPBungeePlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    BungeeUser(UUID uuid, String username, LPBungeePlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        ProxiedPlayer player = plugin.getProxy().getPlayer(getUuid());
        if (player == null) return;

        // Clear existing permissions
        Collection<String> perms = new ArrayList<>(player.getPermissions());
        perms.stream().forEach(p -> player.setPermission(p, false));

        // Re-add all defined permissions for the user
        Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), null);
        local.entrySet().stream().forEach(e -> player.setPermission(e.getKey(), e.getValue()));
    }
}
