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

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.function.Function;

public final class PlayerLocaleUtil {
    private PlayerLocaleUtil() {}

    private static final Function<Player, String> GET_LOCALE_FUNCTION;

    static {
        Function<Player, String> function;
        try {
            // modern bukkit
            Player.class.getMethod("getLocale");
            function = Player::getLocale;
        } catch (ReflectiveOperationException ex) {
            try {
                // legacy spigot method
                Method legacyMethod = Player.Spigot.class.getMethod("getLocale");
                function = player -> {
                    try {
                        return (String) legacyMethod.invoke(player.spigot());
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (ReflectiveOperationException e) {
                // fallback
                function = player -> null;
            }
        }
        GET_LOCALE_FUNCTION = function;
    }

    public static String getLocale(Player player) {
        return GET_LOCALE_FUNCTION.apply(player);
    }

}
