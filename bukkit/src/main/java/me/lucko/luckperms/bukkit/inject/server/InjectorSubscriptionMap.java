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
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

/**
 * Injects a {@link LuckPermsSubscriptionMap} into the {@link PluginManager}.
 */
public class InjectorSubscriptionMap {
    private static final Field PERM_SUBS_FIELD;

    static {
        Field permSubsField = null;
        try {
            permSubsField = SimplePluginManager.class.getDeclaredField("permSubs");
            permSubsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        PERM_SUBS_FIELD = permSubsField;
    }

    private final LPBukkitPlugin plugin;

    public InjectorSubscriptionMap(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    public void inject() {
        try {
            LuckPermsSubscriptionMap subscriptionMap = tryInject();
            if (subscriptionMap != null) {
                this.plugin.setSubscriptionMap(subscriptionMap);
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst injecting LuckPerms Permission Subscription map.", e);
        }
    }

    private LuckPermsSubscriptionMap tryInject() throws Exception {
        Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");
        PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();

        if (!(pluginManager instanceof SimplePluginManager)) {
            this.plugin.getLogger().severe("PluginManager instance is not a 'SimplePluginManager', instead: " + pluginManager.getClass());
            this.plugin.getLogger().severe("Unable to inject LuckPerms Permission Subscription map.");
            return null;
        }

        Object map = PERM_SUBS_FIELD.get(pluginManager);
        if (map instanceof LuckPermsSubscriptionMap) {
            if (((LuckPermsSubscriptionMap) map).plugin == this.plugin) {
                return null;
            }

            map = ((LuckPermsSubscriptionMap) map).detach();
        }

        //noinspection unchecked
        Map<String, Map<Permissible, Boolean>> castedMap = (Map<String, Map<Permissible, Boolean>>) map;

        // make a new subscription map & inject it
        LuckPermsSubscriptionMap newMap = new LuckPermsSubscriptionMap(this.plugin, castedMap);
        PERM_SUBS_FIELD.set(pluginManager, newMap);
        return newMap;
    }

    public void uninject() {
        try {
            Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");

            PluginManager pluginManager = this.plugin.getBootstrap().getServer().getPluginManager();
            if (!(pluginManager instanceof SimplePluginManager)) {
                return;
            }

            Object map = PERM_SUBS_FIELD.get(pluginManager);
            if (map instanceof LuckPermsSubscriptionMap) {
                LuckPermsSubscriptionMap lpMap = (LuckPermsSubscriptionMap) map;
                PERM_SUBS_FIELD.set(pluginManager, lpMap.detach());
            }
        } catch (Exception e) {
            this.plugin.getLogger().severe("Exception occurred whilst uninjecting LuckPerms Permission Subscription map.", e);
        }
    }

}
