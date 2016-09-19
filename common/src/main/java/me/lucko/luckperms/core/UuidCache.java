/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.UUID;

/**
 * @see me.lucko.luckperms.api.UuidCache for docs
 */
public class UuidCache {

    // External UUID --> Internal UUID
    private BiMap<UUID, UUID> cache;

    @Getter
    private final boolean onlineMode;

    public UuidCache(boolean onlineMode) {
        this.onlineMode = onlineMode;

        if (!onlineMode) {
            cache = Maps.synchronizedBiMap(HashBiMap.create());
        }
    }

    public UUID getUUID(UUID external) {
        return onlineMode ? external : cache.getOrDefault(external, external);
    }

    public UUID getExternalUUID(UUID internal) {
        return onlineMode ? internal : cache.inverse().getOrDefault(internal, internal);
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
