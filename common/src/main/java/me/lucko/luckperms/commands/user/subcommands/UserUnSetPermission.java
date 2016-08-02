package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserUnSetPermission extends SubCommand<User> {
    public UserUnSetPermission() {
        super("unset", "Unsets a permission for a user",
                "/%s user <user> unset <node> [server] [world]", Permission.USER_UNSETPERMISSION, Predicate.notinRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String node = args.get(0);

        if (node.contains("/") || node.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.USER_USE_REMOVEGROUP.send(sender);
            return;
        }

        try {
            if (args.size() >= 2) {
                final String server = args.get(1).toLowerCase();
                if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                if (args.size() == 2) {
                    user.unsetPermission(node, server);
                    Message.UNSETPERMISSION_SERVER_SUCCESS.send(sender, node, user.getName(), server);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission(node, server, world);
                    Message.UNSETPERMISSION_SERVER_WORLD_SUCCESS.send(sender, node, user.getName(), server, world);
                }

            } else {
                user.unsetPermission(node);
                Message.UNSETPERMISSION_SUCCESS.send(sender, node, user.getName());
            }

            saveUser(user, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.DOES_NOT_HAVEPERMISSION.send(sender, user.getName());
        }
    }
}
