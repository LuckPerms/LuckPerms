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

package me.lucko.luckperms.bukkit.model.server;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;

import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

/**
 * A replacement map for the 'defaultPerms' instance in Bukkit's SimplePluginManager.
 *
 * This instance allows LuckPerms to intercept calls to
 * {@link PluginManager#addPermission(Permission)}, specifically regarding
 * the default nature of the permission.
 *
 * Injected by {@link InjectorDefaultsMap}.
 */
public final class LPDefaultsMap implements Map<Boolean, Set<Permission>> {
    // keyset for all instances
    private static final Set<Boolean> KEY_SET = ImmutableSet.of(Boolean.TRUE, Boolean.FALSE);

    // the plugin
    final LPBukkitPlugin plugin;

    // the two values in the map
    private final Set<Permission> opSet = new DefaultPermissionSet(true);
    private final Set<Permission> nonOpSet = new DefaultPermissionSet(false);

    // fully resolved defaults (accounts for child permissions too)
    private Map<String, Boolean> resolvedOpDefaults = ImmutableMap.of();
    private Map<String, Boolean> resolvedNonOpDefaults = ImmutableMap.of();

    // #values and #entrySet results - both immutable
    private final Collection<Set<Permission>> values = ImmutableList.of(this.opSet, this.nonOpSet);
    private final Set<Entry<Boolean, Set<Permission>>> entrySet = ImmutableSet.of(
            Maps.immutableEntry(Boolean.TRUE, this.opSet),
            Maps.immutableEntry(Boolean.FALSE, this.nonOpSet)
    );

    public LPDefaultsMap(LPBukkitPlugin plugin, Map<Boolean, Set<Permission>> existingData) {
        this.plugin = plugin;
        this.opSet.addAll(existingData.getOrDefault(Boolean.TRUE, Collections.emptySet()));
        this.nonOpSet.addAll(existingData.getOrDefault(Boolean.FALSE, Collections.emptySet()));
        refreshOp();
        refreshNonOp();
    }

    public Set<Permission> getOpPermissions() {
        return this.opSet;
    }

    public Set<Permission> getNonOpPermissions() {
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
        for (Permission perm : getOpPermissions()) {
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
        for (Permission perm : getNonOpPermissions()) {
            String name = perm.getName().toLowerCase();
            builder.put(name, true);
            for (Map.Entry<String, Boolean> child : this.plugin.getPermissionMap().getChildPermissions(name, true).entrySet()) {
                builder.putIfAbsent(child.getKey(), child.getValue());
            }
        }
        this.resolvedNonOpDefaults = ImmutableMap.copyOf(builder);
    }

    @Override
    public Set<Permission> get(Object key) {
        boolean b = (boolean) key;
        return b ? this.opSet : this.nonOpSet;
    }

    // return wrappers around this map impl
    @Nonnull @Override public Collection<Set<Permission>> values() { return this.values; }
    @Nonnull @Override public Set<Entry<Boolean, Set<Permission>>> entrySet() { return this.entrySet; }
    @Nonnull @Override public Set<Boolean> keySet() { return KEY_SET; }

    // return accurate results for the Map spec
    @Override public int size() { return 2; }
    @Override public boolean isEmpty() { return false; }
    @Override public boolean containsKey(Object key) { return key instanceof Boolean; }
    @Override public boolean containsValue(Object value) { return value == this.opSet || value == this.nonOpSet; }

    // throw unsupported operation exceptions
    @Override public Set<Permission> put(Boolean key, Set<Permission> value) { throw new UnsupportedOperationException(); }
    @Override public Set<Permission> remove(Object key) { throw new UnsupportedOperationException(); }
    @Override public void putAll(@Nonnull Map<? extends Boolean, ? extends Set<Permission>> m) { throw new UnsupportedOperationException(); }
    @Override public void clear() { throw new UnsupportedOperationException(); }

    private final class DefaultPermissionSet extends ForwardingSet<Permission> {
        private final Set<Permission> delegate = ConcurrentHashMap.newKeySet();
        private final boolean op;

        private DefaultPermissionSet(boolean op) {
            this.op = op;
        }

        @Override
        protected Set<Permission> delegate() {
            return this.delegate;
        }

        @Override
        public boolean add(@Nonnull Permission element) {
            boolean ret = super.add(element);
            refresh(this.op);
            return ret;
        }

        @Override
        public boolean addAll(@Nonnull Collection<? extends Permission> collection) {
            boolean ret = super.addAll(collection);
            refresh(this.op);
            return ret;
        }
    }

}
