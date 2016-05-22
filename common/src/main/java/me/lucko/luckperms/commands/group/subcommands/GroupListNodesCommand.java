package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupListNodesCommand extends GroupSubCommand {
    public GroupListNodesCommand() {
        super("listnodes", "Lists the permission nodes the group has",
                "/perms group <group> listnodes", "luckperms.group.listnodes");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        Util.sendPluginMessage(sender, "&e" + group.getName() + "'s Nodes:");
        sender.sendMessage(Util.color(Util.nodesToString(group.getNodes())));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
