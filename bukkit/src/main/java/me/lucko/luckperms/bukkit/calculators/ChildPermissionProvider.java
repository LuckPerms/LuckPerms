/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.bukkit.calculators;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChildPermissionProvider {

    @Getter
    private ImmutableMap<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> permissions = ImmutableMap.of();

    public void setup() {
        ImmutableMap.Builder<Map.Entry<String, Boolean>, ImmutableMap<String, Boolean>> permissions = ImmutableMap.builder();

        for (Permission permission : Bukkit.getServer().getPluginManager().getPermissions()) {
            // handle true
            Map<String, Boolean> children = new HashMap<>();
            resolveChildren(children, Collections.singletonMap(permission.getName(), true), false);
            permissions.put(Maps.immutableEntry(permission.getName().toLowerCase(), true), ImmutableMap.copyOf(children));

            // handle false
            Map<String, Boolean> nChildren = new HashMap<>();
            resolveChildren(nChildren, Collections.singletonMap(permission.getName(), false), false);
            permissions.put(Maps.immutableEntry(permission.getName().toLowerCase(), false), ImmutableMap.copyOf(nChildren));
        }

        this.permissions = permissions.build();
    }

    private static void resolveChildren(Map<String, Boolean> accumulator, Map<String, Boolean> children, boolean invert) {
        for (Map.Entry<String, Boolean> e : children.entrySet()) {
            if (accumulator.containsKey(e.getKey())) {
                continue; // Prevent infinite loops
            }

            Permission perm = Bukkit.getServer().getPluginManager().getPermission(e.getKey());
            boolean value = e.getValue() ^ invert;
            String lName = e.getKey().toLowerCase();

            accumulator.put(lName, value);

            if (perm != null) {
                resolveChildren(accumulator, perm.getChildren(), !value);
            }
        }
    }
}
