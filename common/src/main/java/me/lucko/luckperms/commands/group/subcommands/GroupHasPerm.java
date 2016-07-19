package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupHasPerm extends GroupSubCommand {
    public GroupHasPerm() {
        super("haspermission", "Checks to see if a group has a certain permission node",
                "/%s group <group> haspermission <node> [server]", Permission.GROUP_HASPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        if (args.size() == 2) {
            Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, args.get(1).toLowerCase()));
        } else {
            Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, "global"));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
