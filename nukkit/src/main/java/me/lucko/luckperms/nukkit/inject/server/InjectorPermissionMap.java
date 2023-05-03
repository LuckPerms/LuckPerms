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
import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Injects a {@link LuckPermsPermissionMap} into the {@link PluginManager}.
 */
public class InjectorPermissionMap implements Runnable {
    private static final Field PERMISSIONS_FIELD;

    static {
        Field permissionsField = null;
        try {
            permissionsField = PluginManager.class.getDeclaredField("permissions");
            permissionsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        PERMISSIONS_FIELD = permissionsField;
    }

    private final LPNukkitPlugin plugin;

    public InjectorPermissionMap(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            LuckPermsPermissionMap permissionMap = inject();
            if (permissionMap != null) {
                this.plugin.setPermissionMap(permissionMap);
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst injecting LuckPerms Permission map.", e);
        }
    }

    private LuckPermsPermissionMap inject() throws Exception {
        Objects.requireNonNull(PERMISSIONS_FIELD, "PERMISSIONS_FIELD");
        PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();

        Object map = PERMISSIONS_FIELD.get(pluginManager);
        if (map instanceof LuckPermsPermissionMap && ((LuckPermsPermissionMap) map).plugin == this.plugin) {
            return null;
        }

        //noinspection unchecked
        Map<String, Permission> castedMap = (Map<String, Permission>) map;

        // make a new map & inject it
        LuckPermsPermissionMap newMap = new LuckPermsPermissionMap(this.plugin, castedMap);
        PERMISSIONS_FIELD.set(pluginManager, newMap);
        return newMap;
    }

    public static void uninject() {
        try {
            Objects.requireNonNull(PERMISSIONS_FIELD, "PERMISSIONS_FIELD");
            PluginManager pluginManager = Server.getInstance().getPluginManager();

            Object map = PERMISSIONS_FIELD.get(pluginManager);
            if (map instanceof LuckPermsPermissionMap) {
                LuckPermsPermissionMap lpMap = (LuckPermsPermissionMap) map;
                PERMISSIONS_FIELD.set(pluginManager, new HashMap<>(lpMap));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
