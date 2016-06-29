package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.exceptions.ObjectLacksPermissionException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserRemoveGroupCommand extends UserSubCommand {
    public UserRemoveGroupCommand() {
        super("removegroup", "Removes a user from a group", "/perms user <user> removegroup <group> [server]", Permission.USER_REMOVEGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        String groupName = args.get(0).toLowerCase();

        plugin.getDatastore().loadGroup(groupName, success -> {
            if (!success) {
                Util.sendPluginMessage(sender, groupName + " does not exist!");
            } else {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Util.sendPluginMessage(sender, "That group does not exist!");
                    return;
                }

                if ((args.size() == 1 || (args.size() == 2 && args.get(1).equalsIgnoreCase("global")))
                        && user.getPrimaryGroup().equalsIgnoreCase(group.getName())) {
                    Util.sendPluginMessage(sender, "You cannot remove a user from their primary group.");
                    return;
                }

                try {
                    if (args.size() == 2) {
                        final String server = args.get(1).toLowerCase();
                        user.removeGroup(group, server);
                        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a was removed from group &b" +
                                groupName + "&a on server &b" + server + "&a.");
                    } else {
                        user.removeGroup(group);
                        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a was removed from group &b" +
                                groupName + "&a.");
                    }

                    saveUser(user, sender, plugin);
                } catch (ObjectLacksPermissionException e) {
                    Util.sendPluginMessage(sender, "The user is not a member of that group.");
                }
            }
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
