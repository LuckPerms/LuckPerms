package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.List;

public class SyncCommand extends SingleMainCommand {
    public SyncCommand() {
        super("Sync", "/%s sync", 0, Permission.SYNC);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Message.UPDATE_TASK_RUN.send(sender);
        plugin.runUpdateTask();
    }
}
