package me.lucko.luckperms.utils;

import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This UuidCache is a means of allowing users to have the same internal UUID across a network of offline mode servers
 * or mixed offline mode and online mode servers. Platforms running in offline mode generate a random UUID for a user when
 * they first join the server, but this UUID will then not be consistent across the network. LuckPerms will instead check
 * the datastore cache, to get a UUID for a user that is consistent across an entire network.
 *
 * If you want to get a user object from the datastore using the api on a server in offline mode, you will need to use this cache,
 * OR use Datastore#getUUID, for users that are not online.
 */
public class UuidCache {

    // External UUID --> Internal UUID
    private Map<UUID, UUID> cache;

    @Getter
    private final boolean onlineMode;

    public UuidCache(boolean onlineMode) {
        this.onlineMode = onlineMode;

        if (!onlineMode) {
            cache = new ConcurrentHashMap<>();
        }
    }

    public UUID getUUID(UUID external) {
        return onlineMode ? external : (cache.containsKey(external) ? cache.get(external) : external);
    }

    public UUID getExternalUUID(UUID internal) {
        if (onlineMode) return internal;

        Optional<UUID> external = cache.entrySet().stream().filter(e -> e.getValue().equals(internal)).map(Map.Entry::getKey).findFirst();
        return external.isPresent() ? external.get() : internal;
    }

    public void addToCache(UUID external, UUID internal) {
        if (onlineMode) return;
        cache.put(external, internal);
    }

    public void clearCache(UUID external) {
        if (onlineMode) return;
        cache.remove(external);
    }

}
