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

import cn.nukkit.Player;
import cn.nukkit.permission.Permissible;
import cn.nukkit.plugin.PluginManager;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * A replacement map for the 'permSubs' instance in Nukkit's SimplePluginManager.
 *
 * This instance allows LuckPerms to intercept calls to
 * {@link PluginManager#subscribeToPermission(String, Permissible)},
 * {@link PluginManager#unsubscribeFromPermission(String, Permissible)} and
 * {@link PluginManager#getPermissionSubscriptions(String)}.
 *
 * Nukkit for some reason sometimes uses subscription status to determine whether
 * a permissible has a given node, instead of checking directly with
 * {@link Permissible#hasPermission(String)}.
 *
 * In order to implement predicable Nukkit behaviour, LP has two options:
 * 1) register subscriptions for all players as normal, or
 * 2) inject it's own map instance to proxy calls to {@link PluginManager#getPermissionSubscriptions(String)} back to LuckPerms.
 *
 * This class implements option 2 above. It is preferred because it is faster & uses less memory
 *
 * Injected by {@link InjectorSubscriptionMap}.
 */
public final class LuckPermsSubscriptionMap extends HashMap<String, Set<Permissible>> {

    // the plugin instance
    final LPNukkitPlugin plugin;

    public LuckPermsSubscriptionMap(LPNukkitPlugin plugin, Map<String, Set<Permissible>> existingData) {
        this.plugin = plugin;
        for (Entry<String, Set<Permissible>> entry : existingData.entrySet()) {
            super.put(entry.getKey(), new LPSubscriptionValueSet(entry.getKey(), entry.getValue()));
        }
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
    public Set<Permissible> get(Object key) {
        if (key == null || !(key instanceof String)) {
            return null;
        }

        String permission = (String) key;

        LPSubscriptionValueSet result = (LPSubscriptionValueSet) super.get(key);
        if (result == null) {
            // calculate a new map - always!
            result = new LPSubscriptionValueSet(permission);
            super.put(permission, result);
        }

        return result;
    }

    @Override
    public Set<Permissible> put(String key, Set<Permissible> value) {
        if (value == null) {
            throw new NullPointerException("Map value cannot be null");
        }

        // ensure values are LP subscription maps
        if (!(value instanceof LPSubscriptionValueSet)) {
            value = new LPSubscriptionValueSet(key, value);
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
    public Map<String, Set<Permissible>> detach() {
        Map<String, Set<Permissible>> map = new HashMap<>();
        for (Map.Entry<String, Set<Permissible>> ent : entrySet()) {
            Set<Permissible> backing = ((LPSubscriptionValueSet) ent.getValue()).backing;
            Set<Permissible> copy = Collections.newSetFromMap(new WeakHashMap<>(backing.size()));
            copy.addAll(backing);
            map.put(ent.getKey(), copy);
        }
        return map;
    }

    /**
     * Value map extension which includes LP objects in Permissible related queries.
     */
    public final class LPSubscriptionValueSet implements Set<Permissible> {

        // the permission being mapped to this value map
        private final String permission;

        // the backing map
        private final Set<Permissible> backing;

        private LPSubscriptionValueSet(String permission, Set<Permissible> content) {
            this.permission = permission;
            this.backing = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
            
            if (content != null) {
                this.backing.addAll(content);
            }

            // remove all players from the map
            this.backing.removeIf(p -> p instanceof Player);
        }

        private LPSubscriptionValueSet(String permission) {
            this(permission, null);
        }

        private Set<Permissible> getContentView() {
            // gather players (LPPermissibles)
            Set<Permissible> players = LuckPermsSubscriptionMap.this.plugin.getBootstrap().getServer().getOnlinePlayers().values().stream()
                    .filter(player -> player.hasPermission(this.permission) || player.isPermissionSet(this.permission))
                    .collect(Collectors.toSet());

            ImmutableSet<Permissible> backing;
            synchronized (this.backing) {
                backing = ImmutableSet.copyOf(this.backing);
            }

            return Sets.union(players, backing);
        }

        @Override
        public boolean contains(Object key) {
            // try the backing map
            if (this.backing.contains(key)) {
                return true;
            }

            // then try the permissible
            if (key instanceof Permissible) {
                Permissible p = (Permissible) key;
                return p.hasPermission(this.permission);
            }

            // no result
            return false;
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

        @Override
        public Iterator<Permissible> iterator() {
            return getContentView().iterator();
        }

        @Override
        public Object[] toArray() {
            return getContentView().toArray();
        }

        @Override
        public <T> T[] toArray(@NonNull T[] a) {
            return getContentView().toArray(a);
        }

        @Override
        public boolean add(Permissible permissible) {
            // don't allow players to be put into this map
            if (permissible instanceof Player) {
                return true;
            }

            return this.backing.add(permissible);
        }

        @Override
        public boolean addAll(@NonNull Collection<? extends Permissible> c) {
            return this.backing.addAll(c);
        }

        @Override
        public boolean remove(Object o) {
            return this.backing.remove(o);
        }

        @Override
        public boolean containsAll(@NonNull Collection<?> c) {
            return getContentView().containsAll(c);
        }

        @Override
        public boolean retainAll(@NonNull Collection<?> c) {
            return this.backing.retainAll(c);
        }

        @Override
        public boolean removeAll(@NonNull Collection<?> c) {
            return this.backing.removeAll(c);
        }

        @Override
        public void clear() {
            this.backing.clear();
        }
    }
}
