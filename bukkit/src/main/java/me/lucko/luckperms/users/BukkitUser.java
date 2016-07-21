package me.lucko.luckperms.users;

import lombok.Getter;
import lombok.Setter;
import me.lucko.luckperms.LPBukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Map;
import java.util.UUID;

public class BukkitUser extends User {

    private final LPBukkitPlugin plugin;

    @Getter
    @Setter
    private PermissionAttachment attachment = null;

    BukkitUser(UUID uuid, LPBukkitPlugin plugin) {
        super(uuid, plugin);
        this.plugin = plugin;
    }

    BukkitUser(UUID uuid, String username, LPBukkitPlugin plugin) {
        super(uuid, username, plugin);
        this.plugin = plugin;
    }

    @Override
    public void refreshPermissions() {
        plugin.doSync(() -> {
            final Player player = plugin.getServer().getPlayer(getUuid());
            if (player == null) return;

            if (attachment == null) {
                getPlugin().getLogger().warning("User " + getName() + " does not have a permissions attachment defined.");
                setAttachment(player.addAttachment(plugin));
            }

            // Clear existing permissions
            attachment.getPermissions().keySet().forEach(p -> attachment.setPermission(p, false));

            // Re-add all defined permissions for the user
            Map<String, Boolean> local = getLocalPermissions(getPlugin().getConfiguration().getServer(), null);
            local.entrySet().forEach(e -> attachment.setPermission(e.getKey(), e.getValue()));
        });
    }
}
