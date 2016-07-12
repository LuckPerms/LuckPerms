package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.user.UserSubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserClearCommand extends UserSubCommand {
    public UserClearCommand() {
        super("clear", "Clears a users permissions and groups", "/perms user <user> clear", Permission.USER_CLEAR);
    }

    @Override
    protected void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args) {
        user.clearNodes();
        plugin.getUserManager().giveDefaults(user);
        Message.CLEAR_SUCCESS.send(sender, user.getName());

        saveUser(user, sender, plugin);
    }

    @Override
    public boolean isArgLengthInvalid(int argLength) {
        return false;
    }
}
