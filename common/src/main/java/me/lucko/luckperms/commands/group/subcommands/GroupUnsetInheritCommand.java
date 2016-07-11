package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupUnsetInheritCommand extends GroupSubCommand {
    public GroupUnsetInheritCommand() {
        super("unsetinherit", "Unsets another group for this group to inherit permissions from",
                "/perms group <group> unsetinherit <group> [server]", Permission.GROUP_UNSETINHERIT);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                group.unsetPermission("group." + groupName, server);
                Messages.GROUP_UNSETINHERIT_SERVER_SUCCESS.send(sender, group.getName(), groupName, server);
            } else {
                group.unsetPermission("group." + groupName);
                Messages.GROUP_UNSETINHERIT_SUCCESS.send(sender, group.getName(), groupName);
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksPermissionException e) {
            Messages.GROUP_DOES_NOT_INHERIT.send(sender, group.getName(), groupName);
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
