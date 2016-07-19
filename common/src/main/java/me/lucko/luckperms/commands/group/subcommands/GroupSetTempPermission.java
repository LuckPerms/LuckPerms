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

public class GroupSetTempPermission extends GroupSubCommand {
    public GroupSetTempPermission() {
        super("settemp", "Sets a temporary permission for a group", "/%s group <group> settemp <node> <true|false> <duration> [server]",
                Permission.GROUP_SET_TEMP_PERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        String node = args.get(0);
        String bool = args.get(1).toLowerCase();

        if (node.contains("/") || node.contains("$")) {
            sendUsage(sender, label);
            return;
        }

        if (Patterns.GROUP_MATCH.matcher(node).matches()) {
            Message.GROUP_USE_INHERIT.send(sender);
            return;
        }

        if (!bool.equalsIgnoreCase("true") && !bool.equalsIgnoreCase("false")) {
            sendUsage(sender, label);
            return;
        }

        boolean b = Boolean.parseBoolean(bool);

        long duration;
        try {
            duration = DateUtil.parseDateDiff(args.get(2), true);
        } catch (DateUtil.IllegalDateException e) {
            Message.ILLEGAL_DATE_ERROR.send(sender, args.get(2));
            return;
        }

        if (DateUtil.shouldExpire(duration)) {
            Message.PAST_DATE_ERROR.send(sender);
            return;
        }

        try {
            if (args.size() == 4) {
                final String server = args.get(3).toLowerCase();
                group.setPermission(node, b, server, duration);
                Message.SETPERMISSION_TEMP_SERVER_SUCCESS.send(sender, node, bool, group.getName(), server, DateUtil.formatDateDiff(duration));
            } else {
                group.setPermission(node, b, duration);
                Message.SETPERMISSION_TEMP_SUCCESS.send(sender, node, bool, group.getName(), DateUtil.formatDateDiff(duration));
            }

            saveGroup(group, sender, plugin);
        } catch (ObjectAlreadyHasException e) {
            Message.ALREADY_HAS_TEMP_PERMISSION.send(sender, group.getName());
        }
    }

    @Override
    public List<String> onTabComplete(Sender sender, List<String> args, LuckPermsPlugin plugin) {
        return getBoolTabComplete(args);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 3 && argLength != 4;
    }
}
