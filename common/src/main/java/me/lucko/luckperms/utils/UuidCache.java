package me.lucko.luckperms.utils;

import lombok.Getter;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
