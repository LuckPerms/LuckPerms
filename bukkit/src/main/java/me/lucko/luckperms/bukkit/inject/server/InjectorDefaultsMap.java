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
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Injects a {@link LuckPermsDefaultsMap} info the {@link PluginManager}.
 */
public class InjectorDefaultsMap {
    private static final Field DEFAULT_PERMISSIONS_FIELD;

    static {
        Field permissionsField = null;
        try {
            permissionsField = SimplePluginManager.class.getDeclaredField("defaultPerms");
            permissionsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        DEFAULT_PERMISSIONS_FIELD = permissionsField;
    }

    private final LPBukkitPlugin plugin;

    public InjectorDefaultsMap(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    public void inject() {
        try {
            LuckPermsDefaultsMap defaultsMap = tryInject();
            if (defaultsMap != null) {
                this.plugin.setDefaultPermissionMap(defaultsMap);
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst injecting LuckPerms Default Permission map.", e);
        }
    }

    private LuckPermsDefaultsMap tryInject() throws Exception {
        Objects.requireNonNull(DEFAULT_PERMISSIONS_FIELD, "DEFAULT_PERMISSIONS_FIELD");
        PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();

        if (!(pluginManager instanceof SimplePluginManager)) {
            this.plugin.getLogger().severe("PluginManager instance is not a 'SimplePluginManager', instead: " + pluginManager.getClass());
            this.plugin.getLogger().severe("Unable to inject LuckPerms Default Permission map.");
            return null;
        }

        Object map = DEFAULT_PERMISSIONS_FIELD.get(pluginManager);
        if (map instanceof LuckPermsDefaultsMap && ((LuckPermsDefaultsMap) map).plugin == this.plugin) {
            return null;
        }

        //noinspection unchecked
        Map<Boolean, Set<Permission>> castedMap = (Map<Boolean, Set<Permission>>) map;

        // make a new map & inject it
        LuckPermsDefaultsMap newMap = new LuckPermsDefaultsMap(this.plugin, castedMap);
        DEFAULT_PERMISSIONS_FIELD.set(pluginManager, newMap);
        return newMap;
    }

    public void uninject() {
        try {
            Objects.requireNonNull(DEFAULT_PERMISSIONS_FIELD, "DEFAULT_PERMISSIONS_FIELD");

            PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();
            if (!(pluginManager instanceof SimplePluginManager)) {
                return;
            }

            Object map = DEFAULT_PERMISSIONS_FIELD.get(pluginManager);
            if (map instanceof LuckPermsDefaultsMap) {
                LuckPermsDefaultsMap lpMap = (LuckPermsDefaultsMap) map;
                DEFAULT_PERMISSIONS_FIELD.set(pluginManager, new HashMap<>(lpMap));
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst uninjecting LuckPerms Default Permission map.", e);
        }
    }
}
