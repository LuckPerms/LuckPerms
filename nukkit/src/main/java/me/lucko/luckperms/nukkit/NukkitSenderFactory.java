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

package me.lucko.luckperms.nukkit;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.Component;
import net.luckperms.api.util.Tristate;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;

import java.util.UUID;

public class NukkitSenderFactory extends SenderFactory<CommandSender> {
    public NukkitSenderFactory(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(CommandSender sender) {
        if (sender instanceof Player) {
            return sender.getName();
        }
        return Sender.CONSOLE_NAME;
    }

    @Override
    protected UUID getUniqueId(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getUniqueId();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected void sendMessage(CommandSender sender, String s) {
        // we can safely send async for players and the console
        if (sender instanceof Player || sender instanceof ConsoleCommandSender) {
            sender.sendMessage(s);
            return;
        }

        // otherwise, send the message sync
        getPlugin().getBootstrap().getScheduler().executeSync(new SyncMessengerAgent(sender, s));
    }

    @Override
    protected void sendMessage(CommandSender sender, Component message) {
        // Fallback to legacy format
        sendMessage(sender, TextUtils.toLegacy(message));
    }

    @Override
    protected Tristate getPermissionValue(CommandSender sender, String node) {
        boolean isSet = sender.isPermissionSet(node);
        boolean val = sender.hasPermission(node);

        return !isSet ? val ? Tristate.TRUE : Tristate.UNDEFINED : Tristate.of(val);
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String node) {
        return sender.hasPermission(node);
    }

    private static final class SyncMessengerAgent implements Runnable {
        private final CommandSender sender;
        private final String message;

        private SyncMessengerAgent(CommandSender sender, String message) {
            this.sender = sender;
            this.message = message;
        }

        @Override
        public void run() {
            this.sender.sendMessage(this.message);
        }
    }

}
