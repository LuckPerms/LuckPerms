package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

class CommandManagerBukkit extends CommandManager implements CommandExecutor {
    CommandManagerBukkit(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return onCommand(new Sender() {
            @Override
            public void sendMessage(String s) {
                sender.sendMessage(s);
            }

            @Override
            public boolean hasPermission(String node) {
                return sender.hasPermission(node);
            }
        }, Arrays.asList(args));
    }
}
