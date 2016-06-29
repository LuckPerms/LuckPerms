package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand extends MainCommand {
    public InfoCommand() {
        super("Info", "/perms info", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
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
        return Permission.INFO.isAuthorized(sender);
    }
}
