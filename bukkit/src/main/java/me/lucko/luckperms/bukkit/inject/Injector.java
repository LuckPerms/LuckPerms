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

package me.lucko.luckperms.bukkit.inject;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Injects a {@link LPPermissible} into a {@link Player}
 */
@UtilityClass
public class Injector {
    private static final Map<UUID, LPPermissible> INJECTED_PERMISSIBLES = new ConcurrentHashMap<>();
    private static Field HUMAN_ENTITY_FIELD;

    static {
        try {
            HUMAN_ENTITY_FIELD = Class.forName(getVersionedClassName("entity.CraftHumanEntity")).getDeclaredField("perm");
            HUMAN_ENTITY_FIELD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean inject(Player player, LPPermissible permissible) {
        try {
            HUMAN_ENTITY_FIELD.set(player, permissible);
            INJECTED_PERMISSIBLES.put(player.getUniqueId(), permissible);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean unInject(Player player) {
        try {
            Permissible permissible = (Permissible) HUMAN_ENTITY_FIELD.get(player);
            if (permissible instanceof LPPermissible) {
                /* The player is most likely leaving. Bukkit will attempt to call #clearPermissions, so we cannot set to null.
                   However, there's no need to re-inject a real PermissibleBase, so we just inject a dummy instead.
                   This saves tick time, pointlessly recalculating defaults when the instance will never be used. */
                HUMAN_ENTITY_FIELD.set(player, new DummyPermissibleBase());
            }
            INJECTED_PERMISSIBLES.remove(player.getUniqueId());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static LPPermissible getPermissible(UUID uuid) {
        return INJECTED_PERMISSIBLES.get(uuid);
    }

    private static String getVersionedClassName(String className) {
        Class server = Bukkit.getServer().getClass();
        if (!server.getSimpleName().equals("CraftServer")) {
            throw new RuntimeException("Couldn't inject into server " + server);
        }

        String version;
        if (server.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
            // Non versioned class
            version = ".";
        } else {
            version = server.getName().substring("org.bukkit.craftbukkit".length());
            version = version.substring(0, version.length() - "CraftServer".length());
        }

        return "org.bukkit.craftbukkit" + version + className;
    }

}
