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

package me.lucko.luckperms.bukkit.brigadier;

import com.mojang.brigadier.tree.LiteralCommandNode;

import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import me.lucko.commodore.file.CommodoreFileFormat;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.sender.Sender;

import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.PluginManager;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registers LuckPerms command data to brigadier using {@link Commodore}.
 */
public class LuckPermsBrigadier {

    /**
     * Registers LuckPerms command data to the given {@code pluginCommand}.
     *
     * @param plugin the luckperms plugin
     * @param pluginCommand the command
     * @throws Exception if something goes wrong
     */
    public static void register(LPBukkitPlugin plugin, Command pluginCommand) throws Exception {
        Commodore commodore = CommodoreProvider.getCommodore(plugin.getBootstrap());
        try (InputStream is = plugin.getBootstrap().getResourceStream("luckperms.commodore")) {
            if (is == null) {
                throw new Exception("Brigadier command data missing from jar");
            }

            LiteralCommandNode<?> command = CommodoreFileFormat.parse(is);
            commodore.register(pluginCommand, command);
        }

        // add event listener to prevent completions from being send to players without permission
        // to use any LP commands.
        PluginManager pluginManager = plugin.getBootstrap().getServer().getPluginManager();
        pluginManager.registerEvents(new PermissionListener(plugin, pluginCommand), plugin.getBootstrap());
    }

    private static final class PermissionListener implements Listener {
        private final LPBukkitPlugin plugin;
        private final List<String> aliases;

        private PermissionListener(LPBukkitPlugin plugin, Command pluginCommand) {
            this.plugin = plugin;
            this.aliases = Commodore.getAliases(pluginCommand).stream()
                    .flatMap(alias -> Stream.of(alias, "minecraft:" + alias))
                    .collect(Collectors.toList());
        }

        @EventHandler
        public void onCommandSend(PlayerCommandSendEvent e) {
            Sender playerAsSender = this.plugin.getSenderFactory().wrap(e.getPlayer());
            if (!this.plugin.getCommandManager().hasPermissionForAny(playerAsSender)) {
                e.getCommands().removeAll(this.aliases);
            }
        }
    }

}
