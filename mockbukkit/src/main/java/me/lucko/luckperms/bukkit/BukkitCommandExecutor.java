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

import me.lucko.luckperms.bukkit.util.CommandMapUtil;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.ArgumentTokenizer;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.sender.Sender;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class BukkitCommandExecutor extends CommandManager implements TabExecutor, Listener {
    private static final boolean SELECT_ENTITIES_SUPPORTED;

    static {
        boolean selectEntitiesSupported = false;
        try {
            Server.class.getMethod("selectEntities", CommandSender.class, String.class);
            selectEntitiesSupported = true;
        } catch (NoSuchMethodException e) {
            // ignore
        }
        SELECT_ENTITIES_SUPPORTED = selectEntitiesSupported;
    }

    protected final LPBukkitPlugin plugin;
    protected final PluginCommand command;

    public BukkitCommandExecutor(LPBukkitPlugin plugin, PluginCommand command) {
        super(plugin);
        this.plugin = plugin;
        this.command = command;
    }

    public void register() {
        this.command.setExecutor(this);
        this.command.setTabCompleter(this);
        this.plugin.getBootstrap().getServer().getPluginManager().registerEvents(this, this.plugin.getLoader());
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = resolveSelectors(sender, ArgumentTokenizer.EXECUTE.tokenizeInput(args));
        executeCommand(wrapped, label, arguments);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        Sender wrapped = this.plugin.getSenderFactory().wrap(sender);
        List<String> arguments = resolveSelectors(sender, ArgumentTokenizer.TAB_COMPLETE.tokenizeInput(args));
        return tabCompleteCommand(wrapped, arguments);
    }

    // Support LP commands prefixed with a '/' from the console.
    @EventHandler(ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent e) {
        if (!(e.getSender() instanceof ConsoleCommandSender)) {
            return;
        }

        String buffer = e.getCommand();
        if (buffer.isEmpty() || buffer.charAt(0) != '/') {
            return;
        }

        buffer = buffer.substring(1);

        String commandLabel;
        int firstSpace = buffer.indexOf(' ');
        if (firstSpace == -1) {
            commandLabel = buffer;
        } else {
            commandLabel = buffer.substring(0, firstSpace);
        }

        Command command = CommandMapUtil.getCommandMap(this.plugin.getBootstrap().getServer()).getCommand(commandLabel);
        if (command != this.command) {
            return;
        }

        e.setCommand(buffer);
    }

    private List<String> resolveSelectors(CommandSender sender, List<String> args) {
        if (!SELECT_ENTITIES_SUPPORTED) {
            return args;
        }

        if (!this.plugin.getConfiguration().get(ConfigKeys.RESOLVE_COMMAND_SELECTORS)) {
            return args;
        }

        for (ListIterator<String> it = args.listIterator(); it.hasNext(); ) {
            String arg = it.next();
            if (arg.isEmpty() || arg.charAt(0) != '@') {
                continue;
            }

            List<Player> matchedPlayers;
            try {
                matchedPlayers = this.plugin.getBootstrap().getServer().selectEntities(sender, arg).stream()
                        .filter(e -> e instanceof Player)
                        .map(e -> (Player) e)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + sender + " executing " + args, e);
                continue;
            }

            if (matchedPlayers.isEmpty()) {
                continue;
            }

            if (matchedPlayers.size() > 1) {
                this.plugin.getLogger().warn("Error parsing selector '" + arg + "' for " + sender + " executing " + args +
                        ": ambiguous result (more than one player matched) - " + matchedPlayers);
                continue;
            }

            Player player = matchedPlayers.get(0);
            it.set(player.getUniqueId().toString());
        }

        return args;
    }
}
