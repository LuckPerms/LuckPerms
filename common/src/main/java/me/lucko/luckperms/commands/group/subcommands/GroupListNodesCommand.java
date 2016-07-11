package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupListNodesCommand extends GroupSubCommand {
    public GroupListNodesCommand() {
        super("listnodes", "Lists the permission nodes the group has", "/perms group <group> listnodes",
                Permission.GROUP_LISTNODES);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        Messages.LISTNODES.send(sender, group.getName(), Util.nodesToString(group.getNodes()));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
