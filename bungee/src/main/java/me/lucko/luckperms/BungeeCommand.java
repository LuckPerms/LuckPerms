package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.SenderFactory;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;

class BungeeCommand extends Command implements TabExecutor {
    private static final Factory FACTORY = new Factory();
    private final CommandManager manager;

    public BungeeCommand(CommandManager manager) {
        super("luckpermsbungee", null, "bperms", "lpb", "bpermissions", "bp", "bperm");
        this.manager = manager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        manager.onCommand(FACTORY.wrap(sender), "bperms", Arrays.asList(args));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return manager.onTabComplete(FACTORY.wrap(sender), Arrays.asList(args));
    }

    private static class Factory extends SenderFactory<CommandSender> {

        @Override
        protected void sendMessage(CommandSender sender, String s) {
            sender.sendMessage(new TextComponent(s));
        }

        @Override
        protected boolean hasPermission(CommandSender sender, String node) {
            return sender.hasPermission(node);
        }
    }
}
