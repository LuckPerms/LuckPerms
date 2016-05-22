package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;

public class MainCommand extends Command {
    private final CommandManager manager;

    public MainCommand(CommandManager manager) {
        super("luckpermsbungee", "luckperms.use", "bperms", "lpb", "bpermissions", "bp", "bperm");
        this.manager = manager;

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        manager.onCommand(new Sender() {
            @Override
            public void sendMessage(String s) {
                sender.sendMessage(new TextComponent(s));
            }

            @Override
            public boolean hasPermission(String node) {
                return sender.hasPermission(node);
            }
        }, Arrays.asList(args));
    }
}
