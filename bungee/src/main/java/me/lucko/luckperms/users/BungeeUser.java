package me.lucko.luckperms.users;

import me.lucko.luckperms.LPBungeePlugin;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class BungeeUser extends User {

    private final LPBungeePlugin plugin;

    public BungeeUser(UUID uuid, LPBungeePlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    public BungeeUser(UUID uuid, String username, LPBungeePlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        ProxiedPlayer player = plugin.getProxy().getPlayer(getUuid());
        if (player == null) return;

        // Clear existing permissions
        Collection<String> perms = new ArrayList<>(player.getPermissions());
        for (String p : perms) {
            player.setPermission(p, false);
        }

        // Re-add all defined permissions for the user
        Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), null);
        for (String node : local.keySet()) {
            player.setPermission(node, local.get(node));
        }
    }
}
