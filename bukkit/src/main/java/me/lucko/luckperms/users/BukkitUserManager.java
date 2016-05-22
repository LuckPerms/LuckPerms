package me.lucko.luckperms.users;

import me.lucko.luckperms.LPBukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitUserManager extends UserManager {
    private final LPBukkitPlugin plugin;

    public BukkitUserManager(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void unloadUser(User user) {
        if (user != null) {

            if (user instanceof BukkitUser) {
                BukkitUser u = (BukkitUser) user;

                if (u.getAttachment() != null) {
                    Player player = Bukkit.getPlayer(u.getUuid());

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
        if (Bukkit.getPlayer(user.getUuid()) == null) {
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
}
