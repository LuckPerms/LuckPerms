package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.Collections;
import java.util.List;

public class DebugCommand extends MainCommand {
    public DebugCommand() {
        super("Debug", "/perms debug", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        Message.DEBUG.send(sender, plugin.getPlayerCount(), plugin.getUserManager().getUsers().size(),
                plugin.getGroupManager().getGroups().size(), plugin.getTrackManager().getTracks().size()
        );
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
        return Permission.DEBUG.isAuthorized(sender);
    }
}
