package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupClear extends SubCommand<Group> {
    public GroupClear() {
        super("clear", "Clears a groups permissions", "/%s group <group> clear", Permission.GROUP_CLEAR,
                Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        group.clearNodes();
        Message.CLEAR_SUCCESS.send(sender, group.getName());

        saveGroup(group, sender, plugin);
    }
}
