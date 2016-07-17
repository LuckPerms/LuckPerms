package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

class CommandManagerBukkit extends CommandManager implements CommandExecutor, TabExecutor {
    CommandManagerBukkit(LuckPermsPlugin plugin) {
        super(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return onCommand(makeSender(sender), Arrays.asList(args));
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return onTabComplete(makeSender(sender), Arrays.asList(args));
    }

    private static Sender makeSender(CommandSender sender) {
        return new Sender() {
            final WeakReference<CommandSender> cs = new WeakReference<>(sender);

            @Override
            public void sendMessage(String s) {
                final CommandSender c = cs.get();
                if (c != null) {
                    c.sendMessage(s);
                }
            }

            @Override
            public boolean hasPermission(String node) {
                final CommandSender c = cs.get();
                return c != null && c.hasPermission(node);
            }
        };
    }
}
