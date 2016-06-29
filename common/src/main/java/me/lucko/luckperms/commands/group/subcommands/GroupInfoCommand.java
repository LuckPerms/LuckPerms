package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupInfoCommand extends GroupSubCommand {
    public GroupInfoCommand() {
        super("info", "Gives info about the group", "/perms group <group> info", Permission.GROUP_INFO);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        sender.sendMessage(Util.color(
                Util.PREFIX + "&d-> &eGroup: &6" + group.getName() + "\n" +
                Util.PREFIX + "&d-> &ePermissions: &6" + group.getNodes().keySet().size() + "\n" +
                Util.PREFIX + "&d-> &bUse &a/perms group " + group.getName() + " listnodes &bto see all permissions."
        ));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
