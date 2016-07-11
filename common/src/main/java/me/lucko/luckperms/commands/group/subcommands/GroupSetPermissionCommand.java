package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupSetPermissionCommand extends GroupSubCommand {
    public GroupSetPermissionCommand() {
        super("set", "Sets a permission for a group", "/perms group <group> set <node> <true|false> [server]",
                Permission.GROUP_SETPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String node = args.get(0);
        String bool = args.get(1);

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (node.matches("group\\..*")) {
            Messages.GROUP_USE_INHERIT.send(sender);
            return;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender);
            return;
        }

        boolean b = Boolean.parseBoolean(bool);

        try {
            if (args.size() == 3) {
                final String server = args.get(2).toLowerCase();
                group.setPermission(node, b, server);
                Messages.SETPERMISSION_SERVER_SUCCESS.send(sender, node, bool, group.getName(), server);
            } else {
                group.setPermission(node, b);
                Messages.SETPERMISSION_SUCCESS.send(sender, node, bool, group.getName());
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectAlreadyHasException e) {
            Messages.ALREADY_HASPERMISSION.send(sender, group.getName());
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 2 && argLength != 3;
    }
}
