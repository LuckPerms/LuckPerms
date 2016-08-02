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

public class UserRemoveGroup extends SubCommand<User> {
    public UserRemoveGroup() {
        super("removegroup", "Removes a user from a group", "/%s user <user> removegroup <group> [server] [world]",
                Permission.USER_REMOVEGROUP, Predicate.notinRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if ((args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("global")))
                && user.getPrimaryGroup().equalsIgnoreCase(groupName)) {
            Message.USER_REMOVEGROUP_ERROR_PRIMARY.send(sender);
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
                    user.unsetPermission("group." + groupName, server);
                    Message.USER_REMOVEGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
                } else {
                    final String world = args.get(2).toLowerCase();
                    user.unsetPermission("group." + groupName, server, world);
                    Message.USER_REMOVEGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server, world);
                }

            } else {
                user.unsetPermission("group." + groupName);
                Message.USER_REMOVEGROUP_SUCCESS.send(sender, user.getName(), groupName);
            }

            saveUser(user, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.USER_NOT_MEMBER_OF.send(sender, user.getName(), groupName);
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }
}
