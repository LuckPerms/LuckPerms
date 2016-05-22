package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserGetUUIDCommand extends UserSubCommand {
    public UserGetUUIDCommand() {
        super("getuuid", "Get the UUID of a user", "/perms user <user> getuuid", "luckperms.user.getuuid");
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        Util.sendPluginMessage(sender, "&bThe UUID of &e" + user.getName() + "&b is &e" + user.getUuid().toString() + "&b.");
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
