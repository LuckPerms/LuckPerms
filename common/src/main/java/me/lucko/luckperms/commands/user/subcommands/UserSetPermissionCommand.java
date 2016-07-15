package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserSetPermissionCommand extends UserSubCommand {
    public UserSetPermissionCommand() {
        super("set", "Sets a permission for a user",
                "/perms user <user> set <node> <true|false> [server]", Permission.USER_SETPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (node.contains("/")) {
            sendUsage(sender);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.USER_USE_ADDGROUP.send(sender);
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
                user.setPermission(node, b, server);
                Message.SETPERMISSION_SERVER_SUCCESS.send(sender, node, bool, user.getName(), server);
            } else {
                user.setPermission(node, b);
                Message.SETPERMISSION_SUCCESS.send(sender, node, bool, user.getName());
            }

            saveUser(user, sender, plugin);
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HASPERMISSION.send(sender, user.getName());
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 2 && argLength != 3;
    }
}
