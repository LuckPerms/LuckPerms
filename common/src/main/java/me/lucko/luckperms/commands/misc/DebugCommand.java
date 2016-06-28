package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;

import java.util.ArrayList;
import java.util.List;

public class DebugCommand extends MainCommand {
    public DebugCommand() {
        super("Debug", "/perms debug", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (!sender.hasPermission("luckperms.debug")) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        Util.sendPluginMessage(sender, "&d&l> &dDebug Info");
        Util.sendPluginMessage(sender, "&eOnline Players: &6" + plugin.getPlayerCount());
        Util.sendPluginMessage(sender, "&eLoaded Users: &6" + plugin.getUserManager().getUsers().size());
        Util.sendPluginMessage(sender, "&eLoaded Groups: &6" + plugin.getGroupManager().getGroups().size());
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return sender.hasPermission("luckperms.debug") || sender.hasPermission("luckperms.*");
    }
}
