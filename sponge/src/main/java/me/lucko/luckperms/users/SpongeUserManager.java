package me.lucko.luckperms.users;

import me.lucko.luckperms.LPSpongePlugin;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Optional;
import java.util.UUID;

public class SpongeUserManager extends UserManager {
    private final LPSpongePlugin plugin;

    public SpongeUserManager(LPSpongePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void unloadUser(User user) {
        if (user != null) {
            Optional<Player> p = plugin.getGame().getServer().getPlayer(plugin.getUuidCache().getExternalUUID(user.getUuid()));
            if (p.isPresent()) {
                p.get().getSubjectData().clearParents();
                p.get().getSubjectData().clearPermissions();
            }
            getUsers().remove(user.getUuid());
        }
    }

    @Override
    public void cleanupUser(User user) {
        if (plugin.getGame().getServer().getPlayer(plugin.getUuidCache().getExternalUUID(user.getUuid())).isPresent()) {
            unloadUser(user);
        }
    }

    @Override
    public User makeUser(UUID uuid) {
        return new SpongeUser(uuid, plugin);
    }

    @Override
    public User makeUser(UUID uuid, String username) {
        return new SpongeUser(uuid, username, plugin);
    }

    @Override
    public void updateAllUsers() {
        plugin.getGame().getServer().getOnlinePlayers().stream()
                .map(p -> plugin.getUuidCache().getUUID(p.getUniqueId()))
                .forEach(u -> plugin.getDatastore().loadUser(u));
    }
}
