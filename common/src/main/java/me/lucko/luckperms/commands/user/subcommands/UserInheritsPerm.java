package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserInheritsPerm extends UserSubCommand {
    public UserInheritsPerm() {
        super("inheritspermission", "Checks to see if a user inherits a certain permission node",
                "/%s user <user> inheritspermission <node> [server]", Permission.USER_INHERITSPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        if (args.size() >= 2) {
            Util.sendBoolean(sender, args.get(0), user.inheritsPermission(args.get(0), true, args.get(1)));
        } else {
            Util.sendBoolean(sender, args.get(0), user.inheritsPermission(args.get(0), true));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
