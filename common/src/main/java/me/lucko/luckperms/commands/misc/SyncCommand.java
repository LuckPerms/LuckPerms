package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;

import java.util.ArrayList;
import java.util.List;

public class SyncCommand extends MainCommand {
    public SyncCommand() {
        super("Sync", "/perms sync", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        Util.sendPluginMessage(sender, "&bRunning update task for all online users.");
        plugin.runUpdateTask();
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.SYNC.isAuthorized(sender);
    }
}
