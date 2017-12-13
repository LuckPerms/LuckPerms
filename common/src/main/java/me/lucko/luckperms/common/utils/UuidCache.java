/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
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

package me.lucko.luckperms.common.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import me.lucko.luckperms.common.api.delegates.misc.ApiUuidCache;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.UUID;

/**
 * @see me.lucko.luckperms.api.UuidCache for docs
 */
@RequiredArgsConstructor
public class UuidCache {
    private final LuckPermsPlugin plugin;

    // External UUID --> Internal UUID
    private final BiMap<UUID, UUID> cache = Maps.synchronizedBiMap(HashBiMap.create());

    @Getter
    private final ApiUuidCache delegate = new ApiUuidCache(this);

    public UUID getUUID(UUID external) {
        return inUse() ? external : cache.getOrDefault(external, external);
    }

    public UUID getExternalUUID(UUID internal) {
        return inUse() ? internal : cache.inverse().getOrDefault(internal, internal);
    }

    public void addToCache(UUID external, UUID internal) {
        if (inUse()) return;
        cache.forcePut(external, internal);
    }

    public void clearCache(UUID external) {
        if (inUse()) return;
        cache.remove(external);
    }

    public int getSize() {
        return inUse() ? 0 : cache.size();
    }

    private boolean inUse() {
        return plugin.getConfiguration().get(ConfigKeys.USE_SERVER_UUIDS);
    }

}
