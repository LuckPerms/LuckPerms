package me.lucko.luckperms.commands.group;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SingleMainCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;

import java.util.ArrayList;
import java.util.List;

public class ListGroups extends SingleMainCommand {
    public ListGroups() {
        super("ListGroups", "/%s listgroups", 0);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        plugin.getDatastore().loadAllGroups(success -> {
            if (!success) {
                Message.GROUPS_LOAD_ERROR.send(sender);
            } else {
                Message.GROUPS_LIST.send(sender, Util.listToCommaSep(new ArrayList<>(plugin.getGroupManager().getGroups().keySet())));
            }
        });
    }

    @Override
    protected boolean isAuthorized(Sender sender) {
        return Permission.LIST_GROUPS.isAuthorized(sender);
    }
}
