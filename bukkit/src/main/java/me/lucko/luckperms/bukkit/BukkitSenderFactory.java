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

package me.lucko.luckperms.bukkit;

import lombok.AllArgsConstructor;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.bukkit.compat.BukkitJsonMessageHandler;
import me.lucko.luckperms.bukkit.compat.ReflectionUtil;
import me.lucko.luckperms.bukkit.compat.SpigotJsonMessageHandler;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.sender.SenderFactory;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitSenderFactory extends SenderFactory<CommandSender> {
    private final BukkitJsonMessageHandler bukkitHandler;
    private final SpigotJsonMessageHandler spigotHandler;

    public BukkitSenderFactory(LuckPermsPlugin plugin) {
        super(plugin);
        bukkitHandler = new BukkitJsonMessageHandler();
        spigotHandler = isSpigot() ? new SpigotJsonMessageHandler() : null;
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof Player) {
            return sender.getName();
        }
        return CommandManager.CONSOLE_NAME;
    }

    @Override
    protected UUID getUuid(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return CommandManager.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String s) {
        // send sync if command block
        if (sender instanceof BlockCommandSender) {
            getPlugin().getScheduler().doSync(new BlockMessengerAgent(((BlockCommandSender) sender), s));
            return;
        }

        sender.sendMessage(s);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        if (ReflectionUtil.isChatCompatible() && sender instanceof Player) {
            Player player = (Player) sender;
            String json = ComponentSerializers.JSON.serialize(message);

            // Try Bukkit.
            if (bukkitHandler.sendJsonMessage(player, json)) {
                return;
            }

            // Try Spigot.
            if (spigotHandler != null && spigotHandler.sendJsonMessage(player, json)) {
                return;
            }
        }

        // Fallback to legacy format
        sendMessage(sender, TextUtils.toLegacy(message));
    }

    @Override
    protected Tristate getPermissionValue(CommandSender sender, String node) {
        boolean isSet = sender.isPermissionSet(node);
        boolean val = sender.hasPermission(node);

        return !isSet ? val ? Tristate.TRUE : Tristate.UNDEFINED : Tristate.fromBoolean(val);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    private static boolean isSpigot() {
        try {
            Class.forName("net.md_5.bungee.chat.ComponentSerializer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @AllArgsConstructor
    private static final class BlockMessengerAgent implements Runnable {
        private final BlockCommandSender block;
        private final String message;

        @Override
        public void run() {
            block.sendMessage(message);
        }
    }

}
