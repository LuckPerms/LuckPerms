package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupInfo extends SubCommand<Group> {
    public GroupInfo() {
        super("info", "Gives info about the group", "/%s group <group> info", Permission.GROUP_INFO,
                Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        Message.GROUP_INFO.send(sender, group.getName(), group.getPermanentNodes().keySet().size(),
                group.getTemporaryNodes().keySet().size(), label, group.getName());
    }
}
