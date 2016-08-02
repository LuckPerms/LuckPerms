package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.utils.LPConfiguration;

import java.util.List;

public class InfoCommand extends SingleMainCommand {
    public InfoCommand() {
        super("Info", "/%s info", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final LPConfiguration c = plugin.getConfiguration();
        Message.INFO.send(sender, plugin.getVersion(), plugin.getDatastore().getName(), c.getServer(),
                c.getDefaultGroupName(), c.getSyncTime(), c.getIncludeGlobalPerms(), c.getOnlineMode());
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return Permission.INFO.isAuthorized(sender);
    }
}
