package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupListNodes extends GroupSubCommand {
    public GroupListNodes() {
        super("listnodes", "Lists the permission nodes the group has", "/%s group <group> listnodes",
                Permission.GROUP_LISTNODES);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        Message.LISTNODES.send(sender, group.getName(), Util.permNodesToString(group.getPermanentNodes()));
        Message.LISTNODES_TEMP.send(sender, group.getName(), Util.tempNodesToString(group.getTemporaryNodes()));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
