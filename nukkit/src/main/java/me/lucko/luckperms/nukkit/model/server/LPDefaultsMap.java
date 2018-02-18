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

package me.lucko.luckperms.nukkit.model.server;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginManager;

import java.util.Collections;
import java.util.HashMap;
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
public final class LPDefaultsMap {

    // the plugin
    final LPNukkitPlugin plugin;

    // the two values in the map
    private final Map<String, Permission> opSet = new DefaultPermissionSet(true);
    private final Map<String, Permission> nonOpSet = new DefaultPermissionSet(false);

    // fully resolved defaults (accounts for child permissions too)
    private Map<String, Boolean> resolvedOpDefaults = ImmutableMap.of();
    private Map<String, Boolean> resolvedNonOpDefaults = ImmutableMap.of();

    public LPDefaultsMap(LPNukkitPlugin plugin, Map<Boolean, Map<String, Permission>> existingData) {
        this.plugin = plugin;
        this.opSet.putAll(existingData.getOrDefault(Boolean.TRUE, Collections.emptyMap()));
        this.nonOpSet.putAll(existingData.getOrDefault(Boolean.FALSE, Collections.emptyMap()));
        refreshOp();
        refreshNonOp();
    }

    public Map<String, Permission> getOpPermissions() {
        return this.opSet;
    }

    public Map<String, Permission> getNonOpPermissions() {
        return this.nonOpSet;
    }

    /**
     * Queries whether a given permission should be granted by default.
     *
     * @param permission the permission to query
     * @param isOp if the player is op
     * @return a tristate result
     */
    public Tristate lookupDefaultPermission(String permission, boolean isOp) {
        Map<String, Boolean> map = isOp ? this.resolvedOpDefaults : this.resolvedNonOpDefaults;
        return Tristate.fromNullableBoolean(map.get(permission));
    }

    private void refresh(boolean op) {
        if (op) {
            refreshOp();
        } else {
            refreshNonOp();
        }
    }

    /**
     * Refreshes the op data in this provider.
     */
    private void refreshOp() {
        Map<String, Boolean> builder = new HashMap<>();
        for (Permission perm : getOpPermissions().values()) {
            String name = perm.getName().toLowerCase();
            builder.put(name, true);
            for (Map.Entry<String, Boolean> child : this.plugin.getPermissionMap().getChildPermissions(name, true).entrySet()) {
                builder.putIfAbsent(child.getKey(), child.getValue());
            }
        }
        this.resolvedOpDefaults = ImmutableMap.copyOf(builder);
    }

    /**
     * Refreshes the non op data in this provider.
     */
    private void refreshNonOp() {
        Map<String, Boolean> builder = new HashMap<>();
        for (Permission perm : getNonOpPermissions().values()) {
            String name = perm.getName().toLowerCase();
            builder.put(name, true);
            for (Map.Entry<String, Boolean> child : this.plugin.getPermissionMap().getChildPermissions(name, true).entrySet()) {
                builder.putIfAbsent(child.getKey(), child.getValue());
            }
        }
        this.resolvedNonOpDefaults = ImmutableMap.copyOf(builder);
    }

    final class DefaultPermissionSet extends ForwardingMap<String, Permission> {
        final LPDefaultsMap parent = LPDefaultsMap.this;

        private final Map<String, Permission> delegate = new ConcurrentHashMap<>();
        private final boolean op;

        private DefaultPermissionSet(boolean op) {
            this.op = op;
        }

        @Override
        protected Map<String, Permission> delegate() {
            return this.delegate;
        }

        @Override
        public Permission put(String key, Permission value) {
            Permission ret = super.put(key, value);
            refresh(this.op);
            return ret;
        }

        @Override
        public Permission putIfAbsent(String key, Permission value) {
            Permission ret = super.putIfAbsent(key, value);
            refresh(this.op);
            return ret;
        }

        @Override
        public void putAll(Map<? extends String, ? extends Permission> map) {
            super.putAll(map);
            refresh(this.op);
        }
    }

}
