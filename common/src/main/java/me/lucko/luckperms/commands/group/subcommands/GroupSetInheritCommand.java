package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupSetInheritCommand extends GroupSubCommand {
    public GroupSetInheritCommand() {
        super("setinherit", "Sets another group for this group to inherit permissions from",
                "/perms group <group> setinherit <group> [server]", Permission.GROUP_SETINHERIT);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Message.GROUP_LOAD_ERROR.send(sender);
            } else {
                try {
                    if (args.size() == 2) {
                        final String server = args.get(1).toLowerCase();
                        group.setPermission("group." + groupName, true, server);
                        Message.GROUP_SETINHERIT_SERVER_SUCCESS.send(sender, group.getName(), groupName, server);
                    } else {
                        group.setPermission("group." + groupName, true);
                        Message.GROUP_SETINHERIT_SUCCESS.send(sender, group.getName(), groupName);
                    }

                    saveGroup(group, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.GROUP_ALREADY_INHERITS.send(sender, group.getName(), groupName);
                }
            }
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
