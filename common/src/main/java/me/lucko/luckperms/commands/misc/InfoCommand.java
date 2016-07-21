package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.utils.LPConfiguration;

import java.util.Collections;
import java.util.List;

public class InfoCommand extends MainCommand {
    public InfoCommand() {
        super("Info", "/%s info", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        final LPConfiguration c = plugin.getConfiguration();
        Message.INFO.send(sender, plugin.getVersion(), plugin.getDatastore().getName(), c.getServer(),
                c.getDefaultGroupName(), c.getSyncTime(), c.getIncludeGlobalPerms());
    }

    @Override
    protected List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return Collections.emptyList();
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return Collections.emptyList();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.INFO.isAuthorized(sender);
    }
}
