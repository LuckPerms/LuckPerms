package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectLacksException;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserRemoveTempGroup extends UserSubCommand {
    public UserRemoveTempGroup() {
        super("removetempgroup", "Removes a user from a temporary group", "/%s user <user> removetempgroup <group> [server]", Permission.USER_REMOVETEMPGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        try {
            if (args.size() == 2) {
                final String server = args.get(1).toLowerCase();
                if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                    Message.SERVER_INVALID_ENTRY.send(sender);
                    return;
                }

                user.unsetPermission("group." + groupName, server, true);
                Message.USER_REMOVETEMPGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
            } else {
                user.unsetPermission("group." + groupName, true);
                Message.USER_REMOVETEMPGROUP_SUCCESS.send(sender, user.getName(), groupName);
            }

            saveUser(user, sender, plugin);
        } catch (ObjectLacksException e) {
            Message.USER_NOT_TEMP_MEMBER_OF.send(sender, user.getName(), groupName);
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
