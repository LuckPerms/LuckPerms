package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.*;

import java.util.ArrayList;
import java.util.List;

public class ListGroupsCommand extends MainCommand {
    public ListGroupsCommand() {
        super("ListGroups", "/perms listgroups", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args) {
        plugin.getDatastore().loadAllGroups(success -> {
            if (!success) {
                Util.sendPluginMessage(sender, "Unable to load all groups.");
            } else {
                Util.sendPluginMessage(sender, "&aGroups: " +
                        Util.listToCommaSep(new ArrayList<>(plugin.getGroupManager().getGroups().keySet())));
            }
        });
    }

    @Override
    public List<SubCommand> getSubCommands() {
        return new ArrayList<>();
    }

    @Override
    protected boolean canUse(Sender sender) {
        return Permission.LIST_GROUPS.isAuthorized(sender);
    }
}
