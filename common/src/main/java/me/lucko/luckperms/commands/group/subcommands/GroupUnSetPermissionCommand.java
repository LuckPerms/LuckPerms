package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupUnSetPermissionCommand extends GroupSubCommand {
    public GroupUnSetPermissionCommand() {
        super("unset", "Unsets a permission for a group",
                "/perms group <group> unset <node> [server]", "luckperms.group.unsetpermission");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        String node = args.get(0);

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (node.matches("group\\..*")) {
            Util.sendPluginMessage(sender, "Use the unsetinherit command instead of specifying the node.");
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                group.unsetPermission(node, server);
                Util.sendPluginMessage(sender, "&aUnset &b" + node + "&a for &b" + group.getName() + "&a on server &b" + server + "&a.");
            } else {
                group.unsetPermission(node);
                Util.sendPluginMessage(sender, "&aUnset &b" + node + "&a for &b" + group.getName() + "&a.");
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksPermissionException e) {
            Util.sendPluginMessage(sender, "That group does not have this permission set.");
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength == 0;
    }
}
