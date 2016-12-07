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

package me.lucko.luckperms.bukkit.compat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BukkitJsonMessageHandler {
    private static boolean setup = false;
    private static boolean triedAndFailed = false;

    private static Method GET_HANDLE_METHOD;
    private static Field PLAYER_CONNECTION_FIELD;
    private static Method SEND_PACKET_METHOD;
    private static Constructor<?> PACKET_CHAT_CONSTRUCTOR;
    private static Method SERIALIZE_METHOD;

    private static void setup(Object player) throws Exception {
        Class<?> craftPlayerClass = player.getClass();
        GET_HANDLE_METHOD = craftPlayerClass.getDeclaredMethod("getHandle");

        Object handleObject = GET_HANDLE_METHOD.invoke(player);
        Class<?> handleClass = handleObject.getClass();

        PLAYER_CONNECTION_FIELD = handleClass.getDeclaredField("playerConnection");

        Object playerConnectionObject = PLAYER_CONNECTION_FIELD.get(handleObject);

        Method[] playerConnectionMethods = playerConnectionObject.getClass().getDeclaredMethods();
        for (Method m : playerConnectionMethods) {
            if (m.getName().equals("sendPacket")) {
                SEND_PACKET_METHOD = m;
                break;
            }
        }

        Class<?> packetChatClass = Class.forName(getVersionedClassName("PacketPlayOutChat"));
        Constructor[] packetConstructors = packetChatClass.getDeclaredConstructors();
        for (Constructor c : packetConstructors) {
            Class<?>[] parameters = c.getParameterTypes();
            if (parameters.length == 1 && parameters[0].getName().endsWith("IChatBaseComponent")) {
                PACKET_CHAT_CONSTRUCTOR = c;
                break;
            }
        }

        Class<?> baseComponentClass = Class.forName(getVersionedClassName("IChatBaseComponent"));
        Class<?> chatSerializerClass = baseComponentClass.getClasses()[0];

        SERIALIZE_METHOD = chatSerializerClass.getDeclaredMethod("a", String.class);
    }

    private static String getVersionedClassName(String className) {
        Class server = Bukkit.getServer().getClass();
        if (!server.getSimpleName().equals("CraftServer")) {
            throw new RuntimeException("Couldn't reflect into server " + server);
        }

        String version;
        if (server.getName().equals("org.bukkit.craftbukkit.CraftServer")) {
            // Non versioned class
            version = ".";
        } else {
            version = server.getName().substring("org.bukkit.craftbukkit".length());
            version = version.substring(0, version.length() - "CraftServer".length());
        }

        return "net.minecraft.server" + version + className;
    }

    private static synchronized boolean trySetup(Object player) {
        if (setup) return true;
        if (triedAndFailed) return false;

        try {
            setup(player);
            setup = true;
            return true;
        } catch (Exception e) {
            triedAndFailed = true;
            return false;
        }
    }

    public boolean sendJsonMessage(Player player, String json) {
        if (!trySetup(player)) {
            return false;
        }

        try {
            Object connection = PLAYER_CONNECTION_FIELD.get(GET_HANDLE_METHOD.invoke(player));
            SEND_PACKET_METHOD.invoke(connection, PACKET_CHAT_CONSTRUCTOR.newInstance(SERIALIZE_METHOD.invoke(null, json)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
