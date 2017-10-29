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

package me.lucko.luckperms.bukkit.model;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.bukkit.permissions.Permission;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles permission subscriptions with Bukkits plugin manager, for a given LPPermissible.
 *
 * Bukkit for some reason sometimes uses subscription status to determine whether a permissible has a given node, instead
 * of checking directly with {@link org.bukkit.permissions.Permissible#hasPermission(Permission)}.
 *
 * {@link org.bukkit.Bukkit#broadcast(String, String)} is a good example of this.
 */
@RequiredArgsConstructor
public class SubscriptionManager {

    private final LPPermissible permissible;
    private Set<String> currentSubscriptions = ImmutableSet.of();

    public synchronized void subscribe(Set<String> perms) {
        Set<String> newPerms = ImmutableSet.copyOf(perms);

        // we compare changes to avoid unnecessary time wasted on the main thread mutating this data.
        // the changes can be calculated here async, and then only the needed changes can be applied.
        Map.Entry<Set<String>, Set<String>> changes = compareSets(newPerms, currentSubscriptions);
        if (!changes.getKey().isEmpty() || !changes.getValue().isEmpty()) {
            permissible.getPlugin().getScheduler().doSync(new SubscriptionUpdateTask(permissible, changes.getKey(), changes.getValue()));
        }

        this.currentSubscriptions = newPerms;
    }

    @AllArgsConstructor
    public static final class SubscriptionUpdateTask implements Runnable {
        private final LPPermissible permissible;
        private final Set<String> toAdd;
        private final Set<String> toRemove;

        @Override
        public void run() {
            for (String s : toAdd) {
                permissible.getPlugin().getServer().getPluginManager().subscribeToPermission(s, permissible.getPlayer());
            }
            for (String s : toRemove) {
                permissible.getPlugin().getServer().getPluginManager().unsubscribeFromPermission(s, permissible.getPlayer());
            }
        }
    }

    /**
     * Compares two sets
     * @param local the local set
     * @param remote the remote set
     * @return the entries to add to remote, and the entries to remove from remote
     */
    private static Map.Entry<Set<String>, Set<String>> compareSets(Set<String> local, Set<String> remote) {
        // entries in local but not remote need to be added
        // entries in remote but not local need to be removed

        Set<String> toAdd = new HashSet<>(local);
        toAdd.removeAll(remote);

        Set<String> toRemove = new HashSet<>(remote);
        toRemove.removeAll(local);

        return Maps.immutableEntry(toAdd, toRemove);
    }

}
