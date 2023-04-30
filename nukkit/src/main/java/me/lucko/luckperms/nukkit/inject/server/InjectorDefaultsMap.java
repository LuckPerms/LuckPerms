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

import cn.nukkit.Server;
import cn.nukkit.permission.Permission;
import cn.nukkit.plugin.PluginManager;
import com.google.common.collect.ImmutableMap;
import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Injects a {@link LuckPermsDefaultsMap} info the {@link PluginManager}.
 */
public class InjectorDefaultsMap implements Runnable {
    private static final Field OP_DEFAULT_PERMISSIONS_FIELD;
    private static final Field NON_OP_DEFAULT_PERMISSIONS_FIELD;

    static {
        Field opPermissionsField = null;
        try {
            opPermissionsField = PluginManager.class.getDeclaredField("defaultPermsOp");
            opPermissionsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        OP_DEFAULT_PERMISSIONS_FIELD = opPermissionsField;

        Field nonOpPermissionsField = null;
        try {
            nonOpPermissionsField = PluginManager.class.getDeclaredField("defaultPerms");
            nonOpPermissionsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        NON_OP_DEFAULT_PERMISSIONS_FIELD = nonOpPermissionsField;
    }

    private final LPNukkitPlugin plugin;

    public InjectorDefaultsMap(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            LuckPermsDefaultsMap defaultsMap = inject();
            if (defaultsMap != null) {
                this.plugin.setDefaultPermissionMap(defaultsMap);
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst injecting LuckPerms Default Permission map.", e);
        }
    }

    private LuckPermsDefaultsMap inject() throws Exception {
        Objects.requireNonNull(OP_DEFAULT_PERMISSIONS_FIELD, "OP_DEFAULT_PERMISSIONS_FIELD");
        Objects.requireNonNull(NON_OP_DEFAULT_PERMISSIONS_FIELD, "NON_OP_DEFAULT_PERMISSIONS_FIELD");

        PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();

        Object opMap = OP_DEFAULT_PERMISSIONS_FIELD.get(pluginManager);
        Object nonOpMap = NON_OP_DEFAULT_PERMISSIONS_FIELD.get(pluginManager);

        if (opMap instanceof LuckPermsDefaultsMap.DefaultPermissionSet && ((LuckPermsDefaultsMap.DefaultPermissionSet) opMap).parent.plugin == this.plugin) {
            if (nonOpMap instanceof LuckPermsDefaultsMap.DefaultPermissionSet && ((LuckPermsDefaultsMap.DefaultPermissionSet) nonOpMap).parent.plugin == this.plugin) {
                return null;
            }
        }

        //noinspection unchecked
        Map<String, Permission> castedOpMap = (Map<String, Permission>) opMap;

        //noinspection unchecked
        Map<String, Permission> castedNonOpMap = (Map<String, Permission>) nonOpMap;

        // make a new map & inject it
        LuckPermsDefaultsMap newMap = new LuckPermsDefaultsMap(this.plugin, ImmutableMap.of(true, castedOpMap, false, castedNonOpMap));
        OP_DEFAULT_PERMISSIONS_FIELD.set(pluginManager, newMap.getOpPermissions());
        NON_OP_DEFAULT_PERMISSIONS_FIELD.set(pluginManager, newMap.getNonOpPermissions());
        return newMap;
    }

    public static void uninject() {
        try {
            Objects.requireNonNull(OP_DEFAULT_PERMISSIONS_FIELD, "OP_DEFAULT_PERMISSIONS_FIELD");
            Objects.requireNonNull(NON_OP_DEFAULT_PERMISSIONS_FIELD, "NON_OP_DEFAULT_PERMISSIONS_FIELD");
            PluginManager pluginManager = Server.getInstance().getPluginManager();
            {
                Object map = OP_DEFAULT_PERMISSIONS_FIELD.get(pluginManager);
                if (map instanceof LuckPermsDefaultsMap.DefaultPermissionSet) {
                    LuckPermsDefaultsMap.DefaultPermissionSet lpMap = (LuckPermsDefaultsMap.DefaultPermissionSet) map;
                    OP_DEFAULT_PERMISSIONS_FIELD.set(pluginManager, new HashMap<>(lpMap));
                }
            }
            {
                Object map = NON_OP_DEFAULT_PERMISSIONS_FIELD.get(pluginManager);
                if (map instanceof LuckPermsDefaultsMap.DefaultPermissionSet) {
                    LuckPermsDefaultsMap.DefaultPermissionSet lpMap = (LuckPermsDefaultsMap.DefaultPermissionSet) map;
                    NON_OP_DEFAULT_PERMISSIONS_FIELD.set(pluginManager, new HashMap<>(lpMap));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
