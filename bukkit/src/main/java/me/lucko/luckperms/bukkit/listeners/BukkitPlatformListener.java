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

package me.lucko.luckperms.bukkit.listeners;

import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.message.Message;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class BukkitPlatformListener implements Listener {
    private final LPBukkitPlugin plugin;

    public BukkitPlatformListener(LPBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        handleCommand(e.getPlayer(), e.getMessage().toLowerCase(), e);
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        handleCommand(e.getSender(), e.getCommand().toLowerCase(), e);
    }

    @EventHandler
    public void onRemoteServerCommand(RemoteServerCommandEvent e) {
        handleCommand(e.getSender(), e.getCommand().toLowerCase(), e);
    }

    private void handleCommand(CommandSender sender, String s, Cancellable event) {
        if (s.isEmpty()) {
            return;
        }

        if (this.plugin.getConfiguration().get(ConfigKeys.OPS_ENABLED)) {
            return;
        }

        if (s.charAt(0) == '/') {
            s = s.substring(1);
        }

        if (s.contains(":")) {
            s = s.substring(s.indexOf(':') + 1);
        }

        if (s.equals("op") || s.startsWith("op ") || s.equals("deop") || s.startsWith("deop ")) {
            event.setCancelled(true);
            sender.sendMessage(Message.OP_DISABLED.asString(this.plugin.getLocaleManager()));
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent e) {
        if (e.getPlugin().getName().equalsIgnoreCase("Vault")) {
            this.plugin.tryVaultHook(true);
        }
    }

}
