package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupInheritsPermCommand extends GroupSubCommand {
    public GroupInheritsPermCommand() {
        super("inheritspermission", "Checks to see if a group inherits a certain permission node",
                "/%s group <group> inheritspermission <node> [server]", Permission.GROUP_INHERITSPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args, String label) {
        if (args.size() == 2) {
            Util.sendBoolean(sender, args.get(0), group.inheritsPermission(args.get(0), true, args.get(1).toLowerCase()));
        } else {
            Util.sendBoolean(sender, args.get(0), group.inheritsPermission(args.get(0), true));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
