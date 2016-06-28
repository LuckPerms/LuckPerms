package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupInfoCommand extends GroupSubCommand {
    public GroupInfoCommand() {
        super("info", "Gives info about the group",
                "/perms group <group> info", "luckperms.group.info");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        final String prefix = Util.PREFIX;
        String sb = prefix + "&d-> &eGroup: &6" + group.getName() + "\n" +
                prefix + "&d-> &ePermissions: &6" + group.getNodes().keySet().size() + "\n" +
                prefix + "&d-> &bUse &a/perms group " + group.getName() + " listnodes &bto see all permissions.";

        sender.sendMessage(Util.color(sb));
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
