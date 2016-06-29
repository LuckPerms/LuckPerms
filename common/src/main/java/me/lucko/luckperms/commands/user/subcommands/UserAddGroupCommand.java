package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Permission;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserAddGroupCommand extends UserSubCommand {
    public UserAddGroupCommand() {
        super("addgroup", "Adds the user to a group", "/perms user <user> addgroup <group> [server]", Permission.USER_ADDGROUP);
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

                try {
                    if (args.size() == 2) {
                        final String server = args.get(1).toLowerCase();
                        user.addGroup(group, server);
                        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a successfully added to group &b" +
                                groupName + "&a on the server &b" + server + "&a.");
                    } else {
                        user.addGroup(group);
                        Util.sendPluginMessage(sender, "&b" + user.getName() + "&a successfully added to group &b" +
                                groupName + "&a.");
                    }

                    saveUser(user, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Util.sendPluginMessage(sender, "The user is already a member of that group.");
                }
            }
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
