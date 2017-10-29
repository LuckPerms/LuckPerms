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

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;

import org.bukkit.Bukkit;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.Map;

@RequiredArgsConstructor
public class SubscriptionMapInjector implements Runnable {
    private static final Field PERM_SUBS_FIELD;

    static {
        Field permSubsField;
        try {
            permSubsField = SimplePluginManager.class.getDeclaredField("permSubs");
            permSubsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        PERM_SUBS_FIELD = permSubsField;
    }

    private final LPBukkitPlugin plugin;

    @Override
    public void run() {
        try {
            inject();
        } catch (Exception e) {
            plugin.getLog().severe("Exception occurred whilst injecting LuckPerms Permission Subscription map.");
            e.printStackTrace();
        }
    }

    private void inject() throws Exception {
        PluginManager pluginManager = plugin.getServer().getPluginManager();

        if (!(pluginManager instanceof SimplePluginManager)) {
            plugin.getLog().severe("PluginManager instance is not a 'SimplePluginManager', instead: " + pluginManager.getClass());
            plugin.getLog().severe("Unable to inject LuckPerms Permission Subscription map.");
            return;
        }

        Object map = PERM_SUBS_FIELD.get(pluginManager);
        if (map instanceof LPSubscriptionMap) {
            return; // already injected
        }

        //noinspection unchecked
        Map<String, Map<Permissible, Boolean>> castedMap = (Map<String, Map<Permissible, Boolean>>) map;

        // make a new subscription map
        LPSubscriptionMap newMap = new LPSubscriptionMap(plugin, castedMap);

        // inject it
        PERM_SUBS_FIELD.set(pluginManager, newMap);
    }

    public static void uninject() {
        try {
            PluginManager pluginManager = Bukkit.getServer().getPluginManager();
            if (!(pluginManager instanceof SimplePluginManager)) {
                return;
            }

            Object map = PERM_SUBS_FIELD.get(pluginManager);
            if (map instanceof LPSubscriptionMap) {
                LPSubscriptionMap lpMap = (LPSubscriptionMap) map;
                PERM_SUBS_FIELD.set(pluginManager, lpMap.detach());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
