package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Messages;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupUnSetPermissionCommand extends GroupSubCommand {
    public GroupUnSetPermissionCommand() {
        super("unset", "Unsets a permission for a group",
                "/perms group <group> unset <node> [server]", Permission.GROUP_UNSETPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String node = args.get(0);

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (node.matches("group\\..*")) {
            Messages.GROUP_USE_UNINHERIT.send(sender);
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                group.unsetPermission(node, server);
                Messages.UNSETPERMISSION_SERVER_SUCCESS.send(sender, node, group.getName(), server);
            } else {
                group.unsetPermission(node);
                Messages.UNSETPERMISSION_SUCCESS.send(sender, node, group.getName());
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksPermissionException e) {
            Messages.DOES_NOT_HAVEPERMISSION.send(sender, group.getName());
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
