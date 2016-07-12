package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.MainCommand;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

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
                Message.GROUPS_LOAD_ERROR.send(sender);
            } else {
                Message.GROUPS_LIST.send(sender, Util.listToCommaSep(new ArrayList<>(plugin.getGroupManager().getGroups().keySet())));
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
