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

package me.lucko.luckperms.common.calculators;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.LuckPermsPlugin;

import java.util.List;
import java.util.Map;

/**
 * Calculates and caches permissions
 */
@RequiredArgsConstructor
public class PermissionCalculator {
    private final LuckPermsPlugin plugin;
    private final String objectName;
    private final List<PermissionProcessor> processors;

    private final LoadingCache<String, Tristate> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Tristate>() {
                @Override
                public Tristate load(String s) {
                    return lookupPermissionValue(s);
                }
            });

    public void invalidateCache() {
        cache.invalidateAll();
    }

    public Tristate getPermissionValue(String permission) {
        permission = permission.toLowerCase();
        Tristate t =  cache.getUnchecked(permission);
        plugin.getDebugHandler().offer(objectName, permission, t);
        plugin.getPermissionCache().offer(permission);
        return t;
    }

    private Tristate lookupPermissionValue(String permission) {
        for (PermissionProcessor processor : processors) {
            Tristate v = processor.hasPermission(permission);
            if (v == Tristate.UNDEFINED) {
                continue;
            }

            return v;
        }

        return Tristate.UNDEFINED;
    }

    public synchronized void updateBacking(Map<String, Boolean> map) {
        for (PermissionProcessor processor : processors) {
            processor.updateBacking(map);
        }
    }
}
