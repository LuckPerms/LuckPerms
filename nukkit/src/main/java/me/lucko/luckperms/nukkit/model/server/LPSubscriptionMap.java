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

import com.google.common.collect.Sets;

import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import cn.nukkit.permission.Permissible;
import cn.nukkit.plugin.PluginManager;

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
public final class LPSubscriptionMap extends HashMap<String, Set<Permissible>> {

    // the plugin instance
    final LPNukkitPlugin plugin;

    public LPSubscriptionMap(LPNukkitPlugin plugin, Map<String, Set<Permissible>> existingData) {
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
    public Set<Permissible> get(Object key) {
        if (key == null || !(key instanceof String)) {
            return null;
        }

        String permission = ((String) key);

        Set<Permissible> result = super.get(key);

        if (result == null) {
            // calculate a new map - always!
            result = new LPSubscriptionValueSet(permission);
            super.put(permission, result);
        } else if (!(result instanceof LPSubscriptionValueSet)) {
            // ensure return type is a LPSubscriptionMap
            result = new LPSubscriptionValueSet(permission, result);
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
        Map<String, Set<Permissible>> ret = new HashMap<>();

        for (Map.Entry<String, Set<Permissible>> ent : entrySet()) {
            if (ent.getValue() instanceof LPSubscriptionValueSet) {
                Set<Permissible> backing = ((LPSubscriptionValueSet) ent.getValue()).backing;
                Set<Permissible> copy; (copy = Collections.newSetFromMap(new WeakHashMap<>(backing.size()))).addAll(backing);
                ret.put(ent.getKey(), copy);
            } else {
                ret.put(ent.getKey(), ent.getValue());
            }
        }

        return ret;
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
            this.backing = Collections.newSetFromMap(new WeakHashMap<>());
            
            if (content != null) {
                this.backing.addAll(content);
            }
        }

        private LPSubscriptionValueSet(String permission) {
            this(permission, null);
        }

        private Sets.SetView<Permissible> getContentView() {
            // gather players (LPPermissibles)
            Set<Permissible> players = LPSubscriptionMap.this.plugin.getServer().getOnlinePlayers().values().stream()
                    .filter(player -> player.isPermissionSet(this.permission))
                    .collect(Collectors.toSet());

            return Sets.union(players, this.backing);
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
                return p.isPermissionSet(this.permission);
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
        public <T> T[] toArray(T[] a) {
            return getContentView().toArray(a);
        }

        @Override
        public boolean add(Permissible permissible) {
            return this.backing.add(permissible);
        }

        @Override
        public boolean addAll(Collection<? extends Permissible> c) {
            return this.backing.addAll(c);
        }

        @Override
        public boolean remove(Object o) {
            return this.backing.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return getContentView().containsAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return this.backing.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return this.backing.removeAll(c);
        }

        @Override
        public void clear() {
            this.backing.clear();
        }
    }
}
