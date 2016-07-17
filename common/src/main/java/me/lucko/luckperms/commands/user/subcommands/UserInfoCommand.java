package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.Util;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserInfoCommand extends UserSubCommand {
    public UserInfoCommand() {
        super("info", "Gives info about the user", "/%s user <user> info", Permission.USER_INFO);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        Message.USER_INFO.send(sender, user.getName(), user.getUuid(), plugin.getPlayerStatus(user.getUuid()),
                Util.listToCommaSep(user.getGroupNames()), user.getPrimaryGroup(),
                (user.getNodes().keySet().size() - user.getGroupNames().size()), label, user.getName()
        );
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
