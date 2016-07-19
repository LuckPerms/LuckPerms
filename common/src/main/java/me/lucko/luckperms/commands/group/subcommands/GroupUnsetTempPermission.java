package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupUnsetTempPermission extends GroupSubCommand {
    public GroupUnsetTempPermission() {
        super("unsettemp", "Unsets a temporary permission for a group",
                "/%s group <group> unsettemp <node> [server]", Permission.GROUP_UNSET_TEMP_PERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0);

        if (node.contains("/") || node.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.GROUP_USE_UNINHERIT.send(sender);
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                group.unsetPermission(node, server);
                Message.UNSET_TEMP_PERMISSION_SERVER_SUCCESS.send(sender, node, group.getName(), server, true);
            } else {
                group.unsetPermission(node, true);
                Message.UNSET_TEMP_PERMISSION_SUCCESS.send(sender, node, group.getName());
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVE_TEMP_PERMISSION.send(sender, group.getName());
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
