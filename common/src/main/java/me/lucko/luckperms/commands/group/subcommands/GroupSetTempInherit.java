package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.DateUtil;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupSetTempInherit extends GroupSubCommand {
    public GroupSetTempInherit() {
        super("settempinherit", "Sets another group for this group to inherit permissions from temporarily",
                "/%s group <group> settempinherit <group> <duration> [server] [world]", Permission.GROUP_SET_TEMP_INHERIT);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
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
                Message.GROUP_LOAD_ERROR.send(sender);
            } else {
                try {
                    if (args.size() >= 3) {
                        final String server = args.get(2).toLowerCase();
                        if (Patterns.NON_ALPHA_NUMERIC.matcher(server).find()) {
                            Message.SERVER_INVALID_ENTRY.send(sender);
                            return;
                        }

                        if (args.size() == 3) {
                            group.setPermission("group." + groupName, true, server, duration);
                            Message.GROUP_SET_TEMP_INHERIT_SERVER_SUCCESS.send(sender, group.getName(), groupName, server,
                                    DateUtil.formatDateDiff(duration));
                        } else {
                            final String world = args.get(3).toLowerCase();
                            group.setPermission("group." + groupName, true, server, world, duration);
                            Message.GROUP_SET_TEMP_INHERIT_SERVER_WORLD_SUCCESS.send(sender, group.getName(), groupName, server,
                                    world, DateUtil.formatDateDiff(duration));
                        }

                    } else {
                        group.setPermission("group." + groupName, true, duration);
                        Message.GROUP_SET_TEMP_INHERIT_SUCCESS.send(sender, group.getName(), groupName, DateUtil.formatDateDiff(duration));
                    }

                    saveGroup(group, sender, plugin);
                } catch (ObjectAlreadyHasException e) {
                    Message.GROUP_ALREADY_TEMP_INHERITS.send(sender, group.getName(), groupName);
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
        return argLength != 2 && argLength != 3 && argLength != 4;
    }
}
