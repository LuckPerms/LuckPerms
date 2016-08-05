package me.lucko.luckperms.users;

import me.lucko.luckperms.LPSpongePlugin;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class SpongeUser extends User {
    private final LPSpongePlugin plugin;

    SpongeUser(UUID uuid, LPSpongePlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    SpongeUser(UUID uuid, String username, LPSpongePlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        Optional<Player> p = plugin.getGame().getServer().getPlayer(plugin.getUuidCache().getExternalUUID(getUuid()));
        if (!p.isPresent()) return;

        final Player player = p.get();

        // Clear existing permissions
        player.getSubjectData().clearParents();
        player.getSubjectData().clearPermissions();

        // Re-add all defined permissions for the user
        final String world = player.getWorld().getName();
        Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), world, null);
        local.entrySet().forEach(e -> player.getSubjectData().setPermission(new HashSet<>(), e.getKey(), Tristate.fromBoolean(e.getValue())));
    }
}
