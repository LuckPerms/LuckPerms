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

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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
public final class LuckPermsSubscriptionMap implements Map<String, Map<Permissible, Boolean>> {

    // the plugin instance
    final LPBukkitPlugin plugin;

    private final Map<Permissible, Set<String>> subscriptions = Collections.synchronizedMap(new WeakHashMap<>());

    public LuckPermsSubscriptionMap(LPBukkitPlugin plugin, Map<String, Map<Permissible, Boolean>> existingData) {
        this.plugin = plugin;
        for (Entry<String, Map<Permissible, Boolean>> entry : existingData.entrySet()) {
            entry.getValue().keySet().forEach(permissible -> subscribe(permissible, entry.getKey()));
        }
    }

    /*
     * The get method is the only one which is actually used by SimplePluginManager
     * we override it to always return a value - which means the null check in
     * subscribeToDefaultPerms always fails - soo, we don't have to worry too much
     * about implementing #put.
     */
    @Override
    public Map<Permissible, Boolean> get(Object key) {
        return new ValueMap((String) key);
    }

    public void subscribe(Permissible permissible, String permission) {
        // don't allow players to be put into this map
        if (permissible instanceof Player) {
            return;
        }

        Set<String> perms = this.subscriptions.computeIfAbsent(permissible, x -> Collections.synchronizedSet(new HashSet<>()));
        perms.add(permission);
    }

    public boolean unsubscribe(Permissible permissible, String permission) {
        if (permissible instanceof Player) {
            return false; // ignore calls for players
        }

        Set<String> perms = this.subscriptions.get(permissible);

        if (perms == null) {
            return false;
        }

        return perms.remove(permission);
    }

    public @NonNull Set<Permissible> subscribers(String permission) {
        Collection<? extends Player> onlinePlayers = this.plugin.getBootstrap().getServer().getOnlinePlayers();
        Set<Permissible> set = new HashSet<>(onlinePlayers.size() + this.subscriptions.size());

        // add permissibles from the subscriptions map
        this.subscriptions.forEach((permissible, perms) -> {
            if (perms.contains(permission)) {
                set.add(permissible);
            }
        });

        // add any online players who meet requirements
        for (Player player : onlinePlayers) {
            if (player.hasPermission(permission) || player.isPermissionSet(permission)) {
                set.add(player);
            }
        }

        return set;
    }

    /**
     * Converts this map back to a standard HashMap
     *
     * @return a standard representation of this map
     */
    public Map<String, Map<Permissible, Boolean>> detach() {
        Map<String, Map<Permissible, Boolean>> map = new HashMap<>();
        this.subscriptions.forEach((permissible, perms) -> {
            for (String perm : perms) {
                map.computeIfAbsent(perm, x -> new WeakHashMap<>()).put(permissible, true);
            }
        });
        return map;
    }

    @Override public Map<Permissible, Boolean> put(String key, Map<Permissible, Boolean> value) { throw new UnsupportedOperationException(); }
    @Override public Map<Permissible, Boolean> remove(Object key) { throw new UnsupportedOperationException(); }
    @Override public void putAll(Map<? extends String, ? extends Map<Permissible, Boolean>> m) { throw new UnsupportedOperationException(); }
    @Override public void clear() { throw new UnsupportedOperationException(); }
    @Override public Set<String> keySet() { throw new UnsupportedOperationException(); }
    @Override public Collection<Map<Permissible, Boolean>> values() { throw new UnsupportedOperationException(); }
    @Override public Set<Entry<String, Map<Permissible, Boolean>>> entrySet() { throw new UnsupportedOperationException(); }
    @Override public int size() { return 0; }
    @Override public boolean isEmpty() { throw new UnsupportedOperationException(); }
    @Override public boolean containsKey(Object key) { throw new UnsupportedOperationException(); }
    @Override public boolean containsValue(Object value) { throw new UnsupportedOperationException(); }

    /**
     * Value map extension which includes LP objects in Permissible related queries.
     */
    public final class ValueMap implements Map<Permissible, Boolean> {

        // the permission being mapped to this value map
        private final String permission;

        public ValueMap(String permission) {
            this.permission = permission;
        }

        @Override
        public Boolean put(Permissible key, Boolean value) {
            subscribe(key, this.permission);
            return null;
        }

        @Override
        public Boolean remove(Object k) {
            Permissible key = (Permissible) k;
            return unsubscribe(key, this.permission) ? true : null;
        }

        @Override
        public @NonNull Set<Permissible> keySet() {
            return subscribers(this.permission);
        }

        @Override
        public boolean isEmpty() {
            // we never want to remove this map from the parent - since it just gets recreated
            // on subsequent calls
            return false;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override public void putAll(Map<? extends Permissible, ? extends Boolean> m) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public Collection<Boolean> values() { throw new UnsupportedOperationException(); }
        @Override public Set<Entry<Permissible, Boolean>> entrySet() { throw new UnsupportedOperationException(); }
        @Override public boolean containsKey(Object key) { throw new UnsupportedOperationException(); }
        @Override public boolean containsValue(Object value) { throw new UnsupportedOperationException(); }
        @Override public Boolean get(Object key) { throw new UnsupportedOperationException(); }
    }
}
