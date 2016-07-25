package me.lucko.luckperms.utils;

import lombok.Getter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UuidCache {

    private Map<String, UUID> cache;

    @Getter
    private final boolean onlineMode;

    public UuidCache(boolean onlineMode) {
        this.onlineMode = onlineMode;

        if (!onlineMode) {
            cache = new ConcurrentHashMap<>();
        }
    }

    public UUID getUUID(String name, UUID fallback) {
        return onlineMode ? fallback : (cache.containsKey(name) ? cache.get(name) : fallback);
    }

    public void addToCache(String name, UUID uuid) {
        if (onlineMode) return;
        cache.put(name, uuid);
    }

    public void clearCache(String name) {
        if (onlineMode) return;
        cache.remove(name);
    }

}
