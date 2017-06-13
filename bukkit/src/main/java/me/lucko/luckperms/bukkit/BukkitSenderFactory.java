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

import me.lucko.luckperms.bukkit.compat.MessageHandler;
import me.lucko.luckperms.common.commands.sender.SenderFactory;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitSenderFactory extends SenderFactory<CommandSender> {
    private final MessageHandler messageHandler;

    public BukkitSenderFactory(LuckPermsPlugin plugin) {
        super(plugin);
        messageHandler = new MessageHandler();
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof Player) {
            return sender.getName();
        }
        return Constants.CONSOLE_NAME;
    }

    @Override
    protected UUID getUuid(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Constants.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String s) {

        // send sync if command block
        if (sender instanceof BlockCommandSender) {
            getPlugin().getScheduler().doSync(() -> sender.sendMessage(s));
            return;
        }

        sender.sendMessage(s);
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        messageHandler.sendJsonMessage(sender, message);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }
}
