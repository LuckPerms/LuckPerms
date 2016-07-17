package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.Collections;
import java.util.List;

public class SyncCommand extends MainCommand {
    public SyncCommand() {
        super("Sync", "/%s sync", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Message.UPDATE_TASK_RUN.send(sender);
        plugin.runUpdateTask();
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
        return Permission.SYNC.isAuthorized(sender);
    }
}
