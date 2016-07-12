package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
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
                Message.GROUP_DOES_NOT_EXIST.send(sender);
            } else {
                Group group = plugin.getGroupManager().getGroup(groupName);
                if (group == null) {
                    Message.GROUP_DOES_NOT_EXIST.send(sender);
                    return;
                }

                try {
                    if (args.size() == 2) {
                        final String server = args.get(1).toLowerCase();
                        user.addGroup(group, server);
                        Message.USER_ADDGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
                    } else {
                        user.addGroup(group);
                        Message.USER_ADDGROUP_SUCCESS.send(sender, user.getName(), groupName);
                    }

                    saveUser(user, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.USER_ALREADY_MEMBER_OF.send(sender, user.getName(), group.getName());
                }
            }
        });
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2);
    }
}
