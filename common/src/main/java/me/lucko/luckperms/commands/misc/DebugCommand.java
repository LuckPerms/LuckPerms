package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.List;

public class DebugCommand extends SingleMainCommand {
    public DebugCommand() {
        super("Debug", "/%s debug", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Message.DEBUG.send(sender, plugin.getPlayerCount(), plugin.getUserManager().getUsers().size(),
                plugin.getGroupManager().getGroups().size(), plugin.getTrackManager().getTracks().size()
        );
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return Permission.DEBUG.isAuthorized(sender);
    }
}
