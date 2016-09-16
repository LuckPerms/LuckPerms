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

package me.lucko.luckperms;

import com.google.common.base.Splitter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class BungeePlayerCache {
    private final LuckPermsPlugin plugin;
    private final UUID uuid;
    private final String name;

    @Getter
    private final Map<String, Boolean> permissions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> lookupCache = new HashMap<>();

    public void invalidateCache() {
        synchronized (lookupCache) {
            lookupCache.clear();
        }
    }

    public boolean getPermissionValue(String permission) {
        if (plugin.getConfiguration().getDebugPermissionChecks()) {
            plugin.getLog().info("Checking if " + name + " has permission: " + permission);
        }

        permission = permission.toLowerCase();
        synchronized (lookupCache) {
            if (lookupCache.containsKey(permission)) {
                return lookupCache.get(permission);
            } else {
                boolean t = lookupPermissionValue(permission);
                lookupCache.put(permission, t);
                return t;
            }
        }
    }

    private boolean lookupPermissionValue(String permission) {
        if (permissions.containsKey(permission)) {
            return permissions.get(permission);
        }

        if (plugin.getConfiguration().getApplyWildcards()) {
            if (permissions.containsKey("*")) {
                return permissions.get("*");
            }
            if (permissions.containsKey("'*'")) {
                return permissions.get("'*'");
            }

            String node = "";
            Iterable<String> permParts = Splitter.on('.').split(permission);
            for (String s : permParts) {
                if (node.equals("")) {
                    node = s;
                } else {
                    node = node + "." + s;
                }

                if (permissions.containsKey(node + ".*")) {
                    return permissions.get(node + ".*");
                }
            }
        }

        return false;
    }
}
