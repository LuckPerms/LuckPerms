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

package me.lucko.luckperms.bukkit.util;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CraftBukkitImplementation {
    private CraftBukkitImplementation() {}

    private static final String SERVER_PACKAGE_VERSION;
    private static final boolean CHAT_COMPATIBLE;

    static {
        Class<?> server = Bukkit.getServer().getClass();
        Matcher matcher = Pattern.compile("^org\\.bukkit\\.craftbukkit\\.(\\w+)\\.CraftServer$").matcher(server.getName());
        if (matcher.matches()) {
            SERVER_PACKAGE_VERSION = '.' + matcher.group(1) + '.';
            CHAT_COMPATIBLE = !SERVER_PACKAGE_VERSION.startsWith(".v1_7_");
        } else {
            SERVER_PACKAGE_VERSION = ".";
            CHAT_COMPATIBLE = true;
        }
    }

    public static boolean isChatCompatible() {
        return CHAT_COMPATIBLE;
    }

    public static String nms(String className) {
        return "net.minecraft.server" + SERVER_PACKAGE_VERSION + className;
    }

    public static Class<?> nmsClass(String className) throws ClassNotFoundException {
        return Class.forName(nms(className));
    }

    public static String obc(String className) {
        return "org.bukkit.craftbukkit" + SERVER_PACKAGE_VERSION + className;
    }

    public static Class<?> obcClass(String className) throws ClassNotFoundException {
        return Class.forName(obc(className));
    }
}
