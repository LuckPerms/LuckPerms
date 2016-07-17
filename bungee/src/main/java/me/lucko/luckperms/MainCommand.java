package me.lucko.luckperms;

import me.lucko.luckperms.commands.CommandManager;
import me.lucko.luckperms.commands.Sender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.lang.ref.WeakReference;
import java.util.Arrays;

class MainCommand extends Command implements TabExecutor {
    private final CommandManager manager;

    public MainCommand(CommandManager manager) {
        super("luckpermsbungee", null, "bperms", "lpb", "bpermissions", "bp", "bperm");
        this.manager = manager;

    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        manager.onCommand(makeSender(sender), Arrays.asList(args));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return manager.onTabComplete(makeSender(sender), Arrays.asList(args));
    }

    private static Sender makeSender(CommandSender sender) {
        return new Sender() {
            final WeakReference<CommandSender> cs = new WeakReference<>(sender);

            @Override
            public void sendMessage(String s) {
                final CommandSender c = cs.get();
                if (c != null) {
                    c.sendMessage(new TextComponent(s));
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
