package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;

import java.util.ArrayList;
import java.util.List;

public class ListGroupsCommand extends MainCommand {
    public ListGroupsCommand() {
        super("ListGroups", "/perms listgroups", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        if (!sender.hasPermission("luckperms.listgroups")) {
            Util.sendPluginMessage(sender, "You do not have permission to use this command!");
            return;
        }

        plugin.getDatastore().loadAllGroups(success -> {
            if (!success) {
                Util.sendPluginMessage(sender, "Unable to load all groups.");
            } else {
                Util.sendPluginMessage(sender, "&aGroups: " + Util.listToCommaSep(new ArrayList<>(plugin.getGroupManager().getGroups().keySet())));
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return null;
    }
}
