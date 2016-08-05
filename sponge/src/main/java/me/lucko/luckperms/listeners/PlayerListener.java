package me.lucko.luckperms.listeners;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LPSpongePlugin;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.UuidCache;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.UUID;

@AllArgsConstructor
public class PlayerListener {
    private static final String KICK_MESSAGE = Util.color(Message.PREFIX + "User data could not be loaded. Please contact an administrator.");
    private final LPSpongePlugin plugin;

    @Listener
    public void onClientAuth(ClientConnectionEvent.Auth e) {
        final long startTime = System.currentTimeMillis();
        if (!plugin.getDatastore().isAcceptingLogins()) {
            // Datastore is disabled, prevent players from joining the server
            // Just don't load their data, they will be kickec at login
            return;
        }

        final UuidCache cache = plugin.getUuidCache();
        final GameProfile p = e.getProfile();
        final String name = p.getName().get();

        if (!cache.isOnlineMode()) {
            UUID uuid = plugin.getDatastore().getUUID(name);
            if (uuid != null) {
                cache.addToCache(p.getUniqueId(), uuid);
            } else {
                // No previous data for this player
                cache.addToCache(p.getUniqueId(), p.getUniqueId());
                plugin.getDatastore().saveUUIDData(name, p.getUniqueId(), b -> {});
            }
        } else {
            // Online mode, no cache needed. This is just for name -> uuid lookup.
            plugin.getDatastore().saveUUIDData(name, p.getUniqueId(), b -> {});
        }

        plugin.getDatastore().loadOrCreateUser(cache.getUUID(p.getUniqueId()), name);
        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            plugin.getLog().warn("Processing login for " + p.getName() + " took " + time + "ms.");
        }
    }

    @SuppressWarnings("deprecation")
    @Listener
    public void onClientLogin(ClientConnectionEvent.Login e) {
        final GameProfile player = e.getProfile();
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(player.getUniqueId()));

        if (user == null) {
            e.setCancelled(true);
            e.setMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(KICK_MESSAGE));
            return;
        }

        user.refreshPermissions();
    }

    @Listener
    public void onClientJoin(ClientConnectionEvent.Join e) {
        // Refresh permissions again
        refreshPlayer(e.getTargetEntity());
    }

    @Listener
    public void onPlayerTeleport(DisplaceEntityEvent.Teleport e) {
        final Entity entity = e.getTargetEntity();
        if (!(entity instanceof Player)){
            return;
        }

        refreshPlayer((Player) entity);
    }

    @Listener
    public void onClientLeave(ClientConnectionEvent.Disconnect e) {
        final Player player = e.getTargetEntity();
        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect;
        cache.clearCache(player.getUniqueId());

        final User user = plugin.getUserManager().getUser(cache.getUUID(player.getUniqueId()));
        plugin.getUserManager().unloadUser(user);
    }

    private void refreshPlayer(Player p) {
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(p.getUniqueId()));
        if (user != null) {
            user.refreshPermissions();
        }
    }
}
