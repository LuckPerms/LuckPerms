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

package me.lucko.luckperms.common.calculators;

import lombok.RequiredArgsConstructor;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.verbose.CheckOrigin;

import java.util.List;
import java.util.Map;

/**
 * Calculates and caches permissions
 */
@RequiredArgsConstructor
public class PermissionCalculator implements CacheLoader<String, Tristate> {
    private final LuckPermsPlugin plugin;
    private final PermissionCalculatorMetadata metadata;
    private final List<PermissionProcessor> processors;

    // caches lookup calls.
    private final LoadingCache<String, Tristate> lookupCache = Caffeine.newBuilder().build(this);

    public void invalidateCache() {
        lookupCache.invalidateAll();
    }

    public Tristate getPermissionValue(String permission, CheckOrigin origin) {

        // convert the permission to lowercase, as all values in the backing map are also lowercase.
        // this allows fast case insensitive lookups
        permission = permission.toLowerCase().intern();

        // get the result
        Tristate result = lookupCache.get(permission);

        // log this permission lookup to the verbose handler
        plugin.getVerboseHandler().offerCheckData(origin, metadata.getObjectName(), metadata.getContext(), permission, result);

        // return the result
        return result;
    }

    @Override
    public Tristate load(String permission) {

        // offer the permission to the permission vault
        // we only need to do this once per permission, so it doesn't matter
        // that this call is behind the cache.
        plugin.getPermissionVault().offer(permission);

        for (PermissionProcessor processor : processors) {
            Tristate result = processor.hasPermission(permission);
            if (result == Tristate.UNDEFINED) {
                continue;
            }

            return result;
        }

        return Tristate.UNDEFINED;
    }

    public synchronized void updateBacking(Map<String, Boolean> map) {
        for (PermissionProcessor processor : processors) {
            processor.updateBacking(map);
        }
    }
}
