package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserUnSetPermissionCommand extends UserSubCommand {
    public UserUnSetPermissionCommand() {
        super("unset", "Unsets a permission for a user",
                "/perms user <user> unset <node> [server]", Permission.USER_UNSETPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String node = args.get(0);

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (node.matches("group\\..*")) {
            Util.sendPluginMessage(sender, "Use the removegroup command instead of specifying the node.");
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                user.unsetPermission(node, server);
                Util.sendPluginMessage(sender, "&aUnset &b" + node + "&a for &b" + user.getName() + "&a on server &b" +
                        server + "&a.");
            } else {
                user.unsetPermission(node);
                Util.sendPluginMessage(sender, "&aUnset &b" + node + "&a for &b" + user.getName() + "&a.");
            }

            saveUser(user, sender, plugin);
        } catch (ObjectLacksPermissionException e) {
            Util.sendPluginMessage(sender, "That user does not have this permission set.");
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
