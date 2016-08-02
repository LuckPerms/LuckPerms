package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class GroupHasPerm extends SubCommand<Group> {
    public GroupHasPerm() {
        super("haspermission", "Checks to see if a group has a certain permission node",
                "/%s group <group> haspermission <node> [server] [world]", Permission.GROUP_HASPERMISSION,
                Predicate.notinRange(1, 3));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        if (args.size() >= 2) {
            if (Patterns.NON_ALPHA_NUMERIC.matcher(args.get(1)).find()) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return;
            }

            if (args.size() == 2) {
                Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, args.get(1)));
            } else {
                Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, args.get(1), args.get(2)));
            }

        } else {
            Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, "global"));
        }
    }
}
