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

package me.lucko.luckperms.nukkit.model.server;

import me.lucko.luckperms.nukkit.LPNukkitPlugin;

import cn.nukkit.Server;
import cn.nukkit.permission.Permissible;
import cn.nukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Injects a {@link LPSubscriptionMap} into the {@link PluginManager}.
 */
public class InjectorSubscriptionMap implements Runnable {
    private static final Field PERM_SUBS_FIELD;

    static {
        Field permSubsField = null;
        try {
            permSubsField = PluginManager.class.getDeclaredField("permSubs");
            permSubsField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
        PERM_SUBS_FIELD = permSubsField;
    }

    private final LPNukkitPlugin plugin;

    public InjectorSubscriptionMap(LPNukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            LPSubscriptionMap ret = inject();
            if (ret != null) {
                this.plugin.setSubscriptionMap(ret);
            }
        } catch (Exception e) {
            this.plugin.getLog().severe("Exception occurred whilst injecting LuckPerms Permission Subscription map.");
            e.printStackTrace();
        }
    }

    private LPSubscriptionMap inject() throws Exception {
        Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");
        PluginManager pluginManager = this.plugin.getServer().getPluginManager();

        Object map = PERM_SUBS_FIELD.get(pluginManager);
        if (map instanceof LPSubscriptionMap) {
            if (((LPSubscriptionMap) map).plugin == this.plugin) {
                return null;
            }

            map = ((LPSubscriptionMap) map).detach();
        }

        //noinspection unchecked
        Map<String, Set<Permissible>> castedMap = (Map<String, Set<Permissible>>) map;

        // make a new subscription map & inject it
        LPSubscriptionMap newMap = new LPSubscriptionMap(this.plugin, castedMap);
        PERM_SUBS_FIELD.set(pluginManager, newMap);
        return newMap;
    }

    public static void uninject() {
        try {
            Objects.requireNonNull(PERM_SUBS_FIELD, "PERM_SUBS_FIELD");
            PluginManager pluginManager = Server.getInstance().getPluginManager();

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
