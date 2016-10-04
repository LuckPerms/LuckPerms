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

package me.lucko.luckperms.inject;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;

/**
 * Injects a {@link LPPermissible} into a {@link Player}
 */
@UtilityClass
public class Injector {
    private static Field HUMAN_ENTITY_FIELD;

    static {
        try {
            HUMAN_ENTITY_FIELD = Class.forName(getInternalClassName("entity.CraftHumanEntity")).getDeclaredField("perm");
            HUMAN_ENTITY_FIELD.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean inject(CommandSender sender, PermissibleBase permissible) {
        try {
            Field f = getPermField(sender);
            f.set(sender, permissible);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean unInject(CommandSender sender) {
        try {
            Permissible permissible = getPermissible(sender);
            if (permissible instanceof LPPermissible) {
                getPermField(sender).set(sender, null);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static Permissible getPermissible(CommandSender sender) {
        try {
            Field f = getPermField(sender);
            return (Permissible) f.get(sender);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getPermField(CommandSender sender) {
        if (sender instanceof Player) {
            return HUMAN_ENTITY_FIELD;
        }
        throw new RuntimeException("Couldn't get perm field for sender " + sender.getClass().getName());
    }

    private static String getInternalClassName(String className) {
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
