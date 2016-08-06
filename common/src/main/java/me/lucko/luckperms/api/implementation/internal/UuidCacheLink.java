package me.lucko.luckperms.api.implementation.internal;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.UuidCache;

import java.util.UUID;

/**
 * Provides a link between {@link me.lucko.luckperms.api.UuidCache} and {@link me.lucko.luckperms.utils.UuidCache}
 */
@AllArgsConstructor
public class UuidCacheLink implements UuidCache {
    private final me.lucko.luckperms.utils.UuidCache master;

    @Override
    public UUID getUUID(UUID external) {
        return master.getUUID(external);
    }

    @Override
    public UUID getExternalUUID(UUID internal) {
        return master.getExternalUUID(internal);
    }
}
