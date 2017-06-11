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

package me.lucko.luckperms.bukkit.compat;

import me.lucko.luckperms.common.constants.Constants;

import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageHandler {
    private final BukkitJsonMessageHandler bukkitHandler;
    private final SpigotJsonMessageHandler spigotHandler;

    public MessageHandler() {
        bukkitHandler = new BukkitJsonMessageHandler();
        spigotHandler = isSpigot() ? new SpigotJsonMessageHandler() : null;
    }

    public void sendJsonMessage(CommandSender sender, Component message) {
        if (ReflectionUtil.isChatCompatible() && sender instanceof Player) {
            Player player = (Player) sender;
            String json = ComponentSerializer.serialize(message);

            // Try Bukkit.
            if (bukkitHandler.sendJsonMessage(player, json)) {
                return;
            }

            // Try Spigot.
            if (spigotHandler != null && spigotHandler.sendJsonMessage(player, json)) {
                return;
            }
        }

        // Fallback to Bukkit
        sender.sendMessage(ComponentSerializer.toLegacy(message, Constants.COLOR_CHAR));
    }

    private static boolean isSpigot() {
        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
