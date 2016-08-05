package me.lucko.luckperms.utils;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.users.User;

import java.util.UUID;

@AllArgsConstructor
public class AbstractListener {
    private final LuckPermsPlugin plugin;

    protected void onAsyncLogin(UUID u, String username) {
        final long startTime = System.currentTimeMillis();

        final UuidCache cache = plugin.getUuidCache();
        if (!cache.isOnlineMode()) {
            UUID uuid = plugin.getDatastore().getUUID(username);
            if (uuid != null) {
                cache.addToCache(u, uuid);
            } else {
                // No previous data for this player
                cache.addToCache(u, u);
                plugin.getDatastore().saveUUIDData(username, u, b -> {});
            }
        } else {
            // Online mode, no cache needed. This is just for name -> uuid lookup.
            plugin.getDatastore().saveUUIDData(username, u, b -> {});
        }

        plugin.getDatastore().loadOrCreateUser(cache.getUUID(u), username);
        final long time = System.currentTimeMillis() - startTime;
        if (time >= 1000) {
            plugin.getLog().warn("Processing login for " + username + " took " + time + "ms.");
        }
    }

    protected void onLogin(UUID uuid, String username) {

    }

    protected void onJoin(UUID uuid, String username) {

    }

    protected void onLeave(UUID uuid) {
        final UuidCache cache = plugin.getUuidCache();

        // Unload the user from memory when they disconnect;
        cache.clearCache(uuid);

        final User user = plugin.getUserManager().getUser(cache.getUUID(uuid));
        plugin.getUserManager().unloadUser(user);
    }

    protected void refreshPlayer(UUID uuid) {
        final User user = plugin.getUserManager().getUser(plugin.getUuidCache().getUUID(uuid));
        if (user != null) {
            user.refreshPermissions();
        }
    }
}
