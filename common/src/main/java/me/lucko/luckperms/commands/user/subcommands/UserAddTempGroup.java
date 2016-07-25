package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserAddTempGroup extends UserSubCommand {
    public UserAddTempGroup() {
        super("addtempgroup", "Adds the user to a group temporarily",
                "/%s user <user> addtempgroup <group> <duration> [server] [world]", Permission.USER_ADDTEMPGROUP);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        String groupName = args.get(0).toLowerCase();

        if (groupName.contains("/") || groupName.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(1), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(1));
            return;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
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
                    if (args.size() >= 3) {
                        final String server = args.get(2).toLowerCase();
                        if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                            Message.SERVER_INVALID_ENTRY.send(sender);
                            return;
                        }

                        if (args.size() == 3) {
                            user.addGroup(group, server, duration);
                            Message.USER_ADDTEMPGROUP_SERVER_SUCCESS.send(sender, user.getName(), groupName, server,
                                    DateUtil.formatDateDiff(duration));
                        } else {
                            final String world = args.get(3).toLowerCase();
                            user.addGroup(group, server, world, duration);
                            Message.USER_ADDTEMPGROUP_SERVER_WORLD_SUCCESS.send(sender, user.getName(), groupName, server,
                                    world, DateUtil.formatDateDiff(duration));
                        }

                    } else {
                        user.addGroup(group, duration);
                        Message.USER_ADDTEMPGROUP_SUCCESS.send(sender, user.getName(), groupName, DateUtil.formatDateDiff(duration));
                    }

                    saveUser(user, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.USER_ALREADY_TEMP_MEMBER_OF.send(sender, user.getName(), group.getName());
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
        return (argLength != 2 && argLength != 3 && argLength != 4);
    }
}
