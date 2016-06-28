package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends MainCommand {
    public InfoCommand() {
        super("Info", "/perms info", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (!sender.hasPermission("luckperms.info")) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        Util.sendPluginMessage(sender, "&6Running &bLuckPerms " + plugin.getVersion() + "&6.");
        Util.sendPluginMessage(sender, "&eAuthor: &6Luck");
        Util.sendPluginMessage(sender, "&eStorage Method: &6" + plugin.getDatastore().getName());
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return sender.hasPermission("luckperms.info") || sender.hasPermission("luckperms.*");
    }
}
