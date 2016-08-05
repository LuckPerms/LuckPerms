package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.SenderFactory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

class BukkitCommand extends CommandManager implements CommandExecutor, TabExecutor {
    private static final Factory FACTORY = new Factory();

    BukkitCommand(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return onCommand(FACTORY.wrap(sender), label, Arrays.asList(args));
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return onTabComplete(FACTORY.wrap(sender), Arrays.asList(args));
    }

    private static class Factory extends SenderFactory<CommandSender> {

        @Override
        protected void sendMessage(CommandSender sender, String s) {
            sender.sendMessage(s);
        }

        @Override
        protected boolean hasPermission(CommandSender sender, String node) {
            return sender.hasPermission(node);
        }
    }
}
