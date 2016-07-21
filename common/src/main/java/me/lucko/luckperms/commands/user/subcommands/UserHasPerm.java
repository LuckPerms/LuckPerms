package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;
import me.lucko.luckperms.utils.Patterns;

import java.util.List;

public class UserHasPerm extends UserSubCommand {
    public UserHasPerm() {
        super("haspermission", "Checks to see if a user has a certain permission node",
                "/%s user <user> haspermission <node> [server]", Permission.USER_HASPERMISSION);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        if (args.size() >= 2) {
            if (Patterns.NON_ALPHA_NUMERIC.matcher(args.get(1)).find()) {
                Message.SERVER_INVALID_ENTRY.send(sender);
                return;
            }

            Util.sendBoolean(sender, args.get(0), user.hasPermission(args.get(0), true, args.get(1)));
        } else {
            Util.sendBoolean(sender, args.get(0), user.hasPermission(args.get(0), true, "global"));
        }
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return argLength != 1 && argLength != 2;
    }
}
