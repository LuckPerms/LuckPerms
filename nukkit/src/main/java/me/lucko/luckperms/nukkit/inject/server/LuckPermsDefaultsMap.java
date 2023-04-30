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

package me.lucko.luckperms.nukkit.inject.server;

import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginManager;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.common.cache.Cache;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import net.luckperms.api.util.Tristate;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A replacement map for the 'defaultPerms' instance in Nukkit's SimplePluginManager.
 *
 * This instance allows LuckPerms to intercept calls to
 * {@link PluginManager#addPermission(Permission)}, specifically regarding
 * the default nature of the permission.
 *
 * Injected by {@link InjectorDefaultsMap}.
 */
public final class LuckPermsDefaultsMap {

    // the plugin
    final LPNukkitPlugin plugin;

    // the two values in the map
    private final Map<String, Permission> opSet = new DefaultPermissionSet(true);
    private final Map<String, Permission> nonOpSet = new DefaultPermissionSet(false);

    // fully resolved defaults (accounts for child permissions too)
    private final DefaultsCache opCache = new DefaultsCache(true);
    private final DefaultsCache nonOpCache = new DefaultsCache(false);

    public LuckPermsDefaultsMap(LPNukkitPlugin plugin, Map<Boolean, Map<String, Permission>> existingData) {
        this.plugin = plugin;
        this.opSet.putAll(existingData.getOrDefault(Boolean.TRUE, Collections.emptyMap()));
        this.nonOpSet.putAll(existingData.getOrDefault(Boolean.FALSE, Collections.emptyMap()));
    }

    public Map<String, Permission> getOpPermissions() {
        return this.opSet;
    }

    public Map<String, Permission> getNonOpPermissions() {
        return this.nonOpSet;
    }

    public Map<String, Permission> get(boolean op) {
        return op ? this.opSet : this.nonOpSet;
    }

    private DefaultsCache getCache(boolean op) {
        return op ? this.opCache : this.nonOpCache;
    }

    private void invalidate(boolean op) {
        getCache(op).invalidate();
        this.plugin.getUserManager().invalidateAllPermissionCalculators();
        this.plugin.getGroupManager().invalidateAllPermissionCalculators();
    }

    /**
     * Queries whether a given permission should be granted by default.
     *
     * @param permission the permission to query
     * @param isOp if the player is op
     * @return a tristate result
     */
    public Tristate lookupDefaultPermission(String permission, boolean isOp) {
        Map<String, Boolean> map = getCache(isOp).get();
        return Tristate.of(map.get(permission));
    }

    final class DefaultPermissionSet extends ForwardingMap<String, Permission> {
        final LuckPermsDefaultsMap parent = LuckPermsDefaultsMap.this;

        private final Map<String, Permission> delegate = new ConcurrentHashMap<>();
        private final boolean op;

        DefaultPermissionSet(boolean op) {
            this.op = op;
        }

        @Override
        protected Map<String, Permission> delegate() {
            return this.delegate;
        }

        @Override
        public Permission put(@NonNull String key, @NonNull Permission value) {
            Permission ret = super.put(key, value);
            invalidate(this.op);
            return ret;
        }

        @Override
        public Permission putIfAbsent(String key, Permission value) {
            Permission ret = super.putIfAbsent(key, value);
            invalidate(this.op);
            return ret;
        }

        @Override
        public void putAll(@NonNull Map<? extends String, ? extends Permission> map) {
            super.putAll(map);
            invalidate(this.op);
        }

        @Override
        public Permission remove(@NonNull Object object) {
            Permission ret = super.remove(object);
            invalidate(this.op);
            return ret;
        }
    }

    private final class DefaultsCache extends Cache<Map<String, Boolean>> {
        private final boolean op;

        DefaultsCache(boolean op) {
            this.op = op;
        }

        @Override
        protected @NonNull Map<String, Boolean> supply() {
            Map<String, Boolean> builder = new HashMap<>();
            for (Permission perm : LuckPermsDefaultsMap.this.get(this.op).values()) {
                String name = perm.getName().toLowerCase(Locale.ROOT);
                builder.put(name, true);
                for (Map.Entry<String, Boolean> child : LuckPermsDefaultsMap.this.plugin.getPermissionMap().getChildPermissions(name, true).entrySet()) {
                    builder.putIfAbsent(child.getKey(), child.getValue());
                }
            }
            return ImmutableMap.copyOf(builder);
        }
    }

}
