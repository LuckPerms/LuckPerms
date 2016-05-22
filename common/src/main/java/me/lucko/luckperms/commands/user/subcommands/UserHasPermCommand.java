package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserHasPermCommand extends UserSubCommand {
    public UserHasPermCommand() {
        super("haspermission", "Checks to see if a user has a certain permission node",
                "/perms user <user> haspermission <node> [server]", "luckperms.user.haspermission");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        if (args.size() >= 2) {
            Util.sendBoolean(sender, args.get(0), user.hasPermission(args.get(0), true, args.get(1)));
        } else {
            Util.sendBoolean(sender, args.get(0), user.hasPermission(args.get(0), true, "global"));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength == 0;
    }
}
