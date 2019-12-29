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

package me.lucko.luckperms.bukkit.inject.server;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.util.ImmutableCollectors;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * A replacement map for the 'permSubs' instance in Bukkit's SimplePluginManager.
 *
 * This instance allows LuckPerms to intercept calls to
 * {@link PluginManager#subscribeToPermission(String, Permissible)},
 * {@link PluginManager#unsubscribeFromPermission(String, Permissible)} and
 * {@link PluginManager#getPermissionSubscriptions(String)}.
 *
 * Bukkit for some reason sometimes uses subscription status to determine whether
 * a permissible has a given node, instead of checking directly with
 * {@link Permissible#hasPermission(String)}.
 *
 * {@link org.bukkit.Server#broadcast(String, String)} is a good example of this.
 *
 * In order to implement predicable Bukkit behaviour, LP has two options:
 * 1) register subscriptions for all players as normal, or
 * 2) inject it's own map instance to proxy calls to {@link PluginManager#getPermissionSubscriptions(String)} back to LuckPerms.
 *
 * This class implements option 2 above. It is preferred because it is faster & uses less memory
 *
 * Injected by {@link InjectorSubscriptionMap}.
 */
public final class LuckPermsSubscriptionMap extends HashMap<String, Map<Permissible, Boolean>> {

    // the plugin instance
    final LPBukkitPlugin plugin;

    public LuckPermsSubscriptionMap(LPBukkitPlugin plugin, Map<String, Map<Permissible, Boolean>> existingData) {
        super(existingData);
        this.plugin = plugin;
    }

    /*
     * The get method is the only one which is actually used by SimplePluginManager
     * we override it to always return a value - which means the null check in
     * subscribeToDefaultPerms always fails - soo, we don't have to worry too much
     * about implementing #put.
     *
     * we also ensure all returns are LPSubscriptionValueMaps. this extension
     * will also delegate checks to online players - meaning we don't ever
     * have to register their subscriptions with the plugin manager.
     */
    @Override
    public Map<Permissible, Boolean> get(Object key) {
        if (key == null || !(key instanceof String)) {
            return null;
        }

        String permission = ((String) key);

        Map<Permissible, Boolean> result = super.get(key);

        if (result == null) {
            // calculate a new map - always!
            result = new LPSubscriptionValueMap(permission);
            super.put(permission, result);
        } else if (!(result instanceof LPSubscriptionValueMap)) {
            // ensure return type is a LPSubscriptionMap
            result = new LPSubscriptionValueMap(permission, result);
            super.put(permission, result);
        }
        return result;
    }

    @Override
    public Map<Permissible, Boolean> put(String key, Map<Permissible, Boolean> value) {
        if (value == null) {
            throw new NullPointerException("Map value cannot be null");
        }

        // ensure values are LP subscription maps
        if (!(value instanceof LPSubscriptionValueMap)) {
            value = new LPSubscriptionValueMap(key, value);
        }
        return super.put(key, value);
    }

    // if the key isn't null and is a string, #get will always return a value for it
    @Override
    public boolean containsKey(Object key) {
        return key != null && key instanceof String;
    }

    /**
     * Converts this map back to a standard HashMap
     *
     * @return a standard representation of this map
     */
    public Map<String, Map<Permissible, Boolean>> detach() {
        Map<String, Map<Permissible, Boolean>> map = new HashMap<>();

        for (Map.Entry<String, Map<Permissible, Boolean>> ent : entrySet()) {
            if (ent.getValue() instanceof LPSubscriptionValueMap) {
                map.put(ent.getKey(), ((LPSubscriptionValueMap) ent.getValue()).backing);
            } else {
                map.put(ent.getKey(), ent.getValue());
            }
        }

        return map;
    }

    /**
     * Value map extension which includes LP objects in Permissible related queries.
     */
    public final class LPSubscriptionValueMap implements Map<Permissible, Boolean> {

        // the permission being mapped to this value map
        private final String permission;

        // the backing map
        private final Map<Permissible, Boolean> backing;

        private LPSubscriptionValueMap(String permission, Map<Permissible, Boolean> backing) {
            this.permission = permission;
            this.backing = new WeakHashMap<>(backing);

            // remove all players from the map
            this.backing.keySet().removeIf(p -> p instanceof Player);
        }

        public LPSubscriptionValueMap(String permission) {
            this.permission = permission;
            this.backing = new WeakHashMap<>();
        }

        @Override
        public Boolean get(Object key) {
            boolean isPlayer = key instanceof Player;

            // if the key is a player, check their LPPermissible first
            if (isPlayer) {
                Permissible p = (Permissible) key;
                if (p.hasPermission(this.permission)) {
                    return true;
                }
            }

            // then try the map
            Boolean result = this.backing.get(key);
            if (result != null) {
                return result;
            }

            // then try the permissible, if we haven't already
            if (!isPlayer && key instanceof Permissible) {
                Permissible p = (Permissible) key;
                if (p.hasPermission(this.permission)) {
                    return true;
                }
            }

            // no result
            return null;
        }

        @Override
        public Boolean put(Permissible key, Boolean value) {
            // don't allow players to be put into this map
            if (key instanceof Player) {
                return true;
            }

            return this.backing.put(key, value);
        }

        @Override
        public boolean containsKey(Object key) {
            // delegate through the get method
            return get(key) != null;
        }

        @Override
        public @NonNull Set<Permissible> keySet() {
            // gather players (LPPermissibles)
            Set<Permissible> players = LuckPermsSubscriptionMap.this.plugin.getBootstrap().getServer().getOnlinePlayers().stream()
                    .filter(player -> player.hasPermission(this.permission) || player.isPermissionSet(this.permission))
                    .collect(Collectors.toSet());

            // then combine the players with the backing map
            return Sets.union(players, this.backing.keySet());
        }

        @Override
        public @NonNull Set<Entry<Permissible, Boolean>> entrySet() {
            return keySet().stream()
                    .map(key -> {
                        Boolean value = get(key);
                        return value != null ? Maps.immutableEntry(key, value) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(ImmutableCollectors.toSet());
        }

        @Override
        public boolean isEmpty() {
            // we never want to remove this map from the parent - since it just gets recreated
            // on subsequent calls
            return false;
        }

        @Override
        public int size() {
            return Math.max(1, this.backing.size());
        }

        // just delegate to the backing map

        @Override
        public Boolean remove(Object key) {
            return this.backing.remove(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return this.backing.containsValue(value);
        }

        @Override
        public void putAll(@NonNull Map<? extends Permissible, ? extends Boolean> m) {
            this.backing.putAll(m);
        }

        @Override
        public void clear() {
            this.backing.clear();
        }

        @Override
        public @NonNull Collection<Boolean> values() {
            return this.backing.values();
        }
    }
}
