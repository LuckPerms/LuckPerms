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

package me.lucko.luckperms.calculators;

import lombok.AllArgsConstructor;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.inject.DummyPermissibleBase;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultsProvider {

    private final DummyPermissible opDummy = new DummyPermissible(this::refreshOp);
    private final DummyPermissible nonOpDummy = new DummyPermissible(this::refreshNonOp);

    private final Map<String, Boolean> op = new HashMap<>();
    private final Map<String, Boolean> nonOp = new HashMap<>();

    public void refresh() {
        refreshOp();
        refreshNonOp();
    }

    private void refreshOp() {
        calculateDefaults(op, opDummy, true);
    }

    private void refreshNonOp() {
        calculateDefaults(nonOp, nonOpDummy, false);
    }

    private static void calculateDefaults(Map<String, Boolean> map, DummyPermissible p, boolean op) {
        Set<String> perms = map.keySet();

        for (String name : perms) {
            Bukkit.getServer().getPluginManager().unsubscribeFromPermission(name, p);
        }

        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(false, p);
        Bukkit.getServer().getPluginManager().unsubscribeFromDefaultPerms(true, p);

        map.clear();

        Set<Permission> defaults = Bukkit.getServer().getPluginManager().getDefaultPermissions(op);
        Bukkit.getServer().getPluginManager().subscribeToDefaultPerms(op, p);

        for (Permission perm : defaults) {
            String name = perm.getName().toLowerCase();
            map.put(name, true);
            Bukkit.getServer().getPluginManager().subscribeToPermission(name, p);
            calculateChildPermissions(map, p, perm.getChildren(), false);
        }
    }

    private static void calculateChildPermissions(Map<String, Boolean> map, DummyPermissible p, Map<String, Boolean> children, boolean invert) {
        for (Map.Entry<String, Boolean> e : children.entrySet()) {
            Permission perm = Bukkit.getServer().getPluginManager().getPermission(e.getKey());
            boolean value = e.getValue() ^ invert;
            String lName = e.getKey().toLowerCase();

            map.put(lName, value);
            Bukkit.getServer().getPluginManager().subscribeToPermission(e.getKey(), p);

            if (perm != null) {
                calculateChildPermissions(map, p, perm.getChildren(), !value);
            }
        }
    }

    public Tristate hasDefault(String permission, boolean isOp) {
        Map<String, Boolean> map = isOp ? op : nonOp;
        if (!map.containsKey(permission)) {
            return Tristate.UNDEFINED;
        }

        return Tristate.fromBoolean(map.get(permission));
    }

    @AllArgsConstructor
    private static class DummyPermissible extends DummyPermissibleBase {
        private final Runnable onRefresh;

        @Override
        public void recalculatePermissions() {
            onRefresh.run();
        }
    }

}
