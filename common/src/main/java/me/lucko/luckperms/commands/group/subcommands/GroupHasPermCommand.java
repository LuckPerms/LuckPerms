package me.lucko.luckperms.commands.group.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.group.GroupSubCommand;
import me.lucko.luckperms.groups.Group;

import java.util.List;

public class GroupHasPermCommand extends GroupSubCommand {
    public GroupHasPermCommand() {
        super("haspermission", "Checks to see if a group has a certain permission node",
                "/perms group <group> haspermission <node> [server]", "luckperms.group.haspermission");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, Group group, List<String> args) {
        if (args.size() >= 2) {
            Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, args.get(1)));
        } else {
            Util.sendBoolean(sender, args.get(0), group.hasPermission(args.get(0), true, "global"));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength == 0;
    }
}
