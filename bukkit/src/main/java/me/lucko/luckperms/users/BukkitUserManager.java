package me.lucko.luckperms.users;

import me.lucko.luckperms.LPBukkitPlugin;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitUserManager extends UserManager {
    private final LPBukkitPlugin plugin;

    public BukkitUserManager(LPBukkitPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void unloadUser(User user) {
        if (user != null) {

            if (user instanceof BukkitUser) {
                BukkitUser u = (BukkitUser) user;

                if (u.getAttachment() != null) {
                    Player player = plugin.getServer().getPlayer(u.getUuid());

                    if (player != null) {
                        player.removeAttachment(u.getAttachment());
                    }
                    u.setAttachment(null);
                }
            }

            getUsers().remove(user.getUuid());
        }
    }

    @Override
    public void cleanupUser(User user) {
        if (plugin.getServer().getPlayer(user.getUuid()) == null) {
            unloadUser(user);
        }
    }

    @Override
    public User makeUser(UUID uuid) {
        return new BukkitUser(uuid, plugin);
    }

    @Override
    public User makeUser(UUID uuid, String username) {
        return new BukkitUser(uuid, username, plugin);
    }

    @Override
    public void updateAllUsers() {
        plugin.getServer().getOnlinePlayers().stream().map(Player::getUniqueId).forEach(u -> plugin.getDatastore().loadUser(u));
    }
}
