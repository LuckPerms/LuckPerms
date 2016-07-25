package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserAddGroup extends UserSubCommand {
    public UserAddGroup() {
        super("addgroup", "Adds the user to a group", "/%s user <user> addgroup <group> [server] [world]", Permission.USER_ADDGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

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
                    if (args.size() >= 2) {
                        final String server = args.get(1).toLowerCase();
                        if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                            Message.SERVER_INVALID_ENTRY.send(sender);
                            return;
                        }

                        if (args.size() == 2) {
                            user.addGroup(group, server);
                            Message.USER_ADDGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server);
                        } else {
                            final String world = args.get(2).toLowerCase();
                            user.addGroup(group, server, world);
                            Message.USER_ADDGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server, world);
                        }

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
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getGroupTabComplete(args, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return (argLength != 1 && argLength != 2 && argLength != 3);
    }
}
