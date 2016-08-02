package me.lucko.luckperms.commands.user.subcommands;

import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.commands.Predicate;
import me.lucko.luckperms.commands.Sender;
import me.lucko.luckperms.commands.SubCommand;
import me.lucko.luckperms.constants.Message;
import me.lucko.luckperms.constants.Permission;
import me.lucko.luckperms.users.User;

import java.util.List;

public class UserClear extends SubCommand<User> {
    public UserClear() {
        super("clear", "Clears a users permissions and groups", "/%s user <user> clear", Permission.USER_CLEAR,
                Predicate.alwaysFalse());
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, User user, List<String> args, String label) {
        user.clearNodes();
        plugin.getUserManager().giveDefaults(user);
        Message.CLEAR_SUCCESS.send(sender, user.getName());

        saveUser(user, sender, plugin);
    }
}
