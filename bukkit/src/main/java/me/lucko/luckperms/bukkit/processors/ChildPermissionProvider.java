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
import com.google.common.collect.Maps;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds child permissions registered on the platform.
 *
 * The data stored in this class is pulled from the data in {@link PluginManager#getPermissions()}.
 *
 * The former method is not thread safe, so we populate this class when the server starts to get all of the data
 * in a form which is easily queryable & thread safe.
 *
 * The data is resolved early, so the represented child permissions are a "deep" lookup of permissions.
 */
public class ChildPermissionProvider {

    // in the format:  permission+value  ===>  children (a map of child permissions)
    @Getter
    private ImmutableMap<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> permissions = ImmutableMap.of();

    public void setup() {
        ImmutableMap.Builder<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> permissions = ImmutableMap.builder();

        // iterate all permissions registered on the platform & resolve.
        for (Permission permission : Bukkit.getServer().getPluginManager().getPermissions()) {
            resolve(permissions, permission, true);
            resolve(permissions, permission, false);
        }

        this.permissions = permissions.build();
    }

    private static void resolve(ImmutableMap.Builder<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> accumulator, Permission permission, boolean value) {

        // accumulator for the child permissions being looked up
        Map<String, Boolean> children = new HashMap<>();

        // resolve children for the permission, so pass a map containing just the permission being looked up.
        resolveChildren(children, Collections.singletonMap(permission.getName(), value), false);

        // remove self
        children.remove(permission.getName(), value);

        // only register the children if there are any.
        if (!children.isEmpty()) {
            accumulator.put(Maps.immutableEntry(permission.getName().toLowerCase(), value), ImmutableMap.copyOf(children));
        }
    }

    private static void resolveChildren(Map<String, Boolean> accumulator, Map<String, Boolean> children, boolean invert) {
        // iterate through the current known children.
        // the first time this method is called for a given permission, the children map will contain only the permission itself.
        for (Map.Entry<String, Boolean> e : children.entrySet()) {
            if (accumulator.containsKey(e.getKey())) {
                continue; // Prevent infinite loops
            }

            // xor the value using the parent (bukkit logic, not mine)
            boolean value = e.getValue() ^ invert;
            accumulator.put(e.getKey().toLowerCase(), value);

            // lookup any deeper children & resolve if present
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(e.getKey());
            if (perm != null) {
                resolveChildren(accumulator, perm.getChildren(), !value);
            }
        }
    }
}
