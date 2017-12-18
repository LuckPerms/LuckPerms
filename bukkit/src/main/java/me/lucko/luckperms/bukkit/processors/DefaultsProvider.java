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

package me.lucko.luckperms.bukkit.processors;

import lombok.Getter;

import com.google.common.collect.ImmutableMap;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.model.DummyPermissible;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Holds default permissions registered on the platform.
 *
 * The data stored in this class is pulled from the data in {@link PluginManager#getDefaultPermissions(boolean)}.
 *
 * The former method is not thread safe, so we populate this class when the server starts to get all of the data
 * in a form which is easily queryable & thread safe.
 *
 * The {@link DummyPermissible}s are registered with Bukkit, so we can listen for any
 * changes to default permissions.
 */
public class DefaultsProvider {

    // defaults for opped players
    @Getter
    private Map<String, Boolean> opDefaults = ImmutableMap.of();
    private final DummyPermissible opDummy = new DummyPermissible(this::refreshOp);

    // defaults for non-opped players
    @Getter
    private Map<String, Boolean> nonOpDefaults = ImmutableMap.of();
    private final DummyPermissible nonOpDummy = new DummyPermissible(this::refreshNonOp);

    /**
     * Refreshes the data in this provider.
     */
    public void refresh() {
        refreshOp();
        refreshNonOp();
    }

    /**
     * Queries whether a given permission should be granted by default.
     *
     * @param permission the permission to query
     * @param isOp if the player is op
     * @return a tristate result
     */
    public Tristate lookup(String permission, boolean isOp) {
        Map<String, Boolean> map = isOp ? opDefaults : nonOpDefaults;
        return Tristate.fromNullableBoolean(map.get(permission));
    }

    /**
     * Gets the number of default permissions held by the provider.
     *
     * @return the number of permissions held
     */
    public int size() {
        return opDefaults.size() + nonOpDefaults.size();
    }

    /**
     * Refreshes the op data in this provider.
     */
    private void refreshOp() {
        unregisterDefaults(opDefaults, opDummy, true);

        Map<String, Boolean> builder = new HashMap<>();
        calculateDefaults(builder, opDummy, true);

        opDefaults = ImmutableMap.copyOf(builder);
    }

    /**
     * Refreshes the non op data in this provider.
     */
    private void refreshNonOp() {
        unregisterDefaults(nonOpDefaults, nonOpDummy, false);

        Map<String, Boolean> builder = new HashMap<>();
        calculateDefaults(builder, nonOpDummy, false);

        nonOpDefaults = ImmutableMap.copyOf(builder);
    }

    /**
     * Unregisters the dummy permissibles with Bukkit.
     */
    public void close() {
        unregisterDefaults(opDefaults, opDummy, true);
        unregisterDefaults(nonOpDefaults, nonOpDummy, false);
    }

    private static PluginManager pm() {
        return Bukkit.getServer().getPluginManager();
    }

    /**
     * Unregisters defaults for a given permissible.
     *
     * @param map the map of current defaults
     * @param p the permissible
     */
    private static void unregisterDefaults(Map<String, Boolean> map, DummyPermissible p, boolean op) {
        Set<String> perms = map.keySet();

        for (String name : perms) {
            pm().unsubscribeFromPermission(name, p);
        }

        pm().unsubscribeFromDefaultPerms(op, p);
    }

    private static void calculateDefaults(Map<String, Boolean> map, DummyPermissible p, boolean op) {
        pm().subscribeToDefaultPerms(op, p);

        Set<Permission> defaults = pm().getDefaultPermissions(op);
        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase();

            map.put(name, true);
            pm().subscribeToPermission(name, p);

            // register defaults for any children too
            calculateChildPermissions(map, p, perm.getChildren(), false);
        }
    }

    private static void calculateChildPermissions(Map<String, Boolean> accumulator, DummyPermissible p, Map<String, Boolean> children, boolean invert) {
        for (Map.Entry<String, Boolean> e : children.entrySet()) {
            if (accumulator.containsKey(e.getKey())) {
                continue; // Prevent infinite loops
            }

            // xor the value using the parent (bukkit logic, not mine)
            boolean value = e.getValue() ^ invert;

            accumulator.put(e.getKey().toLowerCase(), value);
            pm().subscribeToPermission(e.getKey(), p);

            // lookup any deeper children & resolve if present
            Permission perm = pm().getPermission(e.getKey());
            if (perm != null) {
                calculateChildPermissions(accumulator, p, perm.getChildren(), !value);
            }
        }
    }

}
