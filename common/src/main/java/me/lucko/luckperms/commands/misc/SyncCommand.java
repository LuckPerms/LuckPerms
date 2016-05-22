package me.lucko.luckperms.commands.misc;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;

import java.util.ArrayList;
import java.util.List;

public class SyncCommand extends MainCommand {
    public SyncCommand() {
        super("Sync", "/perms sync", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (!sender.hasPermission("luckperms.sync")) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        Util.sendPluginMessage(sender, "&bRunning update task for all online users.");
        plugin.runUpdateTask();
    }

    @Override
    public List<? extends SubCommand> getSubCommands() {
        return new ArrayList<>();
    }
}
